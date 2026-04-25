package com.claudej.application.inventory.command;

import lombok.Data;

/**
 * 调整库存命令（管理员操作）
 */
@Data
public class AdjustStockCommand {

    private String inventoryId;
    private int adjustment;
    private String reason;
}