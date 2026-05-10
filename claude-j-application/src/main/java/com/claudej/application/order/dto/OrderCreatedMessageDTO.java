package com.claudej.application.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderCreatedMessageDTO {

    private String eventId;
    private String orderId;
    private String customerId;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDateTime occurredOn;
}
