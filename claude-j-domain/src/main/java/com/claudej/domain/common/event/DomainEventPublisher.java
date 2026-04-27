package com.claudej.domain.common.event;

/**
 * Domain event publisher port interface - implemented by Infrastructure layer
 */
public interface DomainEventPublisher {

    /**
     * Publish a domain event
     */
    void publish(DomainEvent event);
}
