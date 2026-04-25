package com.claudej.application.inventory.command;

import lombok.Data;

/**
 * 预占库存命令（订单创建时调用）
 */
@Data
public class ReserveStockCommand {

    private String productId;
    private int quantity;
}