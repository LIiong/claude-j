package com.claudej.application.order.command;

import lombok.Data;

/**
 * 从购物车创建订单命令
 */
@Data
public class CreateOrderFromCartCommand {

    /**
     * 客户ID
     */
    private String customerId;

    /**
     * 优惠券ID（可选，预留扩展）
     */
    private String couponId;
}
