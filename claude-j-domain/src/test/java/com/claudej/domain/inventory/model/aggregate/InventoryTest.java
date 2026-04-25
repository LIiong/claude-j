package com.claudej.domain.inventory.model.aggregate;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.inventory.model.valobj.InventoryId;
import com.claudej.domain.inventory.model.valobj.SkuCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Inventory 聚合根测试
 */
class InventoryTest {

    private static final String PRODUCT_ID = "PROD-001";
    private static final String SKU_CODE = "SKU-001";
    private static final int INITIAL_STOCK = 100;

    @Test
    void should_create_when_validParameters() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        assertThat(inventory.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(inventory.getSkuCode().getValue()).isEqualTo(SKU_CODE);
        assertThat(inventory.getAvailableStock()).isEqualTo(INITIAL_STOCK);
        assertThat(inventory.getReservedStock()).isEqualTo(0);
        assertThat(inventory.getInventoryId()).isNotNull();
    }

    @Test
    void should_throw_when_productId_is_null() {
        assertThatThrownBy(() -> Inventory.create(null, new SkuCode(SKU_CODE), INITIAL_STOCK))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_ID_EMPTY);
    }

    @Test
    void should_throw_when_productId_is_empty() {
        assertThatThrownBy(() -> Inventory.create("", new SkuCode(SKU_CODE), INITIAL_STOCK))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_ID_EMPTY);
    }

    @Test
    void should_throw_when_skuCode_is_null() {
        assertThatThrownBy(() -> Inventory.create(PRODUCT_ID, null, INITIAL_STOCK))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SKU_CODE_EMPTY);
    }

    @Test
    void should_throw_when_initialStock_negative() {
        assertThatThrownBy(() -> Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), -1))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STOCK_NEGATIVE);
    }

    @Test
    void should_create_when_initialStock_is_zero() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), 0);
        assertThat(inventory.getAvailableStock()).isEqualTo(0);
    }

    @Test
    void should_reserve_when_stockAvailable() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        inventory.reserve(10);
        assertThat(inventory.getAvailableStock()).isEqualTo(90);
        assertThat(inventory.getReservedStock()).isEqualTo(10);
    }

    @Test
    void should_throw_when_reserve_quantity_negative() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        assertThatThrownBy(() -> inventory.reserve(-1))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESERVE_NEGATIVE);
    }

    @Test
    void should_throw_when_reserve_quantity_zero() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        assertThatThrownBy(() -> inventory.reserve(0))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESERVE_NEGATIVE);
    }

    @Test
    void should_throw_when_reserve_exceedsAvailable() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        assertThatThrownBy(() -> inventory.reserve(101))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVENTORY_INSUFFICIENT);
    }

    @Test
    void should_deduct_when_reservedEnough() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        inventory.reserve(10);
        inventory.deduct(10);
        assertThat(inventory.getAvailableStock()).isEqualTo(90);
        assertThat(inventory.getReservedStock()).isEqualTo(0);
    }

    @Test
    void should_deduct_partially_when_reservedEnough() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        inventory.reserve(10);
        inventory.deduct(5);
        assertThat(inventory.getAvailableStock()).isEqualTo(90);
        assertThat(inventory.getReservedStock()).isEqualTo(5);
    }

    @Test
    void should_throw_when_deduct_quantity_negative() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        inventory.reserve(10);
        assertThatThrownBy(() -> inventory.deduct(-1))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEDUCT_NEGATIVE);
    }

    @Test
    void should_throw_when_deduct_quantity_zero() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        inventory.reserve(10);
        assertThatThrownBy(() -> inventory.deduct(0))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEDUCT_NEGATIVE);
    }

    @Test
    void should_throw_when_deduct_exceedsReserved() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        inventory.reserve(10);
        assertThatThrownBy(() -> inventory.deduct(11))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEDUCT_EXCEEDS_RESERVED);
    }

    @Test
    void should_throw_when_deduct_without_reserve() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        assertThatThrownBy(() -> inventory.deduct(1))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEDUCT_EXCEEDS_RESERVED);
    }

    @Test
    void should_release_when_reservedEnough() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        inventory.reserve(10);
        inventory.release(10);
        assertThat(inventory.getAvailableStock()).isEqualTo(100);
        assertThat(inventory.getReservedStock()).isEqualTo(0);
    }

    @Test
    void should_release_partially_when_reservedEnough() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        inventory.reserve(10);
        inventory.release(5);
        assertThat(inventory.getAvailableStock()).isEqualTo(95);
        assertThat(inventory.getReservedStock()).isEqualTo(5);
    }

    @Test
    void should_throw_when_release_quantity_negative() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        inventory.reserve(10);
        assertThatThrownBy(() -> inventory.release(-1))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RELEASE_NEGATIVE);
    }

    @Test
    void should_throw_when_release_quantity_zero() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        inventory.reserve(10);
        assertThatThrownBy(() -> inventory.release(0))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RELEASE_NEGATIVE);
    }

    @Test
    void should_throw_when_release_exceedsReserved() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        inventory.reserve(10);
        assertThatThrownBy(() -> inventory.release(11))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RELEASE_EXCEEDS_RESERVED);
    }

    @Test
    void should_throw_when_release_without_reserve() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        assertThatThrownBy(() -> inventory.release(1))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RELEASE_EXCEEDS_RESERVED);
    }

    @Test
    void should_reconstruct_when_validParameters() {
        InventoryId inventoryId = new InventoryId("INV-001");
        Inventory inventory = Inventory.reconstruct(
                1L, inventoryId, PRODUCT_ID, new SkuCode(SKU_CODE), 100, 10
        );
        assertThat(inventory.getId()).isEqualTo(1L);
        assertThat(inventory.getInventoryId()).isEqualTo(inventoryId);
        assertThat(inventory.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(inventory.getAvailableStock()).isEqualTo(100);
        assertThat(inventory.getReservedStock()).isEqualTo(10);
    }

    @Test
    void should_setId_when_persisted() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        inventory.setId(1L);
        assertThat(inventory.getId()).isEqualTo(1L);
    }

    @Test
    void should_adjustStock_when_positive() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        inventory.adjustStock(50);
        assertThat(inventory.getAvailableStock()).isEqualTo(150);
    }

    @Test
    void should_adjustStock_when_negative() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        inventory.adjustStock(-50);
        assertThat(inventory.getAvailableStock()).isEqualTo(50);
    }

    @Test
    void should_throw_when_adjustStock_result_negative() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), 10);
        assertThatThrownBy(() -> inventory.adjustStock(-11))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STOCK_NEGATIVE);
    }

    @Test
    void should_check_availableQuantity() {
        Inventory inventory = Inventory.create(PRODUCT_ID, new SkuCode(SKU_CODE), INITIAL_STOCK);
        inventory.reserve(10);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(90);
    }
}