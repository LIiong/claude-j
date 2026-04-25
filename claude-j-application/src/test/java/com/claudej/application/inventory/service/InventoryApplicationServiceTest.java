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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * InventoryApplicationService 测试
 */
@ExtendWith(MockitoExtension.class)
class InventoryApplicationServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryAssembler inventoryAssembler;

    @InjectMocks
    private InventoryApplicationService service;

    private static final String PRODUCT_ID = "PROD-001";
    private static final String SKU_CODE = "SKU-001";
    private static final int INITIAL_STOCK = 100;

    @Test
    void should_createInventory_when_commandValid() {
        // Arrange
        CreateInventoryCommand command = new CreateInventoryCommand();
        command.setProductId(PRODUCT_ID);
        command.setSkuCode(SKU_CODE);
        command.setInitialStock(INITIAL_STOCK);

        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        InventoryDTO expectedDTO = createInventoryDTO(inventory);

        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(inventoryAssembler.toDTO(any(Inventory.class))).thenReturn(expectedDTO);

        // Act
        InventoryDTO result = service.createInventory(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(result.getSkuCode()).isEqualTo(SKU_CODE);
        assertThat(result.getAvailableStock()).isEqualTo(INITIAL_STOCK);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void should_throw_when_createInventory_commandNull() {
        assertThatThrownBy(() -> service.createInventory(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVENTORY_NOT_FOUND);
    }

    @Test
    void should_throw_when_createInventory_productIdNull() {
        CreateInventoryCommand command = new CreateInventoryCommand();
        command.setProductId(null);
        command.setSkuCode(SKU_CODE);
        command.setInitialStock(INITIAL_STOCK);

        assertThatThrownBy(() -> service.createInventory(command))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_ID_EMPTY);
    }

    @Test
    void should_throw_when_createInventory_skuCodeNull() {
        CreateInventoryCommand command = new CreateInventoryCommand();
        command.setProductId(PRODUCT_ID);
        command.setSkuCode(null);
        command.setInitialStock(INITIAL_STOCK);

        assertThatThrownBy(() -> service.createInventory(command))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SKU_CODE_EMPTY);
    }

    @Test
    void should_throw_when_createInventory_stockNegative() {
        CreateInventoryCommand command = new CreateInventoryCommand();
        command.setProductId(PRODUCT_ID);
        command.setSkuCode(SKU_CODE);
        command.setInitialStock(-1);

        assertThatThrownBy(() -> service.createInventory(command))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STOCK_NEGATIVE);
    }

    @Test
    void should_getInventory_when_idExists() {
        // Arrange
        String inventoryIdStr = "INV-001";
        InventoryId inventoryId = new InventoryId(inventoryIdStr);
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        InventoryDTO expectedDTO = createInventoryDTO(inventory);

        when(inventoryRepository.findByInventoryId(inventoryId)).thenReturn(Optional.of(inventory));
        when(inventoryAssembler.toDTO(inventory)).thenReturn(expectedDTO);

        // Act
        InventoryDTO result = service.getInventoryById(inventoryIdStr);

        // Assert
        assertThat(result).isNotNull();
        verify(inventoryRepository).findByInventoryId(inventoryId);
    }

    @Test
    void should_throw_when_getInventory_notFound() {
        String inventoryIdStr = "INV-NOT-EXIST";
        InventoryId inventoryId = new InventoryId(inventoryIdStr);

        when(inventoryRepository.findByInventoryId(inventoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getInventoryById(inventoryIdStr))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVENTORY_NOT_FOUND);
    }

    @Test
    void should_getInventoryByProductId_when_exists() {
        // Arrange
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        InventoryDTO expectedDTO = createInventoryDTO(inventory);

        when(inventoryRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(inventory));
        when(inventoryAssembler.toDTO(inventory)).thenReturn(expectedDTO);

        // Act
        InventoryDTO result = service.getInventoryByProductId(PRODUCT_ID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    void should_throw_when_getInventoryByProductId_notFound() {
        when(inventoryRepository.findByProductId("PROD-NOT-EXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getInventoryByProductId("PROD-NOT-EXIST"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVENTORY_NOT_FOUND);
    }

    @Test
    void should_getInventoryBySkuCode_when_exists() {
        // Arrange
        SkuCode skuCode = new SkuCode(SKU_CODE);
        Inventory inventory = Inventory.create(PRODUCT_ID, skuCode, INITIAL_STOCK);
        InventoryDTO expectedDTO = createInventoryDTO(inventory);

        when(inventoryRepository.findBySkuCode(skuCode)).thenReturn(Optional.of(inventory));
        when(inventoryAssembler.toDTO(inventory)).thenReturn(expectedDTO);

        // Act
        InventoryDTO result = service.getInventoryBySkuCode(SKU_CODE);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSkuCode()).isEqualTo(SKU_CODE);
    }

    @Test
    void should_adjustStock_when_commandValid() {
        // Arrange
        String inventoryIdStr = "INV-001";
        AdjustStockCommand command = new AdjustStockCommand();
        command.setInventoryId(inventoryIdStr);
        command.setAdjustment(50);
        command.setReason("入库");

        InventoryId inventoryId = new InventoryId(inventoryIdStr);
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        inventory.setId(1L);

        when(inventoryRepository.findByInventoryId(inventoryId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(inventoryAssembler.toDTO(any(Inventory.class))).thenReturn(createInventoryDTO(inventory));

        // Act
        InventoryDTO result = service.adjustStock(command);

        // Assert
        assertThat(result).isNotNull();
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void should_reserveStock_when_stockAvailable() {
        // Arrange
        int reserveQuantity = 10;
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        inventory.setId(1L);

        when(inventoryRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        // Act
        service.reserveStock(PRODUCT_ID, reserveQuantity);

        // Assert
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void should_throw_when_reserveStock_insufficient() {
        // Arrange
        int reserveQuantity = 101;
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);

        when(inventoryRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(inventory));

        // Act & Assert
        assertThatThrownBy(() -> service.reserveStock(PRODUCT_ID, reserveQuantity))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVENTORY_INSUFFICIENT);
    }

    @Test
    void should_throw_when_reserveStock_inventoryNotFound() {
        when(inventoryRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reserveStock(PRODUCT_ID, 10))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVENTORY_NOT_FOUND);
    }

    @Test
    void should_deductStock_when_reservedEnough() {
        // Arrange
        int reserveQuantity = 10;
        int deductQuantity = 5;
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        inventory.reserve(reserveQuantity);

        when(inventoryRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        // Act
        service.deductStock(PRODUCT_ID, deductQuantity);

        // Assert
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void should_releaseStock_when_reservedEnough() {
        // Arrange
        int reserveQuantity = 10;
        int releaseQuantity = 5;
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        inventory.reserve(reserveQuantity);

        when(inventoryRepository.findByProductId(PRODUCT_ID)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        // Act
        service.releaseStock(PRODUCT_ID, releaseQuantity);

        // Assert
        verify(inventoryRepository).save(any(Inventory.class));
    }

    private InventoryDTO createInventoryDTO(Inventory inventory) {
        InventoryDTO dto = new InventoryDTO();
        dto.setInventoryId(inventory.getInventoryIdValue());
        dto.setProductId(inventory.getProductId());
        dto.setSkuCode(inventory.getSkuCodeValue());
        dto.setAvailableStock(inventory.getAvailableStock());
        dto.setReservedStock(inventory.getReservedStock());
        return dto;
    }
}