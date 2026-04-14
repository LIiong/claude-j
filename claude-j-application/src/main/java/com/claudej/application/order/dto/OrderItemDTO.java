package com.claudej.application.order.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单项 DTO
 */
@Data
public class OrderItemDTO {

    private String productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}
