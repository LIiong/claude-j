package com.claudej.domain.common.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Domain event base class - all domain events inherit from this
 */
@Getter
public abstract class DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;
    private final String aggregateType;
    private final String aggregateId;

    protected DomainEvent(String eventId, LocalDateTime occurredOn, String aggregateType, String aggregateId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new IllegalArgumentException("eventId must not be null or empty");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("occurredOn must not be null");
        }
        if (aggregateType == null || aggregateType.trim().isEmpty()) {
            throw new IllegalArgumentException("aggregateType must not be null or empty");
        }
        if (aggregateId == null || aggregateId.trim().isEmpty()) {
            throw new IllegalArgumentException("aggregateId must not be null or empty");
        }
        this.eventId = eventId;
        this.occurredOn = occurredOn;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
    }
}
