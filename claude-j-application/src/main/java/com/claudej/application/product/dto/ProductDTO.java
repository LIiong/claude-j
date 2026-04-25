package com.claudej.application.product.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 DTO
 */
@Data
public class ProductDTO {

    private String productId;
    private String name;
    private String description;
    private String skuCode;
    private int stock;
    private BigDecimal originalPrice;
    private BigDecimal promotionalPrice;
    private BigDecimal effectivePrice;
    private String currency;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}