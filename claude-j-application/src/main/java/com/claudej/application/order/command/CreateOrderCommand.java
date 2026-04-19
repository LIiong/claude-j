package com.claudej.application.order.command;

import lombok.Data;

import java.util.List;

/**
 * 创建订单命令
 */
@Data
public class CreateOrderCommand {

    private String customerId;
    private String couponId;
    private List<OrderItemCommand> items;

    /**
     * 订单项命令
     */
    @Data
    public static class OrderItemCommand {
        private String productId;
        private String productName;
        private int quantity;
        private java.math.BigDecimal unitPrice;
    }
}
