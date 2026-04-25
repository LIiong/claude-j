package com.claudej.domain.inventory.model.aggregate;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.inventory.model.valobj.InventoryId;
import com.claudej.domain.inventory.model.valobj.SkuCode;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 库存聚合根 - 封装库存预占/扣减/回滚不变量
 */
@Getter
public class Inventory {

    private Long id;
    private InventoryId inventoryId;
    private String productId;
    private SkuCode skuCode;
    private int availableStock;
    private int reservedStock;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private Inventory(String productId, SkuCode skuCode, int initialStock, LocalDateTime createTime) {
        this.productId = productId;
        this.skuCode = skuCode;
        this.availableStock = initialStock;
        this.reservedStock = 0;
        this.createTime = createTime;
        this.updateTime = createTime;
    }

    /**
     * 工厂方法：创建新库存
     */
    public static Inventory create(String productId, SkuCode skuCode, int initialStock) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_ID_EMPTY, "商品ID不能为空");
        }
        if (skuCode == null) {
            throw new BusinessException(ErrorCode.SKU_CODE_EMPTY, "SKU编码不能为空");
        }
        if (initialStock < 0) {
            throw new BusinessException(ErrorCode.STOCK_NEGATIVE, "库存不能为负数");
        }
        Inventory inventory = new Inventory(productId.trim(), skuCode, initialStock, LocalDateTime.now());
        inventory.inventoryId = new InventoryId(UUID.randomUUID().toString().replace("-", ""));
        return inventory;
    }

    /**
     * 从持久化层重建聚合根
     */
    public static Inventory reconstruct(Long id, InventoryId inventoryId, String productId,
                                         SkuCode skuCode, int availableStock, int reservedStock) {
        Inventory inventory = new Inventory(productId, skuCode, availableStock, LocalDateTime.now());
        inventory.id = id;
        inventory.inventoryId = inventoryId;
        inventory.reservedStock = reservedStock;
        return inventory;
    }

    /**
     * 预占库存（创建订单时调用）
     * 不变量：预占数量 <= 可用库存
     */
    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.RESERVE_NEGATIVE, "预占数量必须大于0");
        }
        if (quantity > availableStock) {
            throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT,
                    "库存不足，可用库存: " + availableStock + ", 预占数量: " + quantity);
        }
        availableStock -= quantity;
        reservedStock += quantity;
        updateTime = LocalDateTime.now();
    }

    /**
     * 扣减库存（支付成功时调用）
     * 不变量：扣减数量 <= 预占库存
     */
    public void deduct(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.DEDUCT_NEGATIVE, "扣减数量必须大于0");
        }
        if (quantity > reservedStock) {
            throw new BusinessException(ErrorCode.DEDUCT_EXCEEDS_RESERVED,
                    "扣减数量超过预占库存，预占库存: " + reservedStock + ", 扣减数量: " + quantity);
        }
        reservedStock -= quantity;
        updateTime = LocalDateTime.now();
    }

    /**
     * 回滚库存（取消订单时调用）
     * 不变量：回滚数量 <= 预占库存
     */
    public void release(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.RELEASE_NEGATIVE, "回滚数量必须大于0");
        }
        if (quantity > reservedStock) {
            throw new BusinessException(ErrorCode.RELEASE_EXCEEDS_RESERVED,
                    "回滚数量超过预占库存，预占库存: " + reservedStock + ", 回滚数量: " + quantity);
        }
        reservedStock -= quantity;
        availableStock += quantity;
        updateTime = LocalDateTime.now();
    }

    /**
     * 调整库存（管理员操作）
     * 不变量：调整后库存 >= 0
     */
    public void adjustStock(int adjustment) {
        int newStock = availableStock + adjustment;
        if (newStock < 0) {
            throw new BusinessException(ErrorCode.STOCK_NEGATIVE,
                    "库存不能为负数，当前库存: " + availableStock + ", 调整量: " + adjustment);
        }
        availableStock = newStock;
        updateTime = LocalDateTime.now();
    }

    /**
     * 设置数据库自增 ID（持久化后回填）
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取实际可用库存（可用 - 预占）
     */
    public int getAvailableQuantity() {
        return availableStock;
    }

    /**
     * 便捷获取库存ID字符串值
     */
    public String getInventoryIdValue() {
        return inventoryId.getValue();
    }

    /**
     * 便捷获取SKU编码字符串值
     */
    public String getSkuCodeValue() {
        return skuCode.getValue();
    }
}