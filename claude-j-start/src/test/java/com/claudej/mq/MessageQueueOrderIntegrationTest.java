package com.claudej.mq;

import com.claudej.application.notification.port.NotificationSender;
import com.claudej.application.notification.service.NotificationApplicationService;
import com.claudej.application.order.command.CreateOrderCommand;
import com.claudej.application.order.dto.OrderCreatedMessage;
import com.claudej.application.order.dto.OrderDTO;
import com.claudej.application.order.service.OrderApplicationService;
import com.claudej.domain.notification.model.aggregate.Notification;
import com.claudej.domain.notification.model.valueobject.NotificationChannel;
import com.claudej.domain.notification.repository.NotificationRepository;
import com.claudej.domain.order.model.valobj.OrderId;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "claudej.rabbitmq.order-created-exchange=test.order.created.exchange",
        "claudej.rabbitmq.order-created-queue=test.order.created.queue",
        "claudej.rabbitmq.order-created-routing-key=test.order.created"
})
@ActiveProfiles("test")
class MessageQueueOrderIntegrationTest {

    @TestConfiguration
    static class TestConfig {

        @Bean
        NotificationSender notificationSender() {
            return notification -> {
                // Test double keeps the integration scope focused on MQ bridge and persistence.
            };
        }

        @Bean
        @Primary
        RabbitTemplate rabbitTemplate(NotificationApplicationService notificationApplicationService) {
            return new RabbitTemplate(new CachingConnectionFactory("localhost")) {
                @Override
                public void convertAndSend(String exchange, String routingKey, Object object) {
                    notificationApplicationService.handleOrderCreated((OrderCreatedMessage) object);
                }
            };
        }
    }

    @Autowired
    private OrderApplicationService orderApplicationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private com.claudej.application.order.port.OrderMetricsPort orderMetricsPort;

    @Test
    void should_createNotificationRecord_when_orderCreatedEventBridgesToMessageConsumer() {
        CreateOrderCommand command = new CreateOrderCommand();
        command.setCustomerId("CUST-MQ-001");
        CreateOrderCommand.OrderItemCommand item = new CreateOrderCommand.OrderItemCommand();
        item.setProductId("PROD-MQ-001");
        item.setProductName("MQ Product");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("88.00"));
        command.setItems(Collections.singletonList(item));

        OrderDTO order = orderApplicationService.createOrder(command);

        Optional<Notification> notification = notificationRepository.findByOrderIdAndChannel(
                new OrderId(order.getOrderId()),
                NotificationChannel.INTERNAL
        );

        assertThat(notification).isPresent();
        assertThat(notification.get().getPayload().getCustomerId()).isEqualTo("CUST-MQ-001");
        assertThat(notification.get().getStatus().name()).isEqualTo("SENT");
    }
}
