package com.claudej.adapter.cart.web.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 购物车响应
 */
@Data
public class CartResponse {

    private String userId;
    private List<CartItemResponse> items;
    private BigDecimal totalAmount;
    private String currency;
    private Integer itemCount;
    private LocalDateTime updateTime;
}
