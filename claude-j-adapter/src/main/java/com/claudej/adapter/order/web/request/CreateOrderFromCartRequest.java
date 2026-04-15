package com.claudej.adapter.order.web.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 从购物车创建订单请求
 */
@Data
public class CreateOrderFromCartRequest {

    /**
     * 客户ID
     */
    @NotBlank(message = "客户ID不能为空")
    private String customerId;

    /**
     * 优惠券ID（可选，预留扩展）
     */
    private String couponId;
}
