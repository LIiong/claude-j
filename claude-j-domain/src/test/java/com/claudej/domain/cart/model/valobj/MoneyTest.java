package com.claudej.domain.cart.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void should_createMoney_when_validAmountAndCurrency() {
        // Arrange & Act
        Money money = Money.cny(99.99);

        // Assert
        assertThat(money.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
        assertThat(money.getCurrency()).isEqualTo("CNY");
    }

    @Test
    void should_throwException_when_amountIsNull() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> new Money(null, "CNY"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("不能为空");
    }

    @Test
    void should_throwException_when_amountIsNegative() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> Money.cny(-1.00))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("不能为负数");
    }

    @Test
    void should_throwException_when_currencyIsEmpty() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> new Money(BigDecimal.TEN, ""))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("不能为空");
    }

    @Test
    void should_addMoney_when_sameCurrency() {
        // Arrange
        Money m1 = Money.cny(50.00);
        Money m2 = Money.cny(30.00);

        // Act
        Money result = m1.add(m2);

        // Assert
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(80.00));
    }

    @Test
    void should_throwException_when_differentCurrency() {
        // Arrange
        Money cny = Money.cny(50.00);
        Money usd = new Money(BigDecimal.valueOf(30.00), "USD");

        // Act & Assert
        assertThatThrownBy(() -> cny.add(usd))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("币种不一致");
    }

    @Test
    void should_multiply_when_validMultiplier() {
        // Arrange
        Money money = Money.cny(10.00);

        // Act
        Money result = money.multiply(3);

        // Assert
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(30.00));
    }

    @Test
    void should_roundToTwoDecimalPlaces_when_moreDecimals() {
        // Arrange & Act
        Money money = new Money(BigDecimal.valueOf(99.999), "CNY");

        // Assert
        assertThat(money.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    @Test
    void should_beEqual_when_sameAmountAndCurrency() {
        // Arrange
        Money m1 = Money.cny(50.00);
        Money m2 = Money.cny(50.00);

        // Assert
        assertThat(m1).isEqualTo(m2);
    }

    @Test
    void should_returnTrue_when_isZero() {
        // Arrange
        Money money = Money.cny(0);

        // Assert
        assertThat(money.isZero()).isTrue();
    }

    @Test
    void should_returnFalse_when_isNotZero() {
        // Arrange
        Money money = Money.cny(10.00);

        // Assert
        assertThat(money.isZero()).isFalse();
    }

    @Test
    void should_normalizeCurrencyToUpperCase() {
        // Arrange & Act
        Money money = new Money(BigDecimal.TEN, "cny");

        // Assert
        assertThat(money.getCurrency()).isEqualTo("CNY");
    }
}
