package com.claudej.application.payment.command;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建支付命令
 */
@Data
public class CreatePaymentCommand {

    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private String method;
}