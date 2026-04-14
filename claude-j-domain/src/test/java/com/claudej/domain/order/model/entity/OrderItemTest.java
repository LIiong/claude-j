package com.claudej.domain.order.model.entity;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.order.model.valobj.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderItemTest {

    @Test
    void should_createOrderItem_when_validParametersProvided() {
        // When
        OrderItem item = OrderItem.create("PROD001", "iPhone", 2, Money.cny(5999));

        // Then
        assertThat(item.getProductId()).isEqualTo("PROD001");
        assertThat(item.getProductName()).isEqualTo("iPhone");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getUnitPrice().getAmount().intValue()).isEqualTo(5999);
        assertThat(item.getSubtotal().getAmount().intValue()).isEqualTo(11998);
    }

    @Test
    void should_trimWhitespaceForProductIdAndName() {
        // When
        OrderItem item = OrderItem.create("  PROD001  ", "  iPhone  ", 1, Money.cny(100));

        // Then
        assertThat(item.getProductId()).isEqualTo("PROD001");
        assertThat(item.getProductName()).isEqualTo("iPhone");
    }

    @Test
    void should_throwException_when_productIdIsNull() {
        // When & Then
        assertThatThrownBy(() -> OrderItem.create(null, "iPhone", 1, Money.cny(100)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品ID不能为空");
    }

    @Test
    void should_throwException_when_productIdIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> OrderItem.create("", "iPhone", 1, Money.cny(100)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品ID不能为空");
    }

    @Test
    void should_throwException_when_productNameIsNull() {
        // When & Then
        assertThatThrownBy(() -> OrderItem.create("PROD001", null, 1, Money.cny(100)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品名称不能为空");
    }

    @Test
    void should_throwException_when_productNameIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> OrderItem.create("PROD001", "", 1, Money.cny(100)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品名称不能为空");
    }

    @Test
    void should_throwException_when_quantityIsZero() {
        // When & Then
        assertThatThrownBy(() -> OrderItem.create("PROD001", "iPhone", 0, Money.cny(100)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单项数量必须大于0");
    }

    @Test
    void should_throwException_when_quantityIsNegative() {
        // When & Then
        assertThatThrownBy(() -> OrderItem.create("PROD001", "iPhone", -1, Money.cny(100)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单项数量必须大于0");
    }

    @Test
    void should_throwException_when_unitPriceIsNull() {
        // When & Then
        assertThatThrownBy(() -> OrderItem.create("PROD001", "iPhone", 1, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单项单价不能为负");
    }

    @Test
    void should_calculateSubtotalCorrectly() {
        // Given & When
        OrderItem item1 = OrderItem.create("PROD001", "Item1", 3, Money.cny(100));
        OrderItem item2 = OrderItem.create("PROD002", "Item2", 1, Money.cny(5999.99));

        // Then
        assertThat(item1.getSubtotal().getAmount().intValue()).isEqualTo(300);
        assertThat(item2.getSubtotal().getAmount()).isEqualByComparingTo(java.math.BigDecimal.valueOf(5999.99));
    }

    @Test
    void should_reconstructOrderItem() {
        // When
        OrderItem item = OrderItem.reconstruct("PROD001", "iPhone", 2, Money.cny(5999), Money.cny(11998));

        // Then
        assertThat(item.getProductId()).isEqualTo("PROD001");
        assertThat(item.getQuantity()).isEqualTo(2);
    }
}
