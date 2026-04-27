package com.claudej.infrastructure.common.event;

import com.claudej.domain.common.event.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

/**
 * SpringDomainEventPublisher tests
 */
@ExtendWith(MockitoExtension.class)
class SpringDomainEventPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private SpringDomainEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new SpringDomainEventPublisher(applicationEventPublisher);
    }

    @Test
    void should_publish_event_when_event_is_valid() {
        // Given
        TestDomainEvent event = new TestDomainEvent("EVT001", LocalDateTime.now(), "Order", "ORD001");

        // When
        publisher.publish(event);

        // Then
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isEqualTo(event);
    }

    @Test
    void should_throw_when_event_is_null() {
        assertThatThrownBy(() -> publisher.publish(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("event must not be null");
    }

    /**
     * Test domain event
     */
    private static class TestDomainEvent extends DomainEvent {
        TestDomainEvent(String eventId, LocalDateTime occurredOn, String aggregateType, String aggregateId) {
            super(eventId, occurredOn, aggregateType, aggregateId);
        }
    }
}
