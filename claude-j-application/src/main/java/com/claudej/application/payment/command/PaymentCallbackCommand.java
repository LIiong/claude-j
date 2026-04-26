package com.claudej.application.payment.command;

import lombok.Data;

/**
 * 支付回调命令
 */
@Data
public class PaymentCallbackCommand {

    private String orderId;
    private String transactionNo;
    private boolean success;
    private String message;
}