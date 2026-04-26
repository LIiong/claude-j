package com.claudej.adapter.payment.web.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付响应
 */
@Data
public class PaymentResponse {

    private String paymentId;
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String method;
    private String transactionNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}