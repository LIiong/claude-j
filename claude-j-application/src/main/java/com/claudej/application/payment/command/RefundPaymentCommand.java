package com.claudej.application.payment.command;

import lombok.Data;

/**
 * 退款命令
 */
@Data
public class RefundPaymentCommand {

    private String reason;
}