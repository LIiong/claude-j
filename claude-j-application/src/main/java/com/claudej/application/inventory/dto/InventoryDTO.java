package com.claudej.application.inventory.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存 DTO
 */
@Data
public class InventoryDTO {

    private String inventoryId;
    private String productId;
    private String skuCode;
    private int availableStock;
    private int reservedStock;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}