package com.claudej.domain.cart.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantityTest {

    @Test
    void should_createQuantity_when_valueInRange() {
        // Arrange & Act
        Quantity quantity = new Quantity(5);

        // Assert
        assertThat(quantity.getValue()).isEqualTo(5);
    }

    @Test
    void should_throwException_when_valueLessThanMin() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> new Quantity(0))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("不能小于");
    }

    @Test
    void should_throwException_when_valueGreaterThanMax() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> new Quantity(1000))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("不能超过");
    }

    @Test
    void should_addQuantities_when_addCalled() {
        // Arrange
        Quantity q1 = new Quantity(3);
        Quantity q2 = new Quantity(5);

        // Act
        Quantity result = q1.add(q2);

        // Assert
        assertThat(result.getValue()).isEqualTo(8);
    }

    @Test
    void should_updateQuantity_when_updateCalled() {
        // Arrange
        Quantity quantity = new Quantity(5);

        // Act
        Quantity result = quantity.update(10);

        // Assert
        assertThat(result.getValue()).isEqualTo(10);
    }

    @Test
    void should_beEqual_when_sameValue() {
        // Arrange
        Quantity q1 = new Quantity(5);
        Quantity q2 = new Quantity(5);

        // Assert
        assertThat(q1).isEqualTo(q2);
    }

    @Test
    void should_createAtMinBoundary_when_valueIsOne() {
        // Arrange & Act
        Quantity quantity = new Quantity(1);

        // Assert
        assertThat(quantity.getValue()).isEqualTo(1);
    }

    @Test
    void should_createAtMaxBoundary_when_valueIs999() {
        // Arrange & Act
        Quantity quantity = new Quantity(999);

        // Assert
        assertThat(quantity.getValue()).isEqualTo(999);
    }
}
