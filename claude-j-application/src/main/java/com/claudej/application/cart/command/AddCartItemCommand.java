package com.claudej.application.cart.command;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 添加购物车商品命令
 */
@Data
public class AddCartItemCommand {

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotBlank(message = "商品ID不能为空")
    private String productId;

    @NotBlank(message = "商品名称不能为空")
    private String productName;

    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.01", message = "单价必须大于0")
    private BigDecimal unitPrice;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量最小为1")
    @Max(value = 999, message = "数量最大为999")
    private Integer quantity;
}
