package com.claudej.infrastructure.order.persistence.repository;

import com.claudej.domain.order.model.aggregate.Order;
import com.claudej.domain.order.model.entity.OrderItem;
import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.order.model.valobj.Money;
import com.claudej.domain.order.model.valobj.OrderId;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderRepositoryImplTest {

    @SpringBootApplication(
            exclude = {
                    DataSourceAutoConfiguration.class,
                    DataSourceTransactionManagerAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class,
                    TransactionAutoConfiguration.class
            },
            scanBasePackages = "com.claudej.infrastructure.order.persistence"
    )
    @MapperScan(basePackages = "com.claudej.infrastructure.order.persistence.mapper")
    @Import(OrderRepositoryImplTest.TestConfig.class)
    @ComponentScan(basePackages = "com.claudej.infrastructure.order.persistence", excludeFilters = {
            @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.claudej\\.infrastructure\\.inventory\\..*")
    })
    static class TestConfig {

        @Bean
        public SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Autowired
    private OrderRepositoryImpl orderRepository;

    @Test
    void should_saveNewOrder_when_orderHasNoId() {
        // Given
        Order order = Order.create(new CustomerId("CUST001"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 2, Money.cny(5999)));

        // When
        Order savedOrder = orderRepository.save(order);

        // Then
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getCustomerIdValue()).isEqualTo("CUST001");
    }

    @Test
    void should_findOrderByOrderId_when_orderExists() {
        // Given
        Order order = Order.create(new CustomerId("CUST002"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));
        Order saved = orderRepository.save(order);

        // When
        Optional<Order> found = orderRepository.findByOrderId(new OrderId(saved.getOrderIdValue()));

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getCustomerIdValue()).isEqualTo("CUST002");
        assertThat(found.get().getItems()).hasSize(1);
    }

    @Test
    void should_returnEmpty_when_orderNotFound() {
        // When
        Optional<Order> found = orderRepository.findByOrderId(new OrderId("NONEXISTENT"));

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void should_findOrdersByCustomerId_when_ordersExist() {
        // Given
        Order order1 = Order.create(new CustomerId("CUST003"));
        order1.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));
        Order order2 = Order.create(new CustomerId("CUST003"));
        order2.addItem(OrderItem.create("PROD002", "MacBook", 1, Money.cny(12999)));
        orderRepository.save(order1);
        orderRepository.save(order2);

        // When
        List<Order> orders = orderRepository.findByCustomerId(new CustomerId("CUST003"));

        // Then
        assertThat(orders).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void should_returnTrue_when_orderExists() {
        // Given
        Order order = Order.create(new CustomerId("CUST004"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));
        Order saved = orderRepository.save(order);

        // When & Then
        assertThat(orderRepository.existsByOrderId(new OrderId(saved.getOrderIdValue()))).isTrue();
    }

    @Test
    void should_returnFalse_when_orderDoesNotExist() {
        // When & Then
        assertThat(orderRepository.existsByOrderId(new OrderId("NONEXISTENT"))).isFalse();
    }

    @Test
    void should_updateOrder_when_orderHasId() {
        // Given
        Order order = Order.create(new CustomerId("CUST005"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));
        Order saved = orderRepository.save(order);

        // When - 添加新订单项并保存
        saved.addItem(OrderItem.create("PROD002", "MacBook", 1, Money.cny(12999)));
        Order updated = orderRepository.save(saved);

        // Then
        Optional<Order> found = orderRepository.findByOrderId(new OrderId(updated.getOrderIdValue()));
        assertThat(found).isPresent();
        assertThat(found.get().getItems()).hasSize(2);
    }

    @Test
    void should_calculateTotalAmount_when_saveOrder() {
        // Given
        Order order = Order.create(new CustomerId("CUST006"));
        order.addItem(OrderItem.create("PROD001", "iPhone", 2, Money.cny(5000)));
        order.addItem(OrderItem.create("PROD002", "MacBook", 1, Money.cny(10000)));

        // When
        Order saved = orderRepository.save(order);

        // Then
        Optional<Order> found = orderRepository.findByOrderId(new OrderId(saved.getOrderIdValue()));
        assertThat(found).isPresent();
        assertThat(found.get().getTotalAmount().getAmount().intValue()).isEqualTo(20000);
    }

    @Test
    void should_saveOrderItems_when_saveOrder() {
        // Given
        Order order = Order.create(new CustomerId("CUST007"));
        OrderItem item1 = OrderItem.create("PROD001", "iPhone 15", 2, Money.cny(5999));
        OrderItem item2 = OrderItem.create("PROD002", "AirPods", 1, Money.cny(1999));
        order.addItem(item1);
        order.addItem(item2);

        // When
        Order saved = orderRepository.save(order);

        // Then
        Optional<Order> found = orderRepository.findByOrderId(new OrderId(saved.getOrderIdValue()));
        assertThat(found).isPresent();
        assertThat(found.get().getItems()).hasSize(2);

        // 验证订单项属性
        OrderItem firstItem = found.get().getItems().get(0);
        assertThat(firstItem.getProductId()).isEqualTo("PROD001");
        assertThat(firstItem.getProductName()).isEqualTo("iPhone 15");
        assertThat(firstItem.getQuantity()).isEqualTo(2);
    }
}
