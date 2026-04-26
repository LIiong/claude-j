package com.claudej.application.payment.service;

import com.claudej.application.payment.assembler.PaymentAssembler;
import com.claudej.application.payment.command.CreatePaymentCommand;
import com.claudej.application.payment.command.PaymentCallbackCommand;
import com.claudej.application.payment.command.RefundPaymentCommand;
import com.claudej.application.payment.dto.PaymentDTO;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.inventory.model.aggregate.Inventory;
import com.claudej.domain.inventory.repository.InventoryRepository;
import com.claudej.domain.order.model.aggregate.Order;
import com.claudej.domain.order.model.entity.OrderItem;
import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.order.model.valobj.Money;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.domain.order.model.valobj.OrderStatus;
import com.claudej.domain.order.repository.OrderRepository;
import com.claudej.domain.payment.model.aggregate.Payment;
import com.claudej.domain.payment.model.valobj.PaymentId;
import com.claudej.domain.payment.model.valobj.PaymentMethod;
import com.claudej.domain.payment.model.valobj.PaymentResult;
import com.claudej.domain.payment.model.valobj.PaymentStatus;
import com.claudej.domain.payment.repository.PaymentRepository;
import com.claudej.domain.payment.service.PaymentGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 支付应用服务
 */
@Service
public class PaymentApplicationService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentAssembler paymentAssembler;

    public PaymentApplicationService(PaymentRepository paymentRepository,
                                      OrderRepository orderRepository,
                                      InventoryRepository inventoryRepository,
                                      PaymentGateway paymentGateway,
                                      PaymentAssembler paymentAssembler) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.inventoryRepository = inventoryRepository;
        this.paymentGateway = paymentGateway;
        this.paymentAssembler = paymentAssembler;
    }

    /**
     * 创建支付
     */
    @Transactional
    public PaymentDTO createPayment(CreatePaymentCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, "创建支付命令不能为空");
        }
        if (command.getOrderId() == null || command.getOrderId().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PAYMENT_ORDER_ID_EMPTY, "订单ID不能为空");
        }
        if (command.getCustomerId() == null || command.getCustomerId().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PAYMENT_CUSTOMER_ID_EMPTY, "客户ID不能为空");
        }
        if (command.getAmount() == null || command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_INVALID, "支付金额无效");
        }
        if (command.getMethod() == null || command.getMethod().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PAYMENT_METHOD_INVALID, "支付方式无效");
        }

        // 解析支付方式
        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(command.getMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PAYMENT_METHOD_INVALID, "支付方式无效");
        }

        // 查询订单
        OrderId orderId = new OrderId(command.getOrderId());
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在"));

        // 检查订单状态
        if (order.isPaid()) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID, "订单已支付");
        }

        // 创建支付
        CustomerId customerId = new CustomerId(command.getCustomerId());
        Money amount = Money.cny(command.getAmount().doubleValue());
        Payment payment = Payment.create(orderId, customerId, amount, method);

        // 调用支付网关创建支付订单
        PaymentResult result = paymentGateway.createPayment(payment);

        // 保存支付
        payment = paymentRepository.save(payment);

        return paymentAssembler.toDTO(payment);
    }

    /**
     * 根据支付ID查询
     */
    public PaymentDTO getPaymentById(String paymentId) {
        PaymentId id = new PaymentId(paymentId);
        Payment payment = paymentRepository.findByPaymentId(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, "支付不存在"));
        return paymentAssembler.toDTO(payment);
    }

    /**
     * 根据订单ID查询支付
     */
    public PaymentDTO getPaymentByOrderId(String orderId) {
        OrderId id = new OrderId(orderId);
        Payment payment = paymentRepository.findByOrderId(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, "支付不存在"));
        return paymentAssembler.toDTO(payment);
    }

    /**
     * 处理支付回调
     */
    @Transactional
    public PaymentDTO handleCallback(PaymentCallbackCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.PAYMENT_STATUS_INVALID, "回调命令不能为空");
        }
        if (command.getOrderId() == null || command.getOrderId().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PAYMENT_ORDER_ID_EMPTY, "订单ID不能为空");
        }
        if (command.getTransactionNo() == null || command.getTransactionNo().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PAYMENT_STATUS_INVALID, "交易号不能为空");
        }

        // 根据 orderId 查询支付
        Optional<Payment> paymentOpt = paymentRepository.findByOrderId(new OrderId(command.getOrderId()));

        if (!paymentOpt.isPresent()) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, "支付不存在");
        }

        Payment payment = paymentOpt.get();

        // 如果支付已处理（非 PENDING 状态），直接返回当前状态（幂等性）
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return paymentAssembler.toDTO(payment);
        }

        if (command.isSuccess()) {
            // 支付成功
            payment.markAsSuccess(command.getTransactionNo());
            payment = paymentRepository.save(payment);

            // 更新订单状态
            Order order = orderRepository.findByOrderId(payment.getOrderId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
            order.pay();
            orderRepository.save(order);

            // 扣减库存（预占转为实际扣减）
            deductStockForOrder(order);
        } else {
            // 支付失败
            payment.markAsFailed(command.getMessage());
            payment = paymentRepository.save(payment);
        }

        return paymentAssembler.toDTO(payment);
    }

    /**
     * 退款
     */
    @Transactional
    public PaymentDTO refundPayment(String paymentId, RefundPaymentCommand command) {
        PaymentId id = new PaymentId(paymentId);
        Payment payment = paymentRepository.findByPaymentId(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, "支付不存在"));

        // 检查支付状态（只有 SUCCESS 状态可以退款）
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.PAYMENT_CANNOT_REFUND, "支付状态不允许退款");
        }

        // 调用支付网关退款
        PaymentResult result = paymentGateway.refundPayment(payment.getTransactionNo(), payment.getAmount());

        // 更新支付状态
        payment.refund();
        payment = paymentRepository.save(payment);

        // 更新订单状态
        Order order = orderRepository.findByOrderId(payment.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 回滚库存（仅 PAID 状态订单，退款前检查）
        if (order.getStatus() == OrderStatus.PAID) {
            releaseStockForOrder(order);
        }

        order.refund();
        orderRepository.save(order);

        return paymentAssembler.toDTO(payment);
    }

    /**
     * 扣减订单库存（支付成功时）
     */
    private void deductStockForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND,
                            "商品库存不存在: " + item.getProductId()));
            inventory.deduct(item.getQuantity());
            inventoryRepository.save(inventory);
        }
    }

    /**
     * 回滚订单库存（退款时）
     * 使用 adjustStock 恢复库存（因为库存已经扣减，reservedStock=0）
     */
    private void releaseStockForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND,
                            "商品库存不存在: " + item.getProductId()));
            // 退款时恢复库存，使用 adjustStock 增加 availableStock
            inventory.adjustStock(item.getQuantity());
            inventoryRepository.save(inventory);
        }
    }
}