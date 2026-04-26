package com.claudej.infrastructure.payment.gateway;

import com.claudej.domain.order.model.valobj.Money;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.payment.model.aggregate.Payment;
import com.claudej.domain.payment.model.valobj.PaymentMethod;
import com.claudej.domain.payment.model.valobj.PaymentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockPaymentGatewayTest {

    private MockPaymentGateway mockPaymentGateway;

    @BeforeEach
    void setUp() {
        mockPaymentGateway = new MockPaymentGateway();
        mockPaymentGateway.reset();
    }

    @Test
    void should_returnSuccess_when_createPaymentWithDefaultConfig() {
        // Given
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);

        // When
        PaymentResult result = mockPaymentGateway.createPayment(payment);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransactionNo()).startsWith("MOCK_TXN_");
        assertThat(result.getMessage()).isEqualTo("支付成功");
    }

    @Test
    void should_returnFailed_when_createPaymentWithFailedConfig() {
        // Given
        mockPaymentGateway.setSimulateSuccess(false, "余额不足");
        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);

        // When
        PaymentResult result = mockPaymentGateway.createPayment(payment);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getTransactionNo()).isNull();
        assertThat(result.getMessage()).isEqualTo("余额不足");
    }

    @Test
    void should_returnSuccess_when_queryPaymentWithDefaultConfig() {
        // Given
        String transactionNo = "TXN123456";

        // When
        PaymentResult result = mockPaymentGateway.queryPayment(transactionNo);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransactionNo()).isEqualTo("TXN123456");
    }

    @Test
    void should_returnFailed_when_queryPaymentWithFailedConfig() {
        // Given
        mockPaymentGateway.setSimulateSuccess(false, "交易不存在");
        String transactionNo = "TXN_NOT_EXIST";

        // When
        PaymentResult result = mockPaymentGateway.queryPayment(transactionNo);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("交易不存在");
    }

    @Test
    void should_returnSuccess_when_refundPaymentWithDefaultConfig() {
        // Given
        String transactionNo = "TXN123456";
        Money amount = Money.cny(100);

        // When
        PaymentResult result = mockPaymentGateway.refundPayment(transactionNo, amount);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransactionNo()).startsWith("MOCK_REFUND_");
    }

    @Test
    void should_returnFailed_when_refundPaymentWithFailedConfig() {
        // Given
        mockPaymentGateway.setSimulateSuccess(false, "退款已关闭");
        String transactionNo = "TXN123456";
        Money amount = Money.cny(100);

        // When
        PaymentResult result = mockPaymentGateway.refundPayment(transactionNo, amount);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("退款失败");
    }

    @Test
    void should_resetToDefault_when_resetCalled() {
        // Given
        mockPaymentGateway.setSimulateSuccess(false, "测试失败");
        mockPaymentGateway.reset();

        Payment payment = Payment.create(
                new OrderId("ORD123"), new CustomerId("CUST001"), Money.cny(100), PaymentMethod.ALIPAY);

        // When
        PaymentResult result = mockPaymentGateway.createPayment(payment);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("支付成功");
    }
}