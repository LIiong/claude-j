package com.claudej.domain.notification.model.valueobject;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 通知ID值对象
 */
@Getter
@EqualsAndHashCode
@ToString
public final class NotificationId {

    private final String value;

    public NotificationId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.NOTIFICATION_ID_EMPTY);
        }
        this.value = value.trim();
    }
}
