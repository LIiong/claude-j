package com.claudej.infrastructure.common.event;

import com.claudej.domain.common.event.DomainEvent;
import com.claudej.domain.common.event.DomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring implementation of DomainEventPublisher
 * Delegates to Spring's ApplicationEventPublisher
 */
@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        applicationEventPublisher.publishEvent(event);
    }
}
