package com.claudej.domain.inventory.repository;

import com.claudej.domain.inventory.model.aggregate.Inventory;
import com.claudej.domain.inventory.model.valobj.InventoryId;
import com.claudej.domain.inventory.model.valobj.SkuCode;

import java.util.Optional;

/**
 * 库存 Repository 端口接口
 */
public interface InventoryRepository {

    /**
     * 保存库存
     */
    Inventory save(Inventory inventory);

    /**
     * 根据库存ID查询库存
     */
    Optional<Inventory> findByInventoryId(InventoryId inventoryId);

    /**
     * 根据商品ID查询库存
     */
    Optional<Inventory> findByProductId(String productId);

    /**
     * 根据SKU编码查询库存
     */
    Optional<Inventory> findBySkuCode(SkuCode skuCode);

    /**
     * 检查库存是否存在
     */
    boolean existsByInventoryId(InventoryId inventoryId);
}