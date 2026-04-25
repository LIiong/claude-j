package com.claudej.adapter.product.web.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品响应
 */
@Data
public class ProductResponse {

    private String productId;
    private String name;
    private String description;
    private String skuCode;
    private Integer stock;
    private BigDecimal originalPrice;
    private BigDecimal promotionalPrice;
    private BigDecimal effectivePrice;
    private String currency;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}