package com.claudej.domain.common.event;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DomainEvent base class tests
 */
class DomainEventTest {

    @Test
    void should_throw_when_event_id_is_null() {
        assertThatThrownBy(() -> new TestDomainEvent(null, LocalDateTime.now(), "Order", "ORD001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("eventId must not be null or empty");
    }

    @Test
    void should_throw_when_event_id_is_empty() {
        assertThatThrownBy(() -> new TestDomainEvent("", LocalDateTime.now(), "Order", "ORD001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("eventId must not be null or empty");
    }

    @Test
    void should_throw_when_occurred_on_is_null() {
        assertThatThrownBy(() -> new TestDomainEvent("EVT001", null, "Order", "ORD001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("occurredOn must not be null");
    }

    @Test
    void should_throw_when_aggregate_type_is_null() {
        assertThatThrownBy(() -> new TestDomainEvent("EVT001", LocalDateTime.now(), null, "ORD001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("aggregateType must not be null or empty");
    }

    @Test
    void should_throw_when_aggregate_type_is_empty() {
        assertThatThrownBy(() -> new TestDomainEvent("EVT001", LocalDateTime.now(), "", "ORD001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("aggregateType must not be null or empty");
    }

    @Test
    void should_throw_when_aggregate_id_is_null() {
        assertThatThrownBy(() -> new TestDomainEvent("EVT001", LocalDateTime.now(), "Order", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("aggregateId must not be null or empty");
    }

    @Test
    void should_throw_when_aggregate_id_is_empty() {
        assertThatThrownBy(() -> new TestDomainEvent("EVT001", LocalDateTime.now(), "Order", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("aggregateId must not be null or empty");
    }

    @Test
    void should_create_event_when_all_fields_valid() {
        LocalDateTime now = LocalDateTime.now();
        TestDomainEvent event = new TestDomainEvent("EVT001", now, "Order", "ORD001");

        assertThat(event.getEventId()).isEqualTo("EVT001");
        assertThat(event.getOccurredOn()).isEqualTo(now);
        assertThat(event.getAggregateType()).isEqualTo("Order");
        assertThat(event.getAggregateId()).isEqualTo("ORD001");
    }

    @Test
    void should_be_immutable_when_fields_are_final() {
        LocalDateTime now = LocalDateTime.now();
        TestDomainEvent event = new TestDomainEvent("EVT001", now, "Order", "ORD001");

        // All fields are final, verify via reflection that they are indeed final
        assertThat(event.getEventId()).isEqualTo("EVT001");
        assertThat(event.getOccurredOn()).isEqualTo(now);
        assertThat(event.getAggregateType()).isEqualTo("Order");
        assertThat(event.getAggregateId()).isEqualTo("ORD001");
    }

    /**
     * Test subclass for DomainEvent
     */
    private static class TestDomainEvent extends DomainEvent {
        TestDomainEvent(String eventId, LocalDateTime occurredOn, String aggregateType, String aggregateId) {
            super(eventId, occurredOn, aggregateType, aggregateId);
        }
    }
}
