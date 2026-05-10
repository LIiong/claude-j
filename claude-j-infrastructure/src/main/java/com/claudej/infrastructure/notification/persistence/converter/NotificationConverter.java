package com.claudej.infrastructure.notification.persistence.converter;

import com.claudej.domain.notification.model.aggregate.Notification;
import com.claudej.domain.notification.model.valueobject.NotificationChannel;
import com.claudej.domain.notification.model.valueobject.NotificationId;
import com.claudej.domain.notification.model.valueobject.NotificationPayload;
import com.claudej.domain.notification.model.valueobject.NotificationStatus;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.infrastructure.notification.persistence.dataobject.NotificationDO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class NotificationConverter {

    private final ObjectMapper objectMapper;

    public NotificationConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NotificationDO toDO(Notification notification) {
        NotificationDO notificationDO = new NotificationDO();
        notificationDO.setId(notification.getId());
        notificationDO.setNotificationId(notification.getNotificationId().getValue());
        notificationDO.setOrderId(notification.getOrderId().getValue());
        notificationDO.setChannel(notification.getChannel().name());
        notificationDO.setStatus(notification.getStatus().name());
        notificationDO.setPayloadJson(toPayloadJson(notification.getPayload()));
        notificationDO.setSentAt(notification.getSentAt());
        notificationDO.setCreatedAt(notification.getCreatedAt());
        notificationDO.setUpdatedAt(notification.getUpdatedAt());
        return notificationDO;
    }

    public Notification toDomain(NotificationDO notificationDO) {
        NotificationPayload payload = toPayload(notificationDO.getPayloadJson());
        return Notification.reconstruct(
                notificationDO.getId(),
                new NotificationId(notificationDO.getNotificationId()),
                new OrderId(notificationDO.getOrderId()),
                NotificationChannel.valueOf(notificationDO.getChannel()),
                NotificationStatus.valueOf(notificationDO.getStatus()),
                payload,
                notificationDO.getSentAt(),
                notificationDO.getCreatedAt(),
                notificationDO.getUpdatedAt()
        );
    }

    private String toPayloadJson(NotificationPayload payload) {
        Map<String, Object> payloadMap = new HashMap<String, Object>();
        payloadMap.put("orderId", payload.getOrderId());
        payloadMap.put("customerId", payload.getCustomerId());
        payloadMap.put("totalAmount", payload.getTotalAmount());
        payloadMap.put("message", payload.getMessage());
        try {
            return objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize notification payload", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private NotificationPayload toPayload(String payloadJson) {
        try {
            Map<String, Object> payloadMap = objectMapper.readValue(payloadJson, Map.class);
            return NotificationPayload.of(
                    new OrderId((String) payloadMap.get("orderId")),
                    (String) payloadMap.get("customerId"),
                    new java.math.BigDecimal(String.valueOf(payloadMap.get("totalAmount"))),
                    (String) payloadMap.get("message")
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to deserialize notification payload", ex);
        }
    }
}
