package com.claudej.infrastructure.notification.persistence.repository;

import com.claudej.infrastructure.test.MySqlRepositoryIntegrationTestSupport;
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

@SpringBootTest
@Transactional
class NotificationRepositoryImplTest extends MySqlRepositoryIntegrationTestSupport {

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

    @Test
    void should_preservePayloadMessage_when_payloadContainsComma() {
        Notification notification = Notification.createPending(
                new OrderId("ORD-102"),
                NotificationChannel.INTERNAL,
                NotificationPayload.of(
                        new OrderId("ORD-102"),
                        "CUST-102",
                        new BigDecimal("88.00"),
                        "created, notify warehouse"
                )
        );
        notificationRepository.save(notification);

        Optional<Notification> found = notificationRepository.findByOrderIdAndChannel(
                new OrderId("ORD-102"),
                NotificationChannel.INTERNAL
        );

        assertThat(found).isPresent();
        assertThat(found.get().getPayload().getMessage()).isEqualTo("created, notify warehouse");
    }
}
