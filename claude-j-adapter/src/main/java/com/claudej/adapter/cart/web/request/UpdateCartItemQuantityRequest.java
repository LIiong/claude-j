package com.claudej.adapter.cart.web.request;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 更新购物车商品数量请求
 */
@Data
public class UpdateCartItemQuantityRequest {

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotBlank(message = "商品ID不能为空")
    private String productId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量最小为1")
    @Max(value = 999, message = "数量最大为999")
    private Integer quantity;
}
