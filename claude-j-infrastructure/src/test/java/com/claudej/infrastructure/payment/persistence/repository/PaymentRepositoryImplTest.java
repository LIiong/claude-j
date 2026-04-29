package com.claudej.infrastructure.payment.persistence.repository;

import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.order.model.valobj.Money;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.domain.payment.model.aggregate.Payment;
import com.claudej.domain.payment.model.valobj.PaymentId;
import com.claudej.domain.payment.model.valobj.PaymentMethod;
import com.claudej.domain.payment.model.valobj.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PaymentRepositoryImplTest {

    @SpringBootApplication(scanBasePackages = {"com.claudej.infrastructure", "com.claudej.application"})
    @MapperScan("com.claudej.infrastructure.**.mapper")
    static class TestConfig {
    }

    @Autowired
    private PaymentRepositoryImpl paymentRepository;

    @Test
    void should_saveAndFindPayment_when_newPaymentCreated() {
        // Given
        OrderId orderId = new OrderId("ORD_TEST_001");
        CustomerId customerId = new CustomerId("CUST_TEST_001");
        Money amount = Money.cny(100.00);
        PaymentMethod method = PaymentMethod.ALIPAY;

        Payment payment = Payment.create(orderId, customerId, amount, method);

        // When
        Payment savedPayment = paymentRepository.save(payment);
        Optional<Payment> foundPayment = paymentRepository.findByPaymentId(savedPayment.getPaymentId());

        // Then
        assertThat(savedPayment.getId()).isNotNull();
        assertThat(foundPayment).isPresent();
        assertThat(foundPayment.get().getOrderIdValue()).isEqualTo("ORD_TEST_001");
        assertThat(foundPayment.get().getCustomerIdValue()).isEqualTo("CUST_TEST_001");
        assertThat(foundPayment.get().getAmount()).isEqualTo(Money.cny(100.00));
        assertThat(foundPayment.get().getMethod()).isEqualTo(PaymentMethod.ALIPAY);
        assertThat(foundPayment.get().getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void should_updatePayment_when_paymentStatusChanged() {
        // Given
        OrderId orderId = new OrderId("ORD_TEST_002");
        CustomerId customerId = new CustomerId("CUST_TEST_002");
        Money amount = Money.cny(200.00);
        PaymentMethod method = PaymentMethod.WECHAT;

        Payment payment = Payment.create(orderId, customerId, amount, method);
        Payment savedPayment = paymentRepository.save(payment);

        // When
        savedPayment.markAsSuccess("TXN_TEST_001");
        paymentRepository.save(savedPayment);

        Optional<Payment> foundPayment = paymentRepository.findByPaymentId(savedPayment.getPaymentId());

        // Then
        assertThat(foundPayment).isPresent();
        assertThat(foundPayment.get().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(foundPayment.get().getTransactionNo()).isEqualTo("TXN_TEST_001");
    }

    @Test
    void should_findByOrderId_when_paymentExists() {
        // Given
        OrderId orderId = new OrderId("ORD_TEST_003");
        CustomerId customerId = new CustomerId("CUST_TEST_003");
        Money amount = Money.cny(300.00);
        PaymentMethod method = PaymentMethod.BANK_CARD;

        Payment payment = Payment.create(orderId, customerId, amount, method);
        paymentRepository.save(payment);

        // When
        Optional<Payment> foundPayment = paymentRepository.findByOrderId(orderId);

        // Then
        assertThat(foundPayment).isPresent();
        assertThat(foundPayment.get().getOrderIdValue()).isEqualTo("ORD_TEST_003");
    }

    @Test
    void should_returnEmpty_when_paymentNotFound() {
        // Given
        PaymentId paymentId = new PaymentId("PAY_NOT_EXIST");

        // When
        Optional<Payment> foundPayment = paymentRepository.findByPaymentId(paymentId);

        // Then
        assertThat(foundPayment).isNotPresent();
    }

    @Test
    void should_returnEmpty_when_orderNotFound() {
        // Given
        OrderId orderId = new OrderId("ORD_NOT_EXIST");

        // When
        Optional<Payment> foundPayment = paymentRepository.findByOrderId(orderId);

        // Then
        assertThat(foundPayment).isNotPresent();
    }

    @Test
    void should_existsByPaymentId_when_paymentExists() {
        // Given
        OrderId orderId = new OrderId("ORD_TEST_004");
        CustomerId customerId = new CustomerId("CUST_TEST_004");
        Money amount = Money.cny(400.00);
        PaymentMethod method = PaymentMethod.ALIPAY;

        Payment payment = Payment.create(orderId, customerId, amount, method);
        Payment savedPayment = paymentRepository.save(payment);

        // When
        boolean exists = paymentRepository.existsByPaymentId(savedPayment.getPaymentId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void should_notExistsByPaymentId_when_paymentNotFound() {
        // Given
        PaymentId paymentId = new PaymentId("PAY_NOT_EXIST");

        // When
        boolean exists = paymentRepository.existsByPaymentId(paymentId);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void should_findByCustomerId_when_paymentsExist() {
        // Given
        CustomerId customerId = new CustomerId("CUST_TEST_MULTI");

        Payment payment1 = Payment.create(new OrderId("ORD_MULTI_001"), customerId, Money.cny(100), PaymentMethod.ALIPAY);
        Payment payment2 = Payment.create(new OrderId("ORD_MULTI_002"), customerId, Money.cny(200), PaymentMethod.WECHAT);

        paymentRepository.save(payment1);
        paymentRepository.save(payment2);

        // When
        List<Payment> payments = paymentRepository.findByCustomerId(customerId);

        // Then
        assertThat(payments).hasSizeGreaterThanOrEqualTo(2);
        assertThat(payments.stream().allMatch(p -> p.getCustomerIdValue().equals("CUST_TEST_MULTI"))).isTrue();
    }
}