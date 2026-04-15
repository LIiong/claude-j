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
import com.claudej.domain.order.model.aggregate.Order;
import com.claudej.domain.order.model.entity.OrderItem;
import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.order.model.valobj.Money;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.domain.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 订单应用服务
 */
@Service
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final OrderAssembler orderAssembler;

    public OrderApplicationService(OrderRepository orderRepository, CartRepository cartRepository,
                                   OrderAssembler orderAssembler) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
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

        for (CreateOrderCommand.OrderItemCommand itemCmd : command.getItems()) {
            OrderItem item = OrderItem.create(
                    itemCmd.getProductId(),
                    itemCmd.getProductName(),
                    itemCmd.getQuantity(),
                    new Money(itemCmd.getUnitPrice(), "CNY")
            );
            order.addItem(item);
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

        // 5. 保存订单
        order = orderRepository.save(order);

        // 6. 清空购物车并保存
        cart.clear();
        cartRepository.save(cart);

        // 7. 返回订单DTO
        return orderAssembler.toDTO(order);
    }
}
