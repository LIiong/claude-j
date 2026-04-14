package com.claudej.application.order.service;

import com.claudej.application.order.assembler.OrderAssembler;
import com.claudej.application.order.command.CreateOrderCommand;
import com.claudej.application.order.dto.OrderDTO;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.order.model.aggregate.Order;
import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.domain.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderAssembler orderAssembler;

    @InjectMocks
    private OrderApplicationService orderApplicationService;

    private CreateOrderCommand createCommand;
    private Order mockOrder;
    private OrderDTO mockOrderDTO;

    @BeforeEach
    void setUp() {
        createCommand = new CreateOrderCommand();
        createCommand.setCustomerId("CUST001");

        CreateOrderCommand.OrderItemCommand item1 = new CreateOrderCommand.OrderItemCommand();
        item1.setProductId("PROD001");
        item1.setProductName("iPhone");
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("5999.00"));

        createCommand.setItems(Arrays.asList(item1));

        mockOrder = Order.create(new CustomerId("CUST001"));
        mockOrder.addItem(com.claudej.domain.order.model.entity.OrderItem.create(
                "PROD001", "iPhone", 2,
                com.claudej.domain.order.model.valobj.Money.cny(5999)
        ));

        mockOrderDTO = new OrderDTO();
        mockOrderDTO.setOrderId("ORD123456");
        mockOrderDTO.setCustomerId("CUST001");
        mockOrderDTO.setStatus("CREATED");
    }

    @Test
    void should_createOrder_when_validCommandProvided() {
        // Given
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        // When
        OrderDTO result = orderApplicationService.createOrder(createCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo("ORD123456");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void should_throwException_when_createOrderWithNullCommand() {
        // When & Then
        assertThatThrownBy(() -> orderApplicationService.createOrder(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("客户ID不能为空");
    }

    @Test
    void should_throwException_when_createOrderWithEmptyItems() {
        // Given
        createCommand.setItems(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> orderApplicationService.createOrder(createCommand))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单项不能为空");
    }

    @Test
    void should_returnOrder_when_getOrderByIdExists() {
        // Given
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        // When
        OrderDTO result = orderApplicationService.getOrderById("ORD123456");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo("ORD123456");
    }

    @Test
    void should_throwException_when_getOrderByIdNotExists() {
        // Given
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderApplicationService.getOrderById("NONEXISTENT"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单不存在");
    }

    @Test
    void should_payOrder_when_orderExistsAndCreated() {
        // Given
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        // When
        OrderDTO result = orderApplicationService.payOrder("ORD123456");

        // Then
        assertThat(result).isNotNull();
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void should_cancelOrder_when_orderExistsAndCancellable() {
        // Given
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        // When
        OrderDTO result = orderApplicationService.cancelOrder("ORD123456");

        // Then
        assertThat(result).isNotNull();
        verify(orderRepository).save(any(Order.class));
    }
}
