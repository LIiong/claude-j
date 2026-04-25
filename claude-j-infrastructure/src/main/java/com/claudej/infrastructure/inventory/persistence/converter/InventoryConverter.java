package com.claudej.infrastructure.inventory.persistence.converter;

import com.claudej.domain.inventory.model.aggregate.Inventory;
import com.claudej.domain.inventory.model.valobj.InventoryId;
import com.claudej.domain.inventory.model.valobj.SkuCode;
import com.claudej.infrastructure.inventory.persistence.dataobject.InventoryDO;
import org.springframework.stereotype.Component;

/**
 * Inventory 转换器
 */
@Component
public class InventoryConverter {

    /**
     * Inventory DO 转 Domain
     */
    public Inventory toDomain(InventoryDO inventoryDO) {
        if (inventoryDO == null) {
            return null;
        }
        return Inventory.reconstruct(
                inventoryDO.getId(),
                new InventoryId(inventoryDO.getInventoryId()),
                inventoryDO.getProductId(),
                new SkuCode(inventoryDO.getSkuCode()),
                inventoryDO.getAvailableStock(),
                inventoryDO.getReservedStock()
        );
    }

    /**
     * Inventory Domain 转 DO
     */
    public InventoryDO toDO(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        InventoryDO inventoryDO = new InventoryDO();
        inventoryDO.setId(inventory.getId());
        inventoryDO.setInventoryId(inventory.getInventoryIdValue());
        inventoryDO.setProductId(inventory.getProductId());
        inventoryDO.setSkuCode(inventory.getSkuCodeValue());
        inventoryDO.setAvailableStock(inventory.getAvailableStock());
        inventoryDO.setReservedStock(inventory.getReservedStock());
        inventoryDO.setCreateTime(inventory.getCreateTime());
        inventoryDO.setUpdateTime(inventory.getUpdateTime());
        inventoryDO.setDeleted(0);
        return inventoryDO;
    }
}