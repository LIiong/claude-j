package com.claudej.application.inventory.assembler;

import com.claudej.application.inventory.dto.InventoryDTO;
import com.claudej.domain.inventory.model.aggregate.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Inventory 转换器
 */
@Mapper(componentModel = "spring")
public interface InventoryAssembler {

    /**
     * Domain 转 DTO
     */
    @Mapping(target = "inventoryId", expression = "java(inventory.getInventoryIdValue())")
    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "skuCode", expression = "java(inventory.getSkuCodeValue())")
    InventoryDTO toDTO(Inventory inventory);

    /**
     * Domain 列表转 DTO 列表
     */
    List<InventoryDTO> toDTOList(List<Inventory> inventories);
}