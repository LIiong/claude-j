package com.claudej.domain.payment.model.aggregate;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.order.model.valobj.Money;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.domain.payment.model.valobj.PaymentId;
import com.claudej.domain.payment.model.valobj.PaymentMethod;
import com.claudej.domain.payment.model.valobj.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    // --- create tests ---
    @Test
    void should_createPayment_when_validParametersProvided() {
        // Given
        OrderId orderId = new OrderId("ORD123456");
        CustomerId customerId = new CustomerId("CUST001");
        Money amount = Money.cny(100.00);
        PaymentMethod method = PaymentMethod.ALIPAY;

        // When
        Payment payment = Payment.create(orderId, customerId, amount, method);

        // Then
        assertThat(payment.getPaymentId()).isNotNull();
        assertThat(payment.getPaymentId().getValue()).isNotEmpty();
        assertThat(payment.getOrderId()).isEqualTo(orderId);
        assertThat(payment.getCustomerId()).isEqualTo(customerId);
        assertThat(payment.getAmount()).isEqualTo(amount);
        assertThat(payment.getMethod()).isEqualTo(method);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getTransactionNo()).isNull();
        assertThat(payment.getCreateTime()).isNotNull();
        assertThat(payment.getUpdateTime()).isNotNull();
    }

    @Test
    void should_throwException_when_orderIdIsNull() {
        // Given
        CustomerId customerId = new CustomerId("CUST001");
        Money amount = Money.cny(100.00);
        PaymentMethod method = PaymentMethod.ALIPAY;

        // When & Then
        assertThatThrownBy(() -> Payment.create(null, customerId, amount, method))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单ID不能为空");
    }

    @Test
    void should_throwException_when_customerIdIsNull() {
        // Given
        OrderId orderId = new OrderId("ORD123456");
        Money amount = Money.cny(100.00);
        PaymentMethod method = PaymentMethod.ALIPAY;

        // When & Then
        assertThatThrownBy(() -> Payment.create(orderId, null, amount, method))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("客户ID不能为空");
    }

    @Test
    void should_throwException_when_amountIsNull() {
        // Given
        OrderId orderId = new OrderId("ORD123456");
        CustomerId customerId = new CustomerId("CUST001");
        PaymentMethod method = PaymentMethod.ALIPAY;

        // When & Then
        assertThatThrownBy(() -> Payment.create(orderId, customerId, null, method))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付金额无效");
    }

    @Test
    void should_throwException_when_amountIsZero() {
        // Given
        OrderId orderId = new OrderId("ORD123456");
        CustomerId customerId = new CustomerId("CUST001");
        Money amount = Money.cny(0);
        PaymentMethod method = PaymentMethod.ALIPAY;

        // When & Then
        assertThatThrownBy(() -> Payment.create(orderId, customerId, amount, method))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付金额无效");
    }

    @Test
    void should_throwException_when_methodIsNull() {
        // Given
        OrderId orderId = new OrderId("ORD123456");
        CustomerId customerId = new CustomerId("CUST001");
        Money amount = Money.cny(100.00);

        // When & Then
        assertThatThrownBy(() -> Payment.create(orderId, customerId, amount, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付方式无效");
    }

    // --- markAsSuccess tests ---
    @Test
    void should_markAsSuccess_when_pendingStatus() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);

        // When
        payment.markAsSuccess("TXN123456");

        // Then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getTransactionNo()).isEqualTo("TXN123456");
        assertThat(payment.getUpdateTime()).isNotNull();
    }

    @Test
    void should_throwException_when_markAsSuccessFromSuccessStatus() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);
        payment.markAsSuccess("TXN123");

        // When & Then
        assertThatThrownBy(() -> payment.markAsSuccess("TXN456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态 SUCCESS 不允许转为成功");
    }

    @Test
    void should_throwException_when_markAsSuccessFromFailedStatus() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);
        payment.markAsFailed("支付超时");

        // When & Then
        assertThatThrownBy(() -> payment.markAsSuccess("TXN456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态 FAILED 不允许转为成功");
    }

    @Test
    void should_throwException_when_markAsSuccessFromRefundedStatus() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);
        payment.markAsSuccess("TXN123");
        payment.refund();

        // When & Then
        assertThatThrownBy(() -> payment.markAsSuccess("TXN456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态 REFUNDED 不允许转为成功");
    }

    @Test
    void should_throwException_when_markAsSuccessWithNullTransactionNo() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);

        // When & Then
        assertThatThrownBy(() -> payment.markAsSuccess(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("交易号不能为空");
    }

    @Test
    void should_throwException_when_markAsSuccessWithEmptyTransactionNo() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);

        // When & Then
        assertThatThrownBy(() -> payment.markAsSuccess(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("交易号不能为空");
    }

    // --- markAsFailed tests ---
    @Test
    void should_markAsFailed_when_pendingStatus() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);

        // When
        payment.markAsFailed("余额不足");

        // Then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getTransactionNo()).isNull();
        assertThat(payment.getUpdateTime()).isNotNull();
    }

    @Test
    void should_throwException_when_markAsFailedFromSuccessStatus() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);
        payment.markAsSuccess("TXN123");

        // When & Then
        assertThatThrownBy(() -> payment.markAsFailed("失败"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态 SUCCESS 不允许转为失败");
    }

    @Test
    void should_throwException_when_markAsFailedFromFailedStatus() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);
        payment.markAsFailed("超时");

        // When & Then
        assertThatThrownBy(() -> payment.markAsFailed("再次失败"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态 FAILED 不允许转为失败");
    }

    @Test
    void should_throwException_when_markAsFailedFromRefundedStatus() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);
        payment.markAsSuccess("TXN123");
        payment.refund();

        // When & Then
        assertThatThrownBy(() -> payment.markAsFailed("失败"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态 REFUNDED 不允许转为失败");
    }

    @Test
    void should_throwException_when_markAsFailedWithNullMessage() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);

        // When & Then
        assertThatThrownBy(() -> payment.markAsFailed(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("失败消息不能为空");
    }

    @Test
    void should_throwException_when_markAsFailedWithEmptyMessage() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);

        // When & Then
        assertThatThrownBy(() -> payment.markAsFailed(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("失败消息不能为空");
    }

    // --- refund tests ---
    @Test
    void should_refund_when_successStatus() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);
        payment.markAsSuccess("TXN123");

        // When
        payment.refund();

        // Then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getUpdateTime()).isNotNull();
    }

    @Test
    void should_throwException_when_refundFromPendingStatus() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);

        // When & Then
        assertThatThrownBy(() -> payment.refund())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态 PENDING 不允许退款");
    }

    @Test
    void should_throwException_when_refundFromFailedStatus() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);
        payment.markAsFailed("超时");

        // When & Then
        assertThatThrownBy(() -> payment.refund())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态 FAILED 不允许退款");
    }

    @Test
    void should_throwException_when_refundFromRefundedStatus() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);
        payment.markAsSuccess("TXN123");
        payment.refund();

        // When & Then
        assertThatThrownBy(() -> payment.refund())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态 REFUNDED 不允许退款");
    }

    // --- reconstruct tests ---
    @Test
    void should_reconstructPayment_when_allParametersProvided() {
        // Given
        Long id = 1L;
        PaymentId paymentId = new PaymentId("PAY123");
        OrderId orderId = new OrderId("ORD123");
        CustomerId customerId = new CustomerId("CUST001");
        Money amount = Money.cny(100);
        PaymentStatus status = PaymentStatus.PENDING;
        PaymentMethod method = PaymentMethod.ALIPAY;
        String transactionNo = null;
        LocalDateTime createTime = LocalDateTime.now();
        LocalDateTime updateTime = LocalDateTime.now();

        // When
        Payment payment = Payment.reconstruct(id, paymentId, orderId, customerId, amount, status, method, transactionNo, createTime, updateTime);

        // Then
        assertThat(payment.getId()).isEqualTo(id);
        assertThat(payment.getPaymentId()).isEqualTo(paymentId);
        assertThat(payment.getOrderId()).isEqualTo(orderId);
        assertThat(payment.getCustomerId()).isEqualTo(customerId);
        assertThat(payment.getAmount()).isEqualTo(amount);
        assertThat(payment.getStatus()).isEqualTo(status);
        assertThat(payment.getMethod()).isEqualTo(method);
        assertThat(payment.getTransactionNo()).isNull();
        assertThat(payment.getCreateTime()).isEqualTo(createTime);
        assertThat(payment.getUpdateTime()).isEqualTo(updateTime);
    }

    @Test
    void should_reconstructPayment_withTransactionNo_when_successStatus() {
        // Given
        Long id = 1L;
        PaymentId paymentId = new PaymentId("PAY123");
        OrderId orderId = new OrderId("ORD123");
        CustomerId customerId = new CustomerId("CUST001");
        Money amount = Money.cny(100);
        PaymentStatus status = PaymentStatus.SUCCESS;
        PaymentMethod method = PaymentMethod.ALIPAY;
        String transactionNo = "TXN123456";
        LocalDateTime createTime = LocalDateTime.now();
        LocalDateTime updateTime = LocalDateTime.now();

        // When
        Payment payment = Payment.reconstruct(id, paymentId, orderId, customerId, amount, status, method, transactionNo, createTime, updateTime);

        // Then
        assertThat(payment.getTransactionNo()).isEqualTo("TXN123456");
    }

    // --- setId tests ---
    @Test
    void should_setId_when_called() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);

        // When
        payment.setId(123L);

        // Then
        assertThat(payment.getId()).isEqualTo(123L);
    }

    // --- convenience methods tests ---
    @Test
    void should_returnOrderIdValue_when_getOrderIdValueCalled() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);

        // When & Then
        assertThat(payment.getOrderIdValue()).isEqualTo("ORD123");
    }

    @Test
    void should_returnCustomerIdValue_when_getCustomerIdValueCalled() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);

        // When & Then
        assertThat(payment.getCustomerIdValue()).isEqualTo("CUST001");
    }

    @Test
    void should_returnPaymentIdValue_when_getPaymentIdValueCalled() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);

        // When & Then
        assertThat(payment.getPaymentIdValue()).isNotEmpty();
    }

    // --- isTerminal tests ---
    @Test
    void should_notBeTerminal_when_pendingStatus() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);

        // When & Then
        assertThat(payment.isTerminal()).isFalse();
    }

    @Test
    void should_notBeTerminal_when_successStatus() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);
        payment.markAsSuccess("TXN123");

        // When & Then
        assertThat(payment.isTerminal()).isFalse();
    }

    @Test
    void should_beTerminal_when_failedStatus() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);
        payment.markAsFailed("超时");

        // When & Then
        assertThat(payment.isTerminal()).isTrue();
    }

    @Test
    void should_beTerminal_when_refundedStatus() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);
        payment.markAsSuccess("TXN123");
        payment.refund();

        // When & Then
        assertThat(payment.isTerminal()).isTrue();
    }

    // --- update time tests ---
    @Test
    void should_updateUpdateTime_when_markAsSuccess() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);
        LocalDateTime beforeUpdate = payment.getUpdateTime();

        // When
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // ignore
        }
        payment.markAsSuccess("TXN123");

        // Then
        assertThat(payment.getUpdateTime()).isAfter(beforeUpdate);
    }

    @Test
    void should_updateUpdateTime_when_markAsFailed() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);
        LocalDateTime beforeUpdate = payment.getUpdateTime();

        // When
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // ignore
        }
        payment.markAsFailed("超时");

        // Then
        assertThat(payment.getUpdateTime()).isAfter(beforeUpdate);
    }

    @Test
    void should_updateUpdateTime_when_refund() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);
        payment.markAsSuccess("TXN123");
        LocalDateTime beforeUpdate = payment.getUpdateTime();

        // When
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // ignore
        }
        payment.refund();

        // Then
        assertThat(payment.getUpdateTime()).isAfter(beforeUpdate);
    }

    // --- different payment methods tests ---
    @Test
    void should_createPayment_withWechatMethod() {
        // Given
        OrderId orderId = new OrderId("ORD123");
        CustomerId customerId = new CustomerId("CUST001");
        Money amount = Money.cny(100);
        PaymentMethod method = PaymentMethod.WECHAT;

        // When
        Payment payment = Payment.create(orderId, customerId, amount, method);

        // Then
        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.WECHAT);
    }

    @Test
    void should_createPayment_withBankCardMethod() {
        // Given
        OrderId orderId = new OrderId("ORD123");
        CustomerId customerId = new CustomerId("CUST001");
        Money amount = Money.cny(100);
        PaymentMethod method = PaymentMethod.BANK_CARD;

        // When
        Payment payment = Payment.create(orderId, customerId, amount, method);

        // Then
        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.BANK_CARD);
    }
}