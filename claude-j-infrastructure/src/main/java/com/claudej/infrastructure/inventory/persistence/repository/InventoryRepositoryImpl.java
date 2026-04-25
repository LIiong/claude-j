package com.claudej.infrastructure.inventory.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.claudej.domain.inventory.model.aggregate.Inventory;
import com.claudej.domain.inventory.model.valobj.InventoryId;
import com.claudej.domain.inventory.model.valobj.SkuCode;
import com.claudej.domain.inventory.repository.InventoryRepository;
import com.claudej.infrastructure.inventory.persistence.converter.InventoryConverter;
import com.claudej.infrastructure.inventory.persistence.dataobject.InventoryDO;
import com.claudej.infrastructure.inventory.persistence.mapper.InventoryMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 库存 Repository 实现
 */
@Repository
public class InventoryRepositoryImpl implements InventoryRepository {

    private final InventoryMapper inventoryMapper;
    private final InventoryConverter inventoryConverter;

    public InventoryRepositoryImpl(InventoryMapper inventoryMapper, InventoryConverter inventoryConverter) {
        this.inventoryMapper = inventoryMapper;
        this.inventoryConverter = inventoryConverter;
    }

    @Override
    @Transactional
    public Inventory save(Inventory inventory) {
        if (inventory.getId() == null) {
            // 新增库存
            InventoryDO inventoryDO = inventoryConverter.toDO(inventory);
            inventoryDO.setCreateTime(LocalDateTime.now());
            inventoryDO.setUpdateTime(LocalDateTime.now());
            inventoryDO.setDeleted(0);
            inventoryMapper.insert(inventoryDO);
            inventory.setId(inventoryDO.getId());
        } else {
            // 更新库存
            InventoryDO inventoryDO = inventoryConverter.toDO(inventory);
            inventoryDO.setUpdateTime(LocalDateTime.now());
            inventoryMapper.updateById(inventoryDO);
        }
        return inventory;
    }

    @Override
    public Optional<Inventory> findByInventoryId(InventoryId inventoryId) {
        LambdaQueryWrapper<InventoryDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryDO::getInventoryId, inventoryId.getValue());
        InventoryDO inventoryDO = inventoryMapper.selectOne(wrapper);
        return Optional.ofNullable(inventoryConverter.toDomain(inventoryDO));
    }

    @Override
    public Optional<Inventory> findByProductId(String productId) {
        LambdaQueryWrapper<InventoryDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryDO::getProductId, productId);
        InventoryDO inventoryDO = inventoryMapper.selectOne(wrapper);
        return Optional.ofNullable(inventoryConverter.toDomain(inventoryDO));
    }

    @Override
    public Optional<Inventory> findBySkuCode(SkuCode skuCode) {
        LambdaQueryWrapper<InventoryDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryDO::getSkuCode, skuCode.getValue());
        InventoryDO inventoryDO = inventoryMapper.selectOne(wrapper);
        return Optional.ofNullable(inventoryConverter.toDomain(inventoryDO));
    }

    @Override
    public boolean existsByInventoryId(InventoryId inventoryId) {
        LambdaQueryWrapper<InventoryDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryDO::getInventoryId, inventoryId.getValue());
        return inventoryMapper.selectCount(wrapper) > 0;
    }
}