package com.claudej.application.inventory.command;

import lombok.Data;

/**
 * 回滚库存命令（取消订单时调用）
 */
@Data
public class ReleaseStockCommand {

    private String productId;
    private int quantity;
}