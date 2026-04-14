package com.claudej.application.user.service;

import com.claudej.application.order.dto.OrderDTO;
import com.claudej.application.order.service.OrderApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserOrderQueryServiceTest {

    @Mock
    private OrderApplicationService orderApplicationService;

    @InjectMocks
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
    void should_returnOrders_when_userHasOrders() {
        // Given
        String userId = "UR1234567890ABCDEF";
        when(orderApplicationService.getOrdersByCustomerId(userId))
                .thenReturn(Arrays.asList(mockOrderDTO));

        // When
        java.util.List<OrderDTO> result = userOrderQueryService.getUserOrders(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderId()).isEqualTo("ORD123456");
    }

    @Test
    void should_returnEmptyList_when_userHasNoOrders() {
        // Given
        String userId = "UR1234567890ABCDEF";
        when(orderApplicationService.getOrdersByCustomerId(userId))
                .thenReturn(Collections.emptyList());

        // When
        java.util.List<OrderDTO> result = userOrderQueryService.getUserOrders(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnOrderDetail_when_orderBelongsToUser() {
        // Given
        String userId = "UR1234567890ABCDEF";
        String orderId = "ORD123456";
        when(orderApplicationService.getOrderById(orderId)).thenReturn(mockOrderDTO);

        // When
        OrderDTO result = userOrderQueryService.getUserOrderDetail(userId, orderId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo("ORD123456");
    }

    @Test
    void should_returnNull_when_orderDoesNotBelongToUser() {
        // Given
        String userId = "UR1234567890ABCDEF";
        String orderId = "ORD789012";
        OrderDTO otherOrder = new OrderDTO();
        otherOrder.setOrderId("ORD789012");
        otherOrder.setCustomerId("OTHER_USER"); // Different customer
        when(orderApplicationService.getOrderById(orderId)).thenReturn(otherOrder);

        // When
        OrderDTO result = userOrderQueryService.getUserOrderDetail(userId, orderId);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void should_returnNull_when_orderNotFound() {
        // Given
        String userId = "UR1234567890ABCDEF";
        String orderId = "NONEXISTENT";
        when(orderApplicationService.getOrderById(orderId)).thenReturn(null);

        // When
        OrderDTO result = userOrderQueryService.getUserOrderDetail(userId, orderId);

        // Then
        assertThat(result).isNull();
    }
}
