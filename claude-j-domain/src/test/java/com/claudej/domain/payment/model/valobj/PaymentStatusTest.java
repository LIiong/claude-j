package com.claudej.domain.payment.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentStatusTest {

    @Test
    void should_haveFourStatuses_when_enumCreated() {
        // When
        PaymentStatus[] statuses = PaymentStatus.values();

        // Then
        assertThat(statuses).hasSize(4);
        assertThat(statuses).contains(PaymentStatus.PENDING, PaymentStatus.SUCCESS, PaymentStatus.FAILED, PaymentStatus.REFUNDED);
    }

    @Test
    void should_haveDescription_when_pendingStatus() {
        // When
        String description = PaymentStatus.PENDING.getDescription();

        // Then
        assertThat(description).isEqualTo("待支付");
    }

    @Test
    void should_haveDescription_when_successStatus() {
        // When
        String description = PaymentStatus.SUCCESS.getDescription();

        // Then
        assertThat(description).isEqualTo("支付成功");
    }

    @Test
    void should_haveDescription_when_failedStatus() {
        // When
        String description = PaymentStatus.FAILED.getDescription();

        // Then
        assertThat(description).isEqualTo("支付失败");
    }

    @Test
    void should_haveDescription_when_refundedStatus() {
        // When
        String description = PaymentStatus.REFUNDED.getDescription();

        // Then
        assertThat(description).isEqualTo("已退款");
    }

    // --- canSuccess tests ---
    @Test
    void should_canSuccess_when_pendingStatus() {
        // When
        boolean canSuccess = PaymentStatus.PENDING.canSuccess();

        // Then
        assertThat(canSuccess).isTrue();
    }

    @Test
    void should_notCanSuccess_when_successStatus() {
        // When
        boolean canSuccess = PaymentStatus.SUCCESS.canSuccess();

        // Then
        assertThat(canSuccess).isFalse();
    }

    @Test
    void should_notCanSuccess_when_failedStatus() {
        // When
        boolean canSuccess = PaymentStatus.FAILED.canSuccess();

        // Then
        assertThat(canSuccess).isFalse();
    }

    @Test
    void should_notCanSuccess_when_refundedStatus() {
        // When
        boolean canSuccess = PaymentStatus.REFUNDED.canSuccess();

        // Then
        assertThat(canSuccess).isFalse();
    }

    // --- canFail tests ---
    @Test
    void should_canFail_when_pendingStatus() {
        // When
        boolean canFail = PaymentStatus.PENDING.canFail();

        // Then
        assertThat(canFail).isTrue();
    }

    @Test
    void should_notCanFail_when_successStatus() {
        // When
        boolean canFail = PaymentStatus.SUCCESS.canFail();

        // Then
        assertThat(canFail).isFalse();
    }

    @Test
    void should_notCanFail_when_failedStatus() {
        // When
        boolean canFail = PaymentStatus.FAILED.canFail();

        // Then
        assertThat(canFail).isFalse();
    }

    @Test
    void should_notCanFail_when_refundedStatus() {
        // When
        boolean canFail = PaymentStatus.REFUNDED.canFail();

        // Then
        assertThat(canFail).isFalse();
    }

    // --- canRefund tests ---
    @Test
    void should_canRefund_when_successStatus() {
        // When
        boolean canRefund = PaymentStatus.SUCCESS.canRefund();

        // Then
        assertThat(canRefund).isTrue();
    }

    @Test
    void should_notCanRefund_when_pendingStatus() {
        // When
        boolean canRefund = PaymentStatus.PENDING.canRefund();

        // Then
        assertThat(canRefund).isFalse();
    }

    @Test
    void should_notCanRefund_when_failedStatus() {
        // When
        boolean canRefund = PaymentStatus.FAILED.canRefund();

        // Then
        assertThat(canRefund).isFalse();
    }

    @Test
    void should_notCanRefund_when_refundedStatus() {
        // When
        boolean canRefund = PaymentStatus.REFUNDED.canRefund();

        // Then
        assertThat(canRefund).isFalse();
    }

    // --- toSuccess tests ---
    @Test
    void should_toSuccess_when_pendingStatus() {
        // When
        PaymentStatus status = PaymentStatus.PENDING.toSuccess();

        // Then
        assertThat(status).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void should_throwException_when_toSuccessFromSuccessStatus() {
        // When & Then
        assertThatThrownBy(() -> PaymentStatus.SUCCESS.toSuccess())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态 SUCCESS 不允许转为成功");
    }

    @Test
    void should_throwException_when_toSuccessFromFailedStatus() {
        // When & Then
        assertThatThrownBy(() -> PaymentStatus.FAILED.toSuccess())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态 FAILED 不允许转为成功");
    }

    @Test
    void should_throwException_when_toSuccessFromRefundedStatus() {
        // When & Then
        assertThatThrownBy(() -> PaymentStatus.REFUNDED.toSuccess())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态 REFUNDED 不允许转为成功");
    }

    // --- toFailed tests ---
    @Test
    void should_toFailed_when_pendingStatus() {
        // When
        PaymentStatus status = PaymentStatus.PENDING.toFailed();

        // Then
        assertThat(status).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void should_throwException_when_toFailedFromSuccessStatus() {
        // When & Then
        assertThatThrownBy(() -> PaymentStatus.SUCCESS.toFailed())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态 SUCCESS 不允许转为失败");
    }

    @Test
    void should_throwException_when_toFailedFromFailedStatus() {
        // When & Then
        assertThatThrownBy(() -> PaymentStatus.FAILED.toFailed())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态 FAILED 不允许转为失败");
    }

    @Test
    void should_throwException_when_toFailedFromRefundedStatus() {
        // When & Then
        assertThatThrownBy(() -> PaymentStatus.REFUNDED.toFailed())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态 REFUNDED 不允许转为失败");
    }

    // --- toRefunded tests ---
    @Test
    void should_toRefunded_when_successStatus() {
        // When
        PaymentStatus status = PaymentStatus.SUCCESS.toRefunded();

        // Then
        assertThat(status).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void should_throwException_when_toRefundedFromPendingStatus() {
        // When & Then
        assertThatThrownBy(() -> PaymentStatus.PENDING.toRefunded())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态 PENDING 不允许退款");
    }

    @Test
    void should_throwException_when_toRefundedFromFailedStatus() {
        // When & Then
        assertThatThrownBy(() -> PaymentStatus.FAILED.toRefunded())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态 FAILED 不允许退款");
    }

    @Test
    void should_throwException_when_toRefundedFromRefundedStatus() {
        // When & Then
        assertThatThrownBy(() -> PaymentStatus.REFUNDED.toRefunded())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态 REFUNDED 不允许退款");
    }

    // --- isTerminal tests ---
    @Test
    void should_notBeTerminal_when_pendingStatus() {
        // When
        boolean isTerminal = PaymentStatus.PENDING.isTerminal();

        // Then
        assertThat(isTerminal).isFalse();
    }

    @Test
    void should_notBeTerminal_when_successStatus() {
        // When
        boolean isTerminal = PaymentStatus.SUCCESS.isTerminal();

        // Then
        assertThat(isTerminal).isFalse();
    }

    @Test
    void should_beTerminal_when_failedStatus() {
        // When
        boolean isTerminal = PaymentStatus.FAILED.isTerminal();

        // Then
        assertThat(isTerminal).isTrue();
    }

    @Test
    void should_beTerminal_when_refundedStatus() {
        // When
        boolean isTerminal = PaymentStatus.REFUNDED.isTerminal();

        // Then
        assertThat(isTerminal).isTrue();
    }
}