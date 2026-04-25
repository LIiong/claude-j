package com.claudej.adapter.inventory.web.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存响应
 */
@Data
public class InventoryResponse {

    private String inventoryId;
    private String productId;
    private String skuCode;
    private Integer availableStock;
    private Integer reservedStock;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}