package com.claudej.domain.payment.repository;

import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.domain.payment.model.aggregate.Payment;
import com.claudej.domain.payment.model.valobj.PaymentId;

import java.util.List;
import java.util.Optional;

/**
 * 支付仓库端口接口
 */
public interface PaymentRepository {

    /**
     * 保存支付
     */
    Payment save(Payment payment);

    /**
     * 根据支付ID查询
     */
    Optional<Payment> findByPaymentId(PaymentId paymentId);

    /**
     * 根据订单ID查询
     */
    Optional<Payment> findByOrderId(OrderId orderId);

    /**
     * 检查支付ID是否存在
     */
    boolean existsByPaymentId(PaymentId paymentId);

    /**
     * 根据客户ID查询所有支付
     */
    List<Payment> findByCustomerId(CustomerId customerId);
}