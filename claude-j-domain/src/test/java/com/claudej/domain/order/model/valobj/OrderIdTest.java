package com.claudej.domain.order.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderIdTest {

    @Test
    void should_createOrderId_when_validValueProvided() {
        // When
        OrderId orderId = new OrderId("ORD123456");

        // Then
        assertThat(orderId.getValue()).isEqualTo("ORD123456");
    }

    @Test
    void should_trimWhitespace_when_valueHasLeadingOrTrailingSpaces() {
        // When
        OrderId orderId = new OrderId("  ORD123456  ");

        // Then
        assertThat(orderId.getValue()).isEqualTo("ORD123456");
    }

    @Test
    void should_throwException_when_valueIsNull() {
        // When & Then
        assertThatThrownBy(() -> new OrderId(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单号不能为空");
    }

    @Test
    void should_throwException_when_valueIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> new OrderId(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单号不能为空");
    }

    @Test
    void should_beEqual_when_sameValue() {
        // Given
        OrderId id1 = new OrderId("ORD123");
        OrderId id2 = new OrderId("ORD123");

        // Then
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    void should_notBeEqual_when_differentValue() {
        // Given
        OrderId id1 = new OrderId("ORD123");
        OrderId id2 = new OrderId("ORD456");

        // Then
        assertThat(id1).isNotEqualTo(id2);
    }
}
