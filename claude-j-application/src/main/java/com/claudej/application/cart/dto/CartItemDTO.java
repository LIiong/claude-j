package com.claudej.application.cart.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车项 DTO
 */
@Data
public class CartItemDTO {

    private String productId;
    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
