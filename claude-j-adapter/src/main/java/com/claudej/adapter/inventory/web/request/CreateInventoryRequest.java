package com.claudej.adapter.inventory.web.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;

/**
 * 创建库存请求
 */
@Data
public class CreateInventoryRequest {

    @NotBlank(message = "商品ID不能为空")
    private String productId;

    @NotBlank(message = "SKU编码不能为空")
    private String skuCode;

    @PositiveOrZero(message = "初始库存不能为负数")
    private Integer initialStock;
}