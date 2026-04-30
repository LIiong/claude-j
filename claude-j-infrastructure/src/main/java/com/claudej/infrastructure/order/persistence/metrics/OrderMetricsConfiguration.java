package com.claudej.infrastructure.order.persistence.metrics;

import com.claudej.application.order.port.OrderMetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Order metrics bean configuration.
 */
@Configuration
public class OrderMetricsConfiguration {

    @Bean
    public OrderMetricsPort orderMetricsPort(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry == null) {
            return new NoOpOrderMetricsPort();
        }
        return new MicrometerOrderMetricsRecorder(meterRegistry);
    }
}
