package com.claudej.domain.payment.model.valobj;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentResultTest {

    @Test
    void should_createSuccessResult_when_factoryMethodCalled() {
        // When
        PaymentResult result = PaymentResult.success("TXN123456");

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransactionNo()).isEqualTo("TXN123456");
        assertThat(result.getMessage()).isEqualTo("支付成功");
    }

    @Test
    void should_createFailedResult_when_factoryMethodCalled() {
        // When
        PaymentResult result = PaymentResult.failed("余额不足");

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getTransactionNo()).isNull();
        assertThat(result.getMessage()).isEqualTo("余额不足");
    }

    @Test
    void should_throwException_when_successWithNullTransactionNo() {
        // When & Then
        assertThatThrownBy(() -> PaymentResult.success(null))
                .isInstanceOf(com.claudej.domain.common.exception.BusinessException.class)
                .hasMessageContaining("交易号不能为空");
    }

    @Test
    void should_throwException_when_successWithEmptyTransactionNo() {
        // When & Then
        assertThatThrownBy(() -> PaymentResult.success(""))
                .isInstanceOf(com.claudej.domain.common.exception.BusinessException.class)
                .hasMessageContaining("交易号不能为空");
    }

    @Test
    void should_throwException_when_failedWithNullMessage() {
        // When & Then
        assertThatThrownBy(() -> PaymentResult.failed(null))
                .isInstanceOf(com.claudej.domain.common.exception.BusinessException.class)
                .hasMessageContaining("失败消息不能为空");
    }

    @Test
    void should_throwException_when_failedWithEmptyMessage() {
        // When & Then
        assertThatThrownBy(() -> PaymentResult.failed(""))
                .isInstanceOf(com.claudej.domain.common.exception.BusinessException.class)
                .hasMessageContaining("失败消息不能为空");
    }

    @Test
    void should_beEqual_when_sameSuccessResult() {
        // Given
        PaymentResult result1 = PaymentResult.success("TXN123");
        PaymentResult result2 = PaymentResult.success("TXN123");

        // Then
        assertThat(result1).isEqualTo(result2);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    void should_beEqual_when_sameFailedResult() {
        // Given
        PaymentResult result1 = PaymentResult.failed("余额不足");
        PaymentResult result2 = PaymentResult.failed("余额不足");

        // Then
        assertThat(result1).isEqualTo(result2);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    void should_notBeEqual_when_differentTransactionNo() {
        // Given
        PaymentResult result1 = PaymentResult.success("TXN123");
        PaymentResult result2 = PaymentResult.success("TXN456");

        // Then
        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void should_notBeEqual_when_differentMessage() {
        // Given
        PaymentResult result1 = PaymentResult.failed("余额不足");
        PaymentResult result2 = PaymentResult.failed("超时");

        // Then
        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void should_notBeEqual_when_successVsFailed() {
        // Given
        PaymentResult successResult = PaymentResult.success("TXN123");
        PaymentResult failedResult = PaymentResult.failed("失败");

        // Then
        assertThat(successResult).isNotEqualTo(failedResult);
    }

    @Test
    void should_haveToString_when_successResult() {
        // Given
        PaymentResult result = PaymentResult.success("TXN123456");

        // When & Then
        assertThat(result.toString()).contains("TXN123456");
        assertThat(result.toString()).contains("true");
    }

    @Test
    void should_haveToString_when_failedResult() {
        // Given
        PaymentResult result = PaymentResult.failed("余额不足");

        // When & Then
        assertThat(result.toString()).contains("余额不足");
        assertThat(result.toString()).contains("false");
    }
}