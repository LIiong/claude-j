package com.claudej.application.payment.service;

import com.claudej.application.payment.assembler.PaymentAssembler;
import com.claudej.application.payment.command.CreatePaymentCommand;
import com.claudej.application.payment.command.PaymentCallbackCommand;
import com.claudej.application.payment.command.RefundPaymentCommand;
import com.claudej.application.payment.dto.PaymentDTO;
import com.claudej.domain.common.event.DomainEventPublisher;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.order.model.aggregate.Order;
import com.claudej.domain.order.model.entity.OrderItem;
import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.order.model.valobj.Money;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.domain.order.model.valobj.OrderStatus;
import com.claudej.domain.order.repository.OrderRepository;
import com.claudej.domain.payment.model.aggregate.Payment;
import com.claudej.domain.payment.model.valobj.PaymentId;
import com.claudej.domain.payment.model.valobj.PaymentMethod;
import com.claudej.domain.payment.model.valobj.PaymentStatus;
import com.claudej.domain.payment.repository.PaymentRepository;
import com.claudej.domain.payment.service.PaymentGateway;
import com.claudej.domain.payment.model.valobj.PaymentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentApplicationServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private PaymentAssembler paymentAssembler;

    private PaymentApplicationService paymentApplicationService;

    @BeforeEach
    void setUp() {
        paymentApplicationService = new PaymentApplicationService(
                paymentRepository, orderRepository, domainEventPublisher, paymentGateway, paymentAssembler);
    }

    // --- createPayment tests ---
    @Test
    void should_createPayment_when_validCommandProvided() {
        // Given
        CreatePaymentCommand command = new CreatePaymentCommand();
        command.setOrderId("ORD123456");
        command.setCustomerId("CUST001");
        command.setAmount(BigDecimal.valueOf(100.00));
        command.setMethod("ALIPAY");

        Order mockOrder = createMockOrder("ORD123456", "CUST001", OrderStatus.CREATED);
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));
        when(paymentGateway.createPayment(any(Payment.class))).thenReturn(PaymentResult.success("TXN123"));

        Payment savedPayment = createMockPayment();
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        PaymentDTO expectedDTO = createMockPaymentDTO();
        when(paymentAssembler.toDTO(any(Payment.class))).thenReturn(expectedDTO);

        // When
        PaymentDTO result = paymentApplicationService.createPayment(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPaymentId()).isEqualTo("PAY123456");
        verify(paymentRepository).save(any(Payment.class));
        verify(paymentGateway).createPayment(any(Payment.class));
    }

    @Test
    void should_throwException_when_commandIsNull() {
        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.createPayment(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("创建支付命令不能为空");
    }

    @Test
    void should_throwException_when_orderIdIsNull() {
        // Given
        CreatePaymentCommand command = new CreatePaymentCommand();
        command.setCustomerId("CUST001");
        command.setAmount(BigDecimal.valueOf(100.00));
        command.setMethod("ALIPAY");

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.createPayment(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单ID不能为空");
    }

    @Test
    void should_throwException_when_customerIdIsNull() {
        // Given
        CreatePaymentCommand command = new CreatePaymentCommand();
        command.setOrderId("ORD123456");
        command.setAmount(BigDecimal.valueOf(100.00));
        command.setMethod("ALIPAY");

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.createPayment(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("客户ID不能为空");
    }

    @Test
    void should_throwException_when_amountIsNull() {
        // Given
        CreatePaymentCommand command = new CreatePaymentCommand();
        command.setOrderId("ORD123456");
        command.setCustomerId("CUST001");
        command.setMethod("ALIPAY");

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.createPayment(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付金额无效");
    }

    @Test
    void should_throwException_when_amountIsZero() {
        // Given
        CreatePaymentCommand command = new CreatePaymentCommand();
        command.setOrderId("ORD123456");
        command.setCustomerId("CUST001");
        command.setAmount(BigDecimal.ZERO);
        command.setMethod("ALIPAY");

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.createPayment(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付金额无效");
    }

    @Test
    void should_throwException_when_methodIsNull() {
        // Given
        CreatePaymentCommand command = new CreatePaymentCommand();
        command.setOrderId("ORD123456");
        command.setCustomerId("CUST001");
        command.setAmount(BigDecimal.valueOf(100.00));

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.createPayment(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付方式无效");
    }

    @Test
    void should_throwException_when_methodIsInvalid() {
        // Given
        CreatePaymentCommand command = new CreatePaymentCommand();
        command.setOrderId("ORD123456");
        command.setCustomerId("CUST001");
        command.setAmount(BigDecimal.valueOf(100.00));
        command.setMethod("INVALID");

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.createPayment(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付方式无效");
    }

    @Test
    void should_throwException_when_orderNotFound() {
        // Given
        CreatePaymentCommand command = new CreatePaymentCommand();
        command.setOrderId("ORD_NOT_EXIST");
        command.setCustomerId("CUST001");
        command.setAmount(BigDecimal.valueOf(100.00));
        command.setMethod("ALIPAY");

        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.createPayment(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单不存在");
    }

    @Test
    void should_throwException_when_orderAlreadyPaid() {
        // Given
        CreatePaymentCommand command = new CreatePaymentCommand();
        command.setOrderId("ORD_PAID");
        command.setCustomerId("CUST001");
        command.setAmount(BigDecimal.valueOf(100.00));
        command.setMethod("ALIPAY");

        Order mockOrder = createMockOrder("ORD_PAID", "CUST001", OrderStatus.PAID);
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.createPayment(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单已支付");
    }

    // --- getPaymentById tests ---
    @Test
    void should_returnPayment_when_paymentExists() {
        // Given
        Payment mockPayment = createMockPayment();
        when(paymentRepository.findByPaymentId(any(PaymentId.class))).thenReturn(Optional.of(mockPayment));

        PaymentDTO expectedDTO = createMockPaymentDTO();
        when(paymentAssembler.toDTO(any(Payment.class))).thenReturn(expectedDTO);

        // When
        PaymentDTO result = paymentApplicationService.getPaymentById("PAY123456");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPaymentId()).isEqualTo("PAY123456");
    }

    @Test
    void should_throwException_when_paymentNotFound() {
        // Given
        when(paymentRepository.findByPaymentId(any(PaymentId.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.getPaymentById("PAY_NOT_EXIST"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付不存在");
    }

    // --- getPaymentByOrderId tests ---
    @Test
    void should_returnPayment_when_paymentExistsForOrder() {
        // Given
        Payment mockPayment = createMockPayment();
        when(paymentRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockPayment));

        PaymentDTO expectedDTO = createMockPaymentDTO();
        when(paymentAssembler.toDTO(any(Payment.class))).thenReturn(expectedDTO);

        // When
        PaymentDTO result = paymentApplicationService.getPaymentByOrderId("ORD123456");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo("ORD123456");
    }

    @Test
    void should_throwException_when_paymentNotFoundForOrder() {
        // Given
        when(paymentRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.getPaymentByOrderId("ORD_NO_PAYMENT"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付不存在");
    }

    // --- handleCallback tests ---
    @Test
    void should_handleSuccessCallback_when_paymentIsPending() {
        // Given
        PaymentCallbackCommand command = new PaymentCallbackCommand();
        command.setOrderId("ORD123456");
        command.setTransactionNo("TXN123456");
        command.setSuccess(true);
        command.setMessage("支付成功");

        Payment mockPayment = createMockPendingPayment();
        when(paymentRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        Order mockOrder = createMockOrder("ORD123456", "CUST001", OrderStatus.CREATED);
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        PaymentDTO expectedDTO = createMockPaymentDTO();
        when(paymentAssembler.toDTO(any(Payment.class))).thenReturn(expectedDTO);

        // When
        PaymentDTO result = paymentApplicationService.handleCallback(command);

        // Then
        assertThat(result).isNotNull();
        verify(paymentRepository).save(any(Payment.class));
        verify(orderRepository).save(any(Order.class));
        verify(domainEventPublisher).publish(any());
    }

    @Test
    void should_handleFailedCallback_when_paymentIsPending() {
        // Given
        PaymentCallbackCommand command = new PaymentCallbackCommand();
        command.setOrderId("ORD123456");
        command.setTransactionNo("TXN_FAILED");
        command.setSuccess(false);
        command.setMessage("余额不足");

        Payment mockPayment = createMockPendingPayment();
        when(paymentRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        PaymentDTO expectedDTO = createMockPaymentDTO();
        expectedDTO.setStatus("FAILED");
        when(paymentAssembler.toDTO(any(Payment.class))).thenReturn(expectedDTO);

        // When
        PaymentDTO result = paymentApplicationService.handleCallback(command);

        // Then
        assertThat(result).isNotNull();
        verify(paymentRepository).save(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void should_throwException_when_callbackCommandIsNull() {
        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.handleCallback(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("回调命令不能为空");
    }

    @Test
    void should_throwException_when_transactionNoIsNull() {
        // Given
        PaymentCallbackCommand command = new PaymentCallbackCommand();
        command.setOrderId("ORD123456");
        command.setSuccess(true);
        command.setMessage("支付成功");

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.handleCallback(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("交易号不能为空");
    }

    @Test
    void should_throwException_when_orderIdIsNullInCallback() {
        // Given
        PaymentCallbackCommand command = new PaymentCallbackCommand();
        command.setTransactionNo("TXN123");
        command.setSuccess(true);
        command.setMessage("支付成功");

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.handleCallback(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单ID不能为空");
    }

    @Test
    void should_returnExistingPayment_when_paymentAlreadyProcessed() {
        // Given
        PaymentCallbackCommand command = new PaymentCallbackCommand();
        command.setOrderId("ORD123456");
        command.setTransactionNo("TXN_ALREADY_PROCESSED");
        command.setSuccess(true);
        command.setMessage("支付成功");

        Payment mockPayment = createMockPaymentWithStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockPayment));

        PaymentDTO expectedDTO = createMockPaymentDTO();
        expectedDTO.setStatus("SUCCESS");
        when(paymentAssembler.toDTO(any(Payment.class))).thenReturn(expectedDTO);

        // When
        PaymentDTO result = paymentApplicationService.handleCallback(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    // --- refundPayment tests ---
    @Test
    void should_refundPayment_when_paymentIsSuccess() {
        // Given
        RefundPaymentCommand command = new RefundPaymentCommand();
        command.setReason("用户申请退款");

        Payment mockPayment = createMockPaymentWithStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findByPaymentId(any(PaymentId.class))).thenReturn(Optional.of(mockPayment));
        when(paymentGateway.refundPayment(anyString(), any(Money.class))).thenReturn(PaymentResult.success("REFUND_TXN"));
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        // 订单状态为 PAID，这样退款时会回滚库存
        Order mockOrder = createMockOrder("ORD123456", "CUST001", OrderStatus.PAID);
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        PaymentDTO expectedDTO = createMockPaymentDTO();
        expectedDTO.setStatus("REFUNDED");
        when(paymentAssembler.toDTO(any(Payment.class))).thenReturn(expectedDTO);

        // When
        PaymentDTO result = paymentApplicationService.refundPayment("PAY123456", command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("REFUNDED");
        verify(paymentGateway).refundPayment(anyString(), any(Money.class));
        verify(paymentRepository).save(any(Payment.class));
        verify(orderRepository).save(any(Order.class));
        verify(domainEventPublisher).publish(any());
    }

    @Test
    void should_throwException_when_refundPaymentNotFound() {
        // Given
        RefundPaymentCommand command = new RefundPaymentCommand();
        command.setReason("用户申请退款");

        when(paymentRepository.findByPaymentId(any(PaymentId.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.refundPayment("PAY_NOT_EXIST", command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付不存在");
    }

    @Test
    void should_throwException_when_refundFromPendingStatus() {
        // Given
        RefundPaymentCommand command = new RefundPaymentCommand();
        command.setReason("用户申请退款");

        Payment mockPayment = createMockPaymentWithStatus(PaymentStatus.PENDING);
        when(paymentRepository.findByPaymentId(any(PaymentId.class))).thenReturn(Optional.of(mockPayment));

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.refundPayment("PAY_PENDING", command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态不允许退款");
    }

    @Test
    void should_throwException_when_refundFromFailedStatus() {
        // Given
        RefundPaymentCommand command = new RefundPaymentCommand();
        command.setReason("用户申请退款");

        Payment mockPayment = createMockPaymentWithStatus(PaymentStatus.FAILED);
        when(paymentRepository.findByPaymentId(any(PaymentId.class))).thenReturn(Optional.of(mockPayment));

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.refundPayment("PAY_FAILED", command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态不允许退款");
    }

    @Test
    void should_throwException_when_refundFromRefundedStatus() {
        // Given
        RefundPaymentCommand command = new RefundPaymentCommand();
        command.setReason("用户申请退款");

        Payment mockPayment = createMockPaymentWithStatus(PaymentStatus.REFUNDED);
        when(paymentRepository.findByPaymentId(any(PaymentId.class))).thenReturn(Optional.of(mockPayment));

        // When & Then
        assertThatThrownBy(() -> paymentApplicationService.refundPayment("PAY_REFUNDED", command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付状态不允许退款");
    }

    // --- Helper methods ---
    private Payment createMockPayment() {
        return Payment.reconstruct(
                1L,
                new PaymentId("PAY123456"),
                new OrderId("ORD123456"),
                new CustomerId("CUST001"),
                Money.cny(100.00),
                PaymentStatus.SUCCESS,
                PaymentMethod.ALIPAY,
                "TXN123456",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private Payment createMockPendingPayment() {
        return Payment.reconstruct(
                1L,
                new PaymentId("PAY123456"),
                new OrderId("ORD123456"),
                new CustomerId("CUST001"),
                Money.cny(100.00),
                PaymentStatus.PENDING,
                PaymentMethod.ALIPAY,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private Payment createMockPaymentWithStatus(PaymentStatus status) {
        String transactionNo = status == PaymentStatus.PENDING ? null : "TXN123456";
        return Payment.reconstruct(
                1L,
                new PaymentId("PAY123456"),
                new OrderId("ORD123456"),
                new CustomerId("CUST001"),
                Money.cny(100.00),
                status,
                PaymentMethod.ALIPAY,
                transactionNo,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private PaymentDTO createMockPaymentDTO() {
        PaymentDTO dto = new PaymentDTO();
        dto.setPaymentId("PAY123456");
        dto.setOrderId("ORD123456");
        dto.setCustomerId("CUST001");
        dto.setAmount(BigDecimal.valueOf(100.00));
        dto.setCurrency("CNY");
        dto.setStatus("SUCCESS");
        dto.setMethod("ALIPAY");
        dto.setTransactionNo("TXN123456");
        dto.setCreateTime(LocalDateTime.now());
        dto.setUpdateTime(LocalDateTime.now());
        return dto;
    }

    private Order createMockOrder(String orderId, String customerId, OrderStatus status) {
        OrderItem item = OrderItem.create("PROD001", "iPhone", 1, Money.cny(100));
        return Order.reconstruct(
                1L,
                new OrderId(orderId),
                new CustomerId(customerId),
                status,
                Arrays.asList(item),
                Money.cny(100),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

}