package com.claudej.domain.notification.model.valueobject;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationIdTest {

    @Test
    void should_throwBusinessException_when_notificationIdIsBlank() {
        assertThatThrownBy(() -> new NotificationId("   "))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTIFICATION_ID_EMPTY);
    }

    @Test
    void should_trimValue_when_notificationIdHasWhitespace() {
        NotificationId notificationId = new NotificationId("  notify-001  ");

        assertThat(notificationId.getValue()).isEqualTo("notify-001");
    }
}
