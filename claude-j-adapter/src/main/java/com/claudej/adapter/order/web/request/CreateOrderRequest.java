package com.claudej.adapter.order.web.request;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

/**
 * 创建订单请求
 */
@Data
public class CreateOrderRequest {

    @NotBlank(message = "客户ID不能为空")
    private String customerId;

    @NotEmpty(message = "订单项不能为空")
    @Valid
    private List<OrderItemRequest> items;

    /**
     * 订单项请求
     */
    @Data
    public static class OrderItemRequest {

        @NotBlank(message = "商品ID不能为空")
        private String productId;

        @NotBlank(message = "商品名称不能为空")
        private String productName;

        @NotNull(message = "数量不能为空")
        @Positive(message = "数量必须大于0")
        private Integer quantity;

        @NotNull(message = "单价不能为空")
        @Positive(message = "单价必须大于0")
        private BigDecimal unitPrice;
    }
}
