package com.claudej.domain.inventory.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * InventoryId 值对象测试
 */
class InventoryIdTest {

    @Test
    void should_throw_when_value_is_null() {
        assertThatThrownBy(() -> new InventoryId(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVENTORY_ID_EMPTY);
    }

    @Test
    void should_throw_when_value_is_empty() {
        assertThatThrownBy(() -> new InventoryId(""))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVENTORY_ID_EMPTY);
    }

    @Test
    void should_throw_when_value_is_blank() {
        assertThatThrownBy(() -> new InventoryId("   "))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVENTORY_ID_EMPTY);
    }

    @Test
    void should_create_when_value_is_valid() {
        InventoryId inventoryId = new InventoryId("INV-001");
        assertThat(inventoryId.getValue()).isEqualTo("INV-001");
    }

    @Test
    void should_trim_when_value_has_whitespace() {
        InventoryId inventoryId = new InventoryId("  INV-001  ");
        assertThat(inventoryId.getValue()).isEqualTo("INV-001");
    }

    @Test
    void should_equal_when_values_match() {
        InventoryId id1 = new InventoryId("INV-001");
        InventoryId id2 = new InventoryId("INV-001");
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    void should_not_equal_when_values_differ() {
        InventoryId id1 = new InventoryId("INV-001");
        InventoryId id2 = new InventoryId("INV-002");
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void should_toString_contain_value() {
        InventoryId inventoryId = new InventoryId("INV-001");
        assertThat(inventoryId.toString()).contains("INV-001");
    }
}