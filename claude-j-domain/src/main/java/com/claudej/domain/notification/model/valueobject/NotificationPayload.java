package com.claudej.domain.notification.model.valueobject;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.order.model.valobj.OrderId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 通知载荷值对象
 */
@Getter
@EqualsAndHashCode
@ToString
public final class NotificationPayload {

    private final String orderId;
    private final String customerId;
    private final BigDecimal totalAmount;
    private final String message;

    private NotificationPayload(String orderId, String customerId, BigDecimal totalAmount, String message) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.message = message;
    }

    public static NotificationPayload of(OrderId orderId, String customerId, BigDecimal totalAmount, String message) {
        if (orderId == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_ORDER_ID_EMPTY);
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.NOTIFICATION_CUSTOMER_ID_EMPTY);
        }
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.NOTIFICATION_TOTAL_AMOUNT_INVALID);
        }
        if (message == null || message.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.NOTIFICATION_PAYLOAD_MESSAGE_EMPTY);
        }
        return new NotificationPayload(orderId.getValue(), customerId.trim(), totalAmount, message.trim());
    }
}
