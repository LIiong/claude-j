package com.claudej.adapter.payment.web.request;

import lombok.Data;

/**
 * 退款请求
 */
@Data
public class RefundPaymentRequest {

    private String reason;
}