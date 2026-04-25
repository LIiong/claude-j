package com.claudej.application.product.command;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建商品命令
 */
@Data
public class CreateProductCommand {

    private String name;
    private String skuCode;
    private int stock;
    private BigDecimal originalPrice;
    private BigDecimal promotionalPrice;
    private String description;
}