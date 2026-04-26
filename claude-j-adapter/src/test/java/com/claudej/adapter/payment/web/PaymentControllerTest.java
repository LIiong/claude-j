package com.claudej.adapter.payment.web;

import com.claudej.adapter.common.GlobalExceptionHandler;
import com.claudej.application.payment.command.CreatePaymentCommand;
import com.claudej.application.payment.command.PaymentCallbackCommand;
import com.claudej.application.payment.command.RefundPaymentCommand;
import com.claudej.application.payment.dto.PaymentDTO;
import com.claudej.application.payment.service.PaymentApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {PaymentController.class, GlobalExceptionHandler.class})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentApplicationService paymentApplicationService;

    private PaymentDTO mockPaymentDTO;

    @BeforeEach
    void setUp() {
        mockPaymentDTO = createMockPaymentDTO();
    }

    @Test
    void should_createPayment_when_validRequestProvided() throws Exception {
        // Given
        when(paymentApplicationService.createPayment(any(CreatePaymentCommand.class))).thenReturn(mockPaymentDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"ORD123\",\"customerId\":\"CUST001\",\"amount\":100.00,\"method\":\"ALIPAY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentId").value("PAY123456"));
    }

    @Test
    void should_returnBadRequest_when_orderIdIsNull() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"CUST001\",\"amount\":100.00,\"method\":\"ALIPAY\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_returnBadRequest_when_amountIsNegative() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"ORD123\",\"customerId\":\"CUST001\",\"amount\":-100.00,\"method\":\"ALIPAY\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_getPaymentById_when_paymentExists() throws Exception {
        // Given
        when(paymentApplicationService.getPaymentById("PAY123456")).thenReturn(mockPaymentDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/payments/PAY123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentId").value("PAY123456"));
    }

    @Test
    void should_getPaymentByOrderId_when_paymentExists() throws Exception {
        // Given
        when(paymentApplicationService.getPaymentByOrderId("ORD123456")).thenReturn(mockPaymentDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/payments/orders/ORD123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value("ORD123456"));
    }

    @Test
    void should_handleCallback_when_validRequestProvided() throws Exception {
        // Given
        PaymentDTO successDTO = createMockPaymentDTO();
        successDTO.setStatus("SUCCESS");
        when(paymentApplicationService.handleCallback(any(PaymentCallbackCommand.class))).thenReturn(successDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/payments/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"ORD123\",\"transactionNo\":\"TXN123\",\"success\":true,\"message\":\"支付成功\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    void should_returnBadRequest_when_callbackTransactionNoIsNull() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/payments/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"ORD123\",\"success\":true,\"message\":\"支付成功\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_refundPayment_when_paymentIsSuccess() throws Exception {
        // Given
        PaymentDTO refundedDTO = createMockPaymentDTO();
        refundedDTO.setStatus("REFUNDED");
        when(paymentApplicationService.refundPayment(any(String.class), any(RefundPaymentCommand.class))).thenReturn(refundedDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/payments/PAY123456/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"用户申请退款\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));
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
}