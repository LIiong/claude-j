package com.claudej.application.inventory.service;

import com.claudej.application.inventory.assembler.InventoryAssembler;
import com.claudej.application.inventory.command.AdjustStockCommand;
import com.claudej.application.inventory.command.CreateInventoryCommand;
import com.claudej.application.inventory.dto.InventoryDTO;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.inventory.model.aggregate.Inventory;
import com.claudej.domain.inventory.model.valobj.InventoryId;
import com.claudej.domain.inventory.model.valobj.SkuCode;
import com.claudej.domain.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存应用服务
 */
@Service
public class InventoryApplicationService {

    private final InventoryRepository inventoryRepository;
    private final InventoryAssembler inventoryAssembler;

    public InventoryApplicationService(InventoryRepository inventoryRepository,
                                        InventoryAssembler inventoryAssembler) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryAssembler = inventoryAssembler;
    }

    /**
     * 创建库存
     */
    @Transactional
    public InventoryDTO createInventory(CreateInventoryCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVENTORY_NOT_FOUND, "创建库存命令不能为空");
        }
        if (command.getProductId() == null || command.getProductId().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_ID_EMPTY, "商品ID不能为空");
        }
        if (command.getSkuCode() == null || command.getSkuCode().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.SKU_CODE_EMPTY, "SKU编码不能为空");
        }
        if (command.getInitialStock() < 0) {
            throw new BusinessException(ErrorCode.STOCK_NEGATIVE, "初始库存不能为负数");
        }

        Inventory inventory = Inventory.create(
                command.getProductId(),
                new SkuCode(command.getSkuCode()),
                command.getInitialStock()
        );

        inventory = inventoryRepository.save(inventory);
        return inventoryAssembler.toDTO(inventory);
    }

    /**
     * 根据库存ID查询
     */
    public InventoryDTO getInventoryById(String inventoryId) {
        Inventory inventory = inventoryRepository.findByInventoryId(new InventoryId(inventoryId))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));
        return inventoryAssembler.toDTO(inventory);
    }

    /**
     * 根据商品ID查询
     */
    public InventoryDTO getInventoryByProductId(String productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));
        return inventoryAssembler.toDTO(inventory);
    }

    /**
     * 根据SKU编码查询
     */
    public InventoryDTO getInventoryBySkuCode(String skuCode) {
        Inventory inventory = inventoryRepository.findBySkuCode(new SkuCode(skuCode))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));
        return inventoryAssembler.toDTO(inventory);
    }

    /**
     * 调整库存（管理员操作）
     */
    @Transactional
    public InventoryDTO adjustStock(AdjustStockCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVENTORY_NOT_FOUND, "调整库存命令不能为空");
        }

        InventoryId inventoryId = new InventoryId(command.getInventoryId());
        Inventory inventory = inventoryRepository.findByInventoryId(inventoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));

        inventory.adjustStock(command.getAdjustment());
        inventory = inventoryRepository.save(inventory);
        return inventoryAssembler.toDTO(inventory);
    }

    /**
     * 预占库存（订单创建时调用）
     */
    @Transactional
    public void reserveStock(String productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));
        inventory.reserve(quantity);
        inventoryRepository.save(inventory);
    }

    /**
     * 扣减库存（支付成功时调用）
     */
    @Transactional
    public void deductStock(String productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));
        inventory.deduct(quantity);
        inventoryRepository.save(inventory);
    }

    /**
     * 回滚库存（取消订单时调用）
     */
    @Transactional
    public void releaseStock(String productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));
        inventory.release(quantity);
        inventoryRepository.save(inventory);
    }
}