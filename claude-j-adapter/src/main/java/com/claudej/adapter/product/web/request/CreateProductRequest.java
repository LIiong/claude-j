package com.claudej.adapter.product.web.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 创建商品请求
 */
@Data
public class CreateProductRequest {

    @NotBlank(message = "商品名称不能为空")
    @Size(min = 2, max = 100, message = "商品名称长度必须在2-100之间")
    private String name;

    @NotBlank(message = "SKU编码不能为空")
    @Size(max = 32, message = "SKU编码长度不能超过32")
    private String skuCode;

    @NotNull(message = "库存不能为空")
    private Integer stock;

    @NotNull(message = "原价不能为空")
    @Positive(message = "原价必须大于0")
    private BigDecimal originalPrice;

    private BigDecimal promotionalPrice;

    private String description;
}