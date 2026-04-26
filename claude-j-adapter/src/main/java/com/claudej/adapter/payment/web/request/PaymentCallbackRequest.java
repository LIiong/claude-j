package com.claudej.adapter.payment.web.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 支付回调请求
 */
@Data
public class PaymentCallbackRequest {

    @NotBlank(message = "订单ID不能为空")
    private String orderId;

    @NotBlank(message = "交易号不能为空")
    private String transactionNo;

    private boolean success;

    private String message;
}