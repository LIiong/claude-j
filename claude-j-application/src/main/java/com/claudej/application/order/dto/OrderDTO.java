package com.claudej.application.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单 DTO
 */
@Data
public class OrderDTO {

    private String orderId;
    private String customerId;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String couponId;
    private String currency;
    private List<OrderItemDTO> items;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
