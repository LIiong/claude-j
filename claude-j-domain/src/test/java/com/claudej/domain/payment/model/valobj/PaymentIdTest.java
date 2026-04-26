package com.claudej.domain.payment.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentIdTest {

    @Test
    void should_createPaymentId_when_validValueProvided() {
        // When
        PaymentId paymentId = new PaymentId("PAY123456");

        // Then
        assertThat(paymentId.getValue()).isEqualTo("PAY123456");
    }

    @Test
    void should_trimWhitespace_when_valueHasLeadingOrTrailingSpaces() {
        // When
        PaymentId paymentId = new PaymentId("  PAY123456  ");

        // Then
        assertThat(paymentId.getValue()).isEqualTo("PAY123456");
    }

    @Test
    void should_throwException_when_valueIsNull() {
        // When & Then
        assertThatThrownBy(() -> new PaymentId(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付ID不能为空");
    }

    @Test
    void should_throwException_when_valueIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> new PaymentId(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付ID不能为空");
    }

    @Test
    void should_throwException_when_valueIsBlank() {
        // When & Then
        assertThatThrownBy(() -> new PaymentId("   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付ID不能为空");
    }

    @Test
    void should_beEqual_when_sameValue() {
        // Given
        PaymentId id1 = new PaymentId("PAY123");
        PaymentId id2 = new PaymentId("PAY123");

        // Then
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    void should_notBeEqual_when_differentValue() {
        // Given
        PaymentId id1 = new PaymentId("PAY123");
        PaymentId id2 = new PaymentId("PAY456");

        // Then
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void should_haveToString_when_validValue() {
        // Given
        PaymentId paymentId = new PaymentId("PAY123456");

        // When & Then
        assertThat(paymentId.toString()).contains("PAY123456");
    }
}