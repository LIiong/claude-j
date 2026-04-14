package com.claudej.adapter.user.web;

import com.claudej.adapter.common.GlobalExceptionHandler;
import com.claudej.adapter.order.web.response.OrderResponse;
import com.claudej.application.order.dto.OrderDTO;
import com.claudej.application.user.service.UserOrderQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {UserOrderController.class, GlobalExceptionHandler.class})
class UserOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserOrderQueryService userOrderQueryService;

    private OrderDTO mockOrderDTO;

    @BeforeEach
    void setUp() {
        mockOrderDTO = new OrderDTO();
        mockOrderDTO.setOrderId("ORD123456");
        mockOrderDTO.setCustomerId("UR1234567890ABCDEF");
        mockOrderDTO.setStatus("CREATED");
        mockOrderDTO.setTotalAmount(new BigDecimal("11998.00"));
        mockOrderDTO.setCurrency("CNY");
        mockOrderDTO.setCreateTime(LocalDateTime.now());
        mockOrderDTO.setUpdateTime(LocalDateTime.now());
    }

    @Test
    void should_return200_when_getUserOrdersSuccess() throws Exception {
        // Given
        String userId = "UR1234567890ABCDEF";
        when(userOrderQueryService.getUserOrders(userId))
                .thenReturn(Arrays.asList(mockOrderDTO));

        // When & Then
        mockMvc.perform(get("/api/v1/users/" + userId + "/orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].orderId", is("ORD123456")))
                .andExpect(jsonPath("$.data[0].status", is("CREATED")))
                .andExpect(jsonPath("$.data[0].totalAmount", is(11998.00)));
    }

    @Test
    void should_return200_when_userHasNoOrders() throws Exception {
        // Given
        String userId = "UR1234567890ABCDEF";
        when(userOrderQueryService.getUserOrders(userId))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/users/" + userId + "/orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void should_return200_when_getOrderDetailSuccess() throws Exception {
        // Given
        String userId = "UR1234567890ABCDEF";
        String orderId = "ORD123456";
        when(userOrderQueryService.getUserOrderDetail(userId, orderId))
                .thenReturn(mockOrderDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/users/" + userId + "/orders/" + orderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.orderId", is("ORD123456")))
                .andExpect(jsonPath("$.data.customerId", is("UR1234567890ABCDEF")))
                .andExpect(jsonPath("$.data.status", is("CREATED")));
    }

    @Test
    void should_return200_when_orderDoesNotBelongToUser() throws Exception {
        // Given
        String userId = "UR1234567890ABCDEF";
        String orderId = "ORD789012";
        when(userOrderQueryService.getUserOrderDetail(userId, orderId))
                .thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/v1/users/" + userId + "/orders/" + orderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ORDER_NOT_FOUND")));
    }
}
