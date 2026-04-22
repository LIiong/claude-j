package com.claudej.application.order.service;

import com.claudej.application.order.assembler.OrderAssembler;
import com.claudej.application.order.command.CreateOrderCommand;
import com.claudej.application.order.command.CreateOrderFromCartCommand;
import com.claudej.application.order.dto.OrderDTO;
import com.claudej.domain.cart.model.aggregate.Cart;
import com.claudej.domain.cart.model.entity.CartItem;
import com.claudej.domain.cart.repository.CartRepository;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.coupon.model.aggregate.Coupon;
import com.claudej.domain.coupon.model.valobj.CouponId;
import com.claudej.domain.coupon.repository.CouponRepository;
import com.claudej.domain.order.model.aggregate.Order;
import com.claudej.domain.order.model.entity.OrderItem;
import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.order.model.valobj.Money;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.domain.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单应用服务
 */
@Service
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CouponRepository couponRepository;
    private final OrderAssembler orderAssembler;

    public OrderApplicationService(OrderRepository orderRepository, CartRepository cartRepository,
                                   CouponRepository couponRepository, OrderAssembler orderAssembler) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.couponRepository = couponRepository;
        this.orderAssembler = orderAssembler;
    }

    /**
     * 创建订单
     */
    @Transactional
    public OrderDTO createOrder(CreateOrderCommand command) {
        if (command == null || command.getCustomerId() == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "客户ID不能为空");
        }
        if (command.getItems() == null || command.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单项不能为空");
        }

        CustomerId customerId = new CustomerId(command.getCustomerId());
        Order order = Order.create(customerId);

        // 添加订单项
        for (CreateOrderCommand.OrderItemCommand itemCmd : command.getItems()) {
            OrderItem item = OrderItem.create(
                    itemCmd.getProductId(),
                    itemCmd.getProductName(),
                    itemCmd.getQuantity(),
                    new Money(itemCmd.getUnitPrice(), "CNY")
            );
            order.addItem(item);
        }

        // 处理优惠券
        if (command.getCouponId() != null && !command.getCouponId().trim().isEmpty()) {
            applyCouponToOrder(order, command.getCouponId(), command.getCustomerId());
        }

        order = orderRepository.save(order);
        return orderAssembler.toDTO(order);
    }

    /**
     * 根据订单号查询订单
     */
    public OrderDTO getOrderById(String orderId) {
        Order order = orderRepository.findByOrderId(new OrderId(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return orderAssembler.toDTO(order);
    }

    /**
     * 根据客户ID查询订单列表
     */
    public List<OrderDTO> getOrdersByCustomerId(String customerId) {
        List<Order> orders = orderRepository.findByCustomerId(new CustomerId(customerId));
        return orderAssembler.toDTOList(orders);
    }

    /**
     * 支付订单
     */
    @Transactional
    public OrderDTO payOrder(String orderId) {
        Order order = orderRepository.findByOrderId(new OrderId(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 如果有优惠券，核销优惠券
        if (order.hasCoupon()) {
            Coupon coupon = couponRepository.findByCouponId(order.getCouponId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
            coupon.use(order.getOrderIdValue(), LocalDateTime.now());
            couponRepository.save(coupon);
        }

        order.pay();
        order = orderRepository.save(order);
        return orderAssembler.toDTO(order);
    }

    /**
     * 取消订单
     */
    @Transactional
    public OrderDTO cancelOrder(String orderId) {
        Order order = orderRepository.findByOrderId(new OrderId(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 如果订单已支付且有优惠券，回滚优惠券
        if (order.isPaid() && order.hasCoupon()) {
            Coupon coupon = couponRepository.findByCouponId(order.getCouponId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
            coupon.unuse();
            couponRepository.save(coupon);
        }

        // 如果订单未支付但有优惠券，移除优惠券关联
        if (order.hasCoupon()) {
            order.removeCoupon();
        }

        order.cancel();
        order = orderRepository.save(order);
        return orderAssembler.toDTO(order);
    }

    /**
     * 从购物车创建订单
     */
    @Transactional
    public OrderDTO createOrderFromCart(CreateOrderFromCartCommand command) {
        if (command == null || command.getCustomerId() == null || command.getCustomerId().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND, "客户ID不能为空");
        }

        // 1. 查询购物车
        Cart cart = cartRepository.findByUserId(command.getCustomerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_NOT_FOUND));

        // 2. 验证购物车非空
        if (cart.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }

        // 3. 创建订单
        CustomerId customerId = new CustomerId(command.getCustomerId());
        Order order = Order.create(customerId);

        // 4. 将购物车项转换为订单项
        List<CartItem> cartItems = cart.getItems();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = OrderItem.create(
                    cartItem.getProductId(),
                    cartItem.getProductName(),
                    cartItem.getQuantity().getValue(),
                    new Money(cartItem.getUnitPrice().getAmount(), cartItem.getUnitPrice().getCurrency())
            );
            order.addItem(orderItem);
        }

        // 5. 处理优惠券
        if (command.getCouponId() != null && !command.getCouponId().trim().isEmpty()) {
            applyCouponToOrder(order, command.getCouponId(), command.getCustomerId());
        }

        // 6. 保存订单
        order = orderRepository.save(order);

        // 7. 清空购物车并保存
        cart.clear();
        cartRepository.save(cart);

        // 8. 返回订单DTO
        return orderAssembler.toDTO(order);
    }

    /**
     * 应用优惠券到订单
     */
    private void applyCouponToOrder(Order order, String couponIdStr, String customerId) {
        CouponId couponId = new CouponId(couponIdStr);
        Coupon coupon = couponRepository.findByCouponId(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        // 验证优惠券归属
        if (!coupon.getUserId().equals(customerId)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_BELONG_TO_USER);
        }

        // 检查有效期（懒过期策略）
        LocalDateTime now = LocalDateTime.now();
        coupon.checkAndExpire(now);
        if (coupon.getStatus() != com.claudej.domain.coupon.model.valobj.CouponStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        // 验证最低消费
        BigDecimal totalAmount = order.getTotalAmount().getAmount();
        if (totalAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BusinessException(ErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET);
        }

        // 计算折扣金额
        BigDecimal discountAmount = coupon.calculateDiscount(totalAmount);
        order.applyCoupon(couponId, new Money(discountAmount, order.getTotalAmount().getCurrency()));
    }

    /**
     * 发货
     */
    @Transactional
    public OrderDTO shipOrder(String orderId) {
        Order order = orderRepository.findByOrderId(new OrderId(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        order.ship();
        order = orderRepository.save(order);
        return orderAssembler.toDTO(order);
    }

    /**
     * 确认送达
     */
    @Transactional
    public OrderDTO deliverOrder(String orderId) {
        Order order = orderRepository.findByOrderId(new OrderId(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        order.deliver();
        order = orderRepository.save(order);
        return orderAssembler.toDTO(order);
    }

    /**
     * 退款
     */
    @Transactional
    public OrderDTO refundOrder(String orderId) {
        Order order = orderRepository.findByOrderId(new OrderId(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 如果有优惠券，回滚优惠券
        if (order.hasCoupon()) {
            Coupon coupon = couponRepository.findByCouponId(order.getCouponId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
            coupon.unuse();
            couponRepository.save(coupon);
            order.removeCoupon();
        }

        order.refund();
        order = orderRepository.save(order);
        return orderAssembler.toDTO(order);
    }
}
