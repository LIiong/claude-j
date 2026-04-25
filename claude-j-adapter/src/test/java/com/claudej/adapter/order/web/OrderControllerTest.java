package com.claudej.adapter.order.web;

import com.claudej.adapter.common.GlobalExceptionHandler;
import com.claudej.adapter.order.web.request.CreateOrderFromCartRequest;
import com.claudej.adapter.order.web.request.CreateOrderRequest;
import com.claudej.application.order.dto.OrderDTO;
import com.claudej.application.order.service.OrderApplicationService;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
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
import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {OrderController.class, GlobalExceptionHandler.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderApplicationService orderApplicationService;

    private OrderDTO mockOrderDTO;

    @BeforeEach
    void setUp() {
        mockOrderDTO = new OrderDTO();
        mockOrderDTO.setOrderId("ORD123456");
        mockOrderDTO.setCustomerId("CUST001");
        mockOrderDTO.setStatus("CREATED");
        mockOrderDTO.setTotalAmount(new BigDecimal("11998.00"));
        mockOrderDTO.setCurrency("CNY");
        mockOrderDTO.setCreateTime(LocalDateTime.now());
        mockOrderDTO.setUpdateTime(LocalDateTime.now());
    }

    @Test
    void should_return200_when_createOrderSuccess() throws Exception {
        // Given
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId("CUST001");

        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId("PROD001");
        item.setProductName("iPhone");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("5999.00"));
        request.setItems(Arrays.asList(item));

        when(orderApplicationService.createOrder(any())).thenReturn(mockOrderDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.orderId", is("ORD123456")))
                .andExpect(jsonPath("$.data.customerId", is("CUST001")))
                .andExpect(jsonPath("$.data.status", is("CREATED")));
    }

    @Test
    void should_return400_when_createOrderWithInvalidInput() throws Exception {
        // Given
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId("");  // Invalid: empty customerId

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return200_when_getOrderByIdSuccess() throws Exception {
        // Given
        when(orderApplicationService.getOrderById("ORD123456")).thenReturn(mockOrderDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/orders/ORD123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.orderId", is("ORD123456")))
                .andExpect(jsonPath("$.data.customerId", is("CUST001")));
    }

    @Test
    void should_return404_when_getNonExistentOrder() throws Exception {
        // Given
        when(orderApplicationService.getOrderById("NONEXISTENT"))
                .thenThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/api/v1/orders/NONEXISTENT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ORDER_NOT_FOUND")));
    }

    @Test
    void should_return200_when_payOrderSuccess() throws Exception {
        // Given
        mockOrderDTO.setStatus("PAID");
        when(orderApplicationService.payOrder("ORD123456")).thenReturn(mockOrderDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/orders/ORD123456/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("PAID")));
    }

    @Test
    void should_return404_when_payNonExistentOrder() throws Exception {
        // Given
        when(orderApplicationService.payOrder("NONEXISTENT"))
                .thenThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // When & Then
        mockMvc.perform(post("/api/v1/orders/NONEXISTENT/pay"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ORDER_NOT_FOUND")));
    }

    @Test
    void should_return200_when_cancelOrderSuccess() throws Exception {
        // Given
        mockOrderDTO.setStatus("CANCELLED");
        when(orderApplicationService.cancelOrder("ORD123456")).thenReturn(mockOrderDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/orders/ORD123456/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("CANCELLED")));
    }

    @Test
    void should_return400_when_cancelNonCancellableOrder() throws Exception {
        // Given
        when(orderApplicationService.cancelOrder("ORD123456"))
                .thenThrow(new BusinessException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION));

        // When & Then
        mockMvc.perform(post("/api/v1/orders/ORD123456/cancel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("INVALID_ORDER_STATUS_TRANSITION")));
    }

    @Test
    void should_return200_when_createOrderFromCart_success() throws Exception {
        // Given
        CreateOrderFromCartRequest request = new CreateOrderFromCartRequest();
        request.setCustomerId("CUST001");

        mockOrderDTO.setTotalAmount(new BigDecimal("11998.00"));

        when(orderApplicationService.createOrderFromCart(any())).thenReturn(mockOrderDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/orders/from-cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.orderId", is("ORD123456")))
                .andExpect(jsonPath("$.data.customerId", is("CUST001")))
                .andExpect(jsonPath("$.data.status", is("CREATED")));
    }

    @Test
    void should_return400_when_createOrderFromCart_withBlankCustomerId() throws Exception {
        // Given
        CreateOrderFromCartRequest request = new CreateOrderFromCartRequest();
        request.setCustomerId("");  // Invalid: empty customerId

        // When & Then
        mockMvc.perform(post("/api/v1/orders/from-cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return404_when_createOrderFromCart_cartNotFound() throws Exception {
        // Given
        CreateOrderFromCartRequest request = new CreateOrderFromCartRequest();
        request.setCustomerId("CUST001");

        when(orderApplicationService.createOrderFromCart(any()))
                .thenThrow(new BusinessException(ErrorCode.CART_NOT_FOUND));

        // When & Then
        mockMvc.perform(post("/api/v1/orders/from-cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("CART_NOT_FOUND")));
    }

    @Test
    void should_return400_when_createOrderFromCart_cartIsEmpty() throws Exception {
        // Given
        CreateOrderFromCartRequest request = new CreateOrderFromCartRequest();
        request.setCustomerId("CUST001");

        when(orderApplicationService.createOrderFromCart(any()))
                .thenThrow(new BusinessException(ErrorCode.CART_EMPTY));

        // When & Then
        mockMvc.perform(post("/api/v1/orders/from-cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("CART_EMPTY")));
    }

    // --- Ship/Deliver/Refund endpoint tests ---

    @Test
    void should_return200_when_shipOrderSuccess() throws Exception {
        // Given
        mockOrderDTO.setStatus("SHIPPED");
        when(orderApplicationService.shipOrder("ORD123456")).thenReturn(mockOrderDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/orders/ORD123456/ship"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("SHIPPED")));
    }

    @Test
    void should_return400_when_shipInvalidStatusOrder() throws Exception {
        // Given
        when(orderApplicationService.shipOrder("ORD123456"))
                .thenThrow(new BusinessException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION));

        // When & Then
        mockMvc.perform(post("/api/v1/orders/ORD123456/ship"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("INVALID_ORDER_STATUS_TRANSITION")));
    }

    @Test
    void should_return404_when_shipNonExistentOrder() throws Exception {
        // Given
        when(orderApplicationService.shipOrder("NONEXISTENT"))
                .thenThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // When & Then
        mockMvc.perform(post("/api/v1/orders/NONEXISTENT/ship"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ORDER_NOT_FOUND")));
    }

    @Test
    void should_return200_when_deliverOrderSuccess() throws Exception {
        // Given
        mockOrderDTO.setStatus("DELIVERED");
        when(orderApplicationService.deliverOrder("ORD123456")).thenReturn(mockOrderDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/orders/ORD123456/deliver"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("DELIVERED")));
    }

    @Test
    void should_return400_when_deliverInvalidStatusOrder() throws Exception {
        // Given
        when(orderApplicationService.deliverOrder("ORD123456"))
                .thenThrow(new BusinessException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION));

        // When & Then
        mockMvc.perform(post("/api/v1/orders/ORD123456/deliver"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("INVALID_ORDER_STATUS_TRANSITION")));
    }

    @Test
    void should_return200_when_refundOrderSuccess() throws Exception {
        // Given
        mockOrderDTO.setStatus("REFUNDED");
        when(orderApplicationService.refundOrder("ORD123456")).thenReturn(mockOrderDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/orders/ORD123456/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("REFUNDED")));
    }

    @Test
    void should_return400_when_refundInvalidStatusOrder() throws Exception {
        // Given
        when(orderApplicationService.refundOrder("ORD123456"))
                .thenThrow(new BusinessException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION));

        // When & Then
        mockMvc.perform(post("/api/v1/orders/ORD123456/refund"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("INVALID_ORDER_STATUS_TRANSITION")));
    }

    @Test
    void should_return404_when_refundNonExistentOrder() throws Exception {
        // Given
        when(orderApplicationService.refundOrder("NONEXISTENT"))
                .thenThrow(new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // When & Then
        mockMvc.perform(post("/api/v1/orders/NONEXISTENT/refund"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ORDER_NOT_FOUND")));
    }

    @Test
    void should_return404_when_refundOrderCouponNotFound() throws Exception {
        // Given
        when(orderApplicationService.refundOrder("ORD123456"))
                .thenThrow(new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        // When & Then
        mockMvc.perform(post("/api/v1/orders/ORD123456/refund"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("COUPON_NOT_FOUND")));
    }
}
