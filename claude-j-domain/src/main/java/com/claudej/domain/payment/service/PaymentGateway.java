package com.claudej.domain.payment.service;

import com.claudej.domain.order.model.valobj.Money;
import com.claudej.domain.payment.model.aggregate.Payment;
import com.claudej.domain.payment.model.valobj.PaymentResult;

/**
 * 支付网关端口接口 - PSP (Payment Service Provider) 抽象
 */
public interface PaymentGateway {

    /**
     * 创建支付订单（调用第三方支付平台）
     */
    PaymentResult createPayment(Payment payment);

    /**
     * 查询支付状态
     */
    PaymentResult queryPayment(String transactionNo);

    /**
     * 退款
     */
    PaymentResult refundPayment(String transactionNo, Money amount);
}