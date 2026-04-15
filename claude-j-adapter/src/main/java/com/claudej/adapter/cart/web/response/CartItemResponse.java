package com.claudej.adapter.cart.web.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车项响应
 */
@Data
public class CartItemResponse {

    private String productId;
    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
