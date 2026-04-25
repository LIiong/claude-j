package com.claudej.application.inventory.command;

import lombok.Data;

/**
 * 创建库存命令
 */
@Data
public class CreateInventoryCommand {

    private String productId;
    private String skuCode;
    private int initialStock;
}