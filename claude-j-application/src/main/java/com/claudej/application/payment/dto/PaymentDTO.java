package com.claudej.application.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付 DTO
 */
@Data
public class PaymentDTO {

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