package com.claudej.adapter.product.web.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * 调价请求
 */
@Data
public class UpdatePriceRequest {

    @NotNull(message = "原价不能为空")
    @Positive(message = "原价必须大于0")
    private BigDecimal originalPrice;

    private BigDecimal promotionalPrice;
}