package com.claudej.application.cart.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 购物车 DTO
 */
@Data
public class CartDTO {

    private String userId;
    private List<CartItemDTO> items;
    private BigDecimal totalAmount;
    private String currency;
    private Integer itemCount;
    private LocalDateTime updateTime;
}
