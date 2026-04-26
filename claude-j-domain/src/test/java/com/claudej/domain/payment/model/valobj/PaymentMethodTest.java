package com.claudej.domain.payment.model.valobj;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMethodTest {

    @Test
    void should_haveThreeMethods_when_enumCreated() {
        // When
        PaymentMethod[] methods = PaymentMethod.values();

        // Then
        assertThat(methods).hasSize(3);
        assertThat(methods).contains(PaymentMethod.ALIPAY, PaymentMethod.WECHAT, PaymentMethod.BANK_CARD);
    }

    @Test
    void should_haveDescription_when_alipayMethod() {
        // When
        String description = PaymentMethod.ALIPAY.getDescription();

        // Then
        assertThat(description).isEqualTo("支付宝");
    }

    @Test
    void should_haveDescription_when_wechatMethod() {
        // When
        String description = PaymentMethod.WECHAT.getDescription();

        // Then
        assertThat(description).isEqualTo("微信支付");
    }

    @Test
    void should_haveDescription_when_bankCardMethod() {
        // When
        String description = PaymentMethod.BANK_CARD.getDescription();

        // Then
        assertThat(description).isEqualTo("银行卡");
    }

    @Test
    void should_getMethodByName_when_validNameProvided() {
        // When
        PaymentMethod method = PaymentMethod.valueOf("ALIPAY");

        // Then
        assertThat(method).isEqualTo(PaymentMethod.ALIPAY);
    }

    @Test
    void should_throwException_when_invalidNameProvided() {
        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> PaymentMethod.valueOf("INVALID")
        );
    }
}