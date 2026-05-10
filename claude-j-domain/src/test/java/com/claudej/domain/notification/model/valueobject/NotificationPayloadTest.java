package com.claudej.domain.notification.model.valueobject;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.order.model.valobj.OrderId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationPayloadTest {

    @Test
    void should_throwBusinessException_when_messageIsBlank() {
        assertThatThrownBy(() -> NotificationPayload.of(new OrderId("ORD-001"), "CUST-001", new BigDecimal("99.00"), "   "))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTIFICATION_PAYLOAD_MESSAGE_EMPTY);
    }

    @Test
    void should_createPayload_when_orderSnapshotIsComplete() {
        NotificationPayload payload = NotificationPayload.of(
                new OrderId("ORD-001"),
                "CUST-001",
                new BigDecimal("99.00"),
                "order created"
        );

        assertThat(payload.getOrderId()).isEqualTo("ORD-001");
        assertThat(payload.getCustomerId()).isEqualTo("CUST-001");
        assertThat(payload.getTotalAmount()).isEqualByComparingTo("99.00");
        assertThat(payload.getMessage()).isEqualTo("order created");
    }
}
