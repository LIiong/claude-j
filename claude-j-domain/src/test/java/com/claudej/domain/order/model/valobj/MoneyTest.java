package com.claudej.domain.order.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void should_createMoney_when_validAmountAndCurrencyProvided() {
        // When
        Money money = new Money(new BigDecimal("100.50"), "CNY");

        // Then
        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(money.getCurrency()).isEqualTo("CNY");
    }

    @Test
    void should_normalizeCurrencyToUpperCase() {
        // When
        Money money = new Money(new BigDecimal("100"), "cny");

        // Then
        assertThat(money.getCurrency()).isEqualTo("CNY");
    }

    @Test
    void should_scaleToTwoDecimalPlaces() {
        // When
        Money money = new Money(new BigDecimal("100.555"), "CNY");

        // Then
        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.56"));
    }

    @Test
    void should_createCnyConveniently() {
        // When
        Money money = Money.cny(100.50);

        // Then
        assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(money.getCurrency()).isEqualTo("CNY");
    }

    @Test
    void should_throwException_when_amountIsNull() {
        // When & Then
        assertThatThrownBy(() -> new Money(null, "CNY"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("金额不能为空");
    }

    @Test
    void should_throwException_when_amountIsNegative() {
        // When & Then
        assertThatThrownBy(() -> new Money(new BigDecimal("-100"), "CNY"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("金额不能为负数");
    }

    @Test
    void should_throwException_when_currencyIsNull() {
        // When & Then
        assertThatThrownBy(() -> new Money(new BigDecimal("100"), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("币种不能为空");
    }

    @Test
    void should_throwException_when_currencyIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> new Money(new BigDecimal("100"), ""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("币种不能为空");
    }

    @Test
    void should_addMoney_when_sameCurrency() {
        // Given
        Money money1 = Money.cny(100);
        Money money2 = Money.cny(50);

        // When
        Money result = money1.add(money2);

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("150"));
        assertThat(result.getCurrency()).isEqualTo("CNY");
    }

    @Test
    void should_multiplyMoney() {
        // Given
        Money money = Money.cny(100);

        // When
        Money result = money.multiply(3);

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("300"));
    }

    @Test
    void should_returnZero_when_createdWithZero() {
        // When
        Money money = Money.cny(0);

        // Then
        assertThat(money.isZero()).isTrue();
    }

    @Test
    void should_beGreaterThan_when_amountIsLarger() {
        // Given
        Money money1 = Money.cny(100);
        Money money2 = Money.cny(50);

        // Then
        assertThat(money1.isGreaterThan(money2)).isTrue();
        assertThat(money2.isGreaterThan(money1)).isFalse();
    }

    @Test
    void should_beEqual_when_sameAmountAndCurrency() {
        // Given
        Money money1 = new Money(new BigDecimal("100.00"), "CNY");
        Money money2 = new Money(new BigDecimal("100.00"), "CNY");

        // Then
        assertThat(money1).isEqualTo(money2);
        assertThat(money1.hashCode()).isEqualTo(money2.hashCode());
    }

    @Test
    void should_notBeEqual_when_differentAmount() {
        // Given
        Money money1 = Money.cny(100);
        Money money2 = Money.cny(200);

        // Then
        assertThat(money1).isNotEqualTo(money2);
    }
}
