package com.claudej.infrastructure.order.persistence.metrics;

import com.claudej.application.order.port.OrderMetricsPort;

/**
 * No-op metrics implementation for contexts without MeterRegistry.
 */
public class NoOpOrderMetricsPort implements OrderMetricsPort {

    @Override
    public void recordCreateOrderSuccess(String source) {
        // Intentionally no-op for contexts without metrics export.
    }

    @Override
    public void recordCreateOrderFailure(String source, String reasonType) {
        // Intentionally no-op for contexts without metrics export.
    }

    @Override
    public void recordCreateOrderDuration(String source, String outcome, long nanos) {
        // Intentionally no-op for contexts without metrics export.
    }
}
