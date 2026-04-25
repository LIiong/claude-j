package com.claudej.application.inventory.command;

import lombok.Data;

/**
 * 扣减库存命令（支付成功时调用）
 */
@Data
public class DeductStockCommand {

    private String productId;
    private int quantity;
}