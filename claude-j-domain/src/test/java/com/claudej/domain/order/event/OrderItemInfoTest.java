package com.claudej.domain.order.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OrderItemInfo value object tests
 */
class OrderItemInfoTest {

    @Test
    void should_create_when_all_fields_valid() {
        OrderItemInfo info = new OrderItemInfo("PROD001", "iPhone", 2);

        assertThat(info.getProductId()).isEqualTo("PROD001");
        assertThat(info.getProductName()).isEqualTo("iPhone");
        assertThat(info.getQuantity()).isEqualTo(2);
    }

    @Test
    void should_throw_when_product_id_is_null() {
        assertThatThrownBy(() -> new OrderItemInfo(null, "iPhone", 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("productId must not be null or empty");
    }

    @Test
    void should_throw_when_product_id_is_empty() {
        assertThatThrownBy(() -> new OrderItemInfo("", "iPhone", 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("productId must not be null or empty");
    }

    @Test
    void should_throw_when_product_name_is_null() {
        assertThatThrownBy(() -> new OrderItemInfo("PROD001", null, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("productName must not be null or empty");
    }

    @Test
    void should_throw_when_product_name_is_empty() {
        assertThatThrownBy(() -> new OrderItemInfo("PROD001", "", 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("productName must not be null or empty");
    }

    @Test
    void should_throw_when_quantity_is_zero() {
        assertThatThrownBy(() -> new OrderItemInfo("PROD001", "iPhone", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quantity must be positive");
    }

    @Test
    void should_throw_when_quantity_is_negative() {
        assertThatThrownBy(() -> new OrderItemInfo("PROD001", "iPhone", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quantity must be positive");
    }

    @Test
    void should_equal_when_all_fields_match() {
        OrderItemInfo info1 = new OrderItemInfo("PROD001", "iPhone", 2);
        OrderItemInfo info2 = new OrderItemInfo("PROD001", "iPhone", 2);

        assertThat(info1).isEqualTo(info2);
        assertThat(info1.hashCode()).isEqualTo(info2.hashCode());
    }

    @Test
    void should_not_equal_when_fields_differ() {
        OrderItemInfo info1 = new OrderItemInfo("PROD001", "iPhone", 2);
        OrderItemInfo info2 = new OrderItemInfo("PROD002", "iPhone", 2);

        assertThat(info1).isNotEqualTo(info2);
    }
}
