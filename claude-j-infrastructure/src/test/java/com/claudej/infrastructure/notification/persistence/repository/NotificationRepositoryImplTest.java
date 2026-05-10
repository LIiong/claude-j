package com.claudej.infrastructure.notification.persistence.repository;

import com.claudej.domain.notification.model.aggregate.Notification;
import com.claudej.domain.notification.model.valueobject.NotificationChannel;
import com.claudej.domain.notification.model.valueobject.NotificationPayload;
import com.claudej.domain.notification.repository.NotificationRepository;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.infrastructure.notification.persistence.converter.NotificationConverter;
import com.claudej.infrastructure.notification.persistence.mapper.NotificationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:notification_repo_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@Transactional
class NotificationRepositoryImplTest {

    @SpringBootApplication(scanBasePackageClasses = {
            NotificationRepositoryImpl.class,
            NotificationConverter.class,
            NotificationMapper.class
    })
    @MapperScan(basePackageClasses = NotificationMapper.class)
    static class TestConfig {

        @org.springframework.context.annotation.Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @org.springframework.context.annotation.Bean
        org.springframework.boot.CommandLineRunner notificationTableInitializer(javax.sql.DataSource dataSource) {
            return args -> {
                try (java.sql.Connection connection = dataSource.getConnection();
                     java.sql.Statement statement = connection.createStatement()) {
                    statement.execute("CREATE TABLE IF NOT EXISTS t_notification (" +
                            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                            "notification_id VARCHAR(64) NOT NULL, " +
                            "order_id VARCHAR(64) NOT NULL, " +
                            "channel VARCHAR(32) NOT NULL, " +
                            "status VARCHAR(32) NOT NULL, " +
                            "payload_json CLOB NOT NULL, " +
                            "sent_at TIMESTAMP NULL, " +
                            "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                            "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
                    statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_notification_order_channel ON t_notification(order_id, channel)");
                }
            };
        }
    }

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void should_saveNotification_when_notificationIsNew() {
        Notification notification = Notification.createPending(
                new OrderId("ORD-100"),
                NotificationChannel.INTERNAL,
                NotificationPayload.of(new OrderId("ORD-100"), "CUST-100", new BigDecimal("66.00"), "created")
        );

        Notification saved = notificationRepository.save(notification);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getNotificationId()).isNotNull();
    }

    @Test
    void should_findNotificationByOrderIdAndChannel_when_notificationExists() {
        Notification notification = Notification.createPending(
                new OrderId("ORD-101"),
                NotificationChannel.INTERNAL,
                NotificationPayload.of(new OrderId("ORD-101"), "CUST-101", new BigDecimal("77.00"), "created")
        );
        notificationRepository.save(notification);

        Optional<Notification> found = notificationRepository.findByOrderIdAndChannel(
                new OrderId("ORD-101"),
                NotificationChannel.INTERNAL
        );

        assertThat(found).isPresent();
        assertThat(found.get().getPayload().getCustomerId()).isEqualTo("CUST-101");
        assertThat(found.get().getPayload().getTotalAmount()).isEqualByComparingTo("77.00");
    }
}
