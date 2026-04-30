package com.claudej.infrastructure.order.persistence.metrics;

import com.claudej.application.order.port.OrderMetricsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import javax.annotation.PostConstruct;

/**
 * Micrometer order metrics recorder.
 */
public class MicrometerOrderMetricsRecorder implements OrderMetricsPort {

    private final MeterRegistry meterRegistry;
    private Counter createOrderSuccessDirect;
    private Counter createOrderSuccessCart;
    private Counter createOrderFailureDirectValidation;
    private Counter createOrderFailureDirectBusiness;
    private Counter createOrderFailureDirectSystem;
    private Counter createOrderFailureCartValidation;
    private Counter createOrderFailureCartBusiness;
    private Counter createOrderFailureCartSystem;
    private Timer createOrderTimer;

    public MicrometerOrderMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        createOrderSuccessDirect = Counter.builder("claudej_order_create_total")
                .tag("source", "direct")
                .register(meterRegistry);
        createOrderSuccessCart = Counter.builder("claudej_order_create_total")
                .tag("source", "cart")
                .register(meterRegistry);
        createOrderFailureDirectValidation = Counter.builder("claudej_order_create_failure_total")
                .tag("source", "direct")
                .tag("reason_type", "validation")
                .register(meterRegistry);
        createOrderFailureDirectBusiness = Counter.builder("claudej_order_create_failure_total")
                .tag("source", "direct")
                .tag("reason_type", "business")
                .register(meterRegistry);
        createOrderFailureDirectSystem = Counter.builder("claudej_order_create_failure_total")
                .tag("source", "direct")
                .tag("reason_type", "system")
                .register(meterRegistry);
        createOrderFailureCartValidation = Counter.builder("claudej_order_create_failure_total")
                .tag("source", "cart")
                .tag("reason_type", "validation")
                .register(meterRegistry);
        createOrderFailureCartBusiness = Counter.builder("claudej_order_create_failure_total")
                .tag("source", "cart")
                .tag("reason_type", "business")
                .register(meterRegistry);
        createOrderFailureCartSystem = Counter.builder("claudej_order_create_failure_total")
                .tag("source", "cart")
                .tag("reason_type", "system")
                .register(meterRegistry);
        createOrderTimer = Timer.builder("claudej_order_create_duration")
                .tag("source", "direct")
                .tag("outcome", "success")
                .register(meterRegistry);
        Timer.builder("claudej_order_create_duration")
                .tag("source", "direct")
                .tag("outcome", "business_error")
                .register(meterRegistry);
        Timer.builder("claudej_order_create_duration")
                .tag("source", "direct")
                .tag("outcome", "system_error")
                .register(meterRegistry);
        Timer.builder("claudej_order_create_duration")
                .tag("source", "cart")
                .tag("outcome", "success")
                .register(meterRegistry);
        Timer.builder("claudej_order_create_duration")
                .tag("source", "cart")
                .tag("outcome", "business_error")
                .register(meterRegistry);
        Timer.builder("claudej_order_create_duration")
                .tag("source", "cart")
                .tag("outcome", "system_error")
                .register(meterRegistry);
    }

    @Override
    public void recordCreateOrderSuccess(String source) {
        if ("cart".equals(source)) {
            createOrderSuccessCart.increment();
            return;
        }
        createOrderSuccessDirect.increment();
    }

    @Override
    public void recordCreateOrderFailure(String source, String reasonType) {
        if ("cart".equals(source)) {
            if ("validation".equals(reasonType)) {
                createOrderFailureCartValidation.increment();
                return;
            }
            if ("business".equals(reasonType)) {
                createOrderFailureCartBusiness.increment();
                return;
            }
            createOrderFailureCartSystem.increment();
            return;
        }
        if ("validation".equals(reasonType)) {
            createOrderFailureDirectValidation.increment();
            return;
        }
        if ("business".equals(reasonType)) {
            createOrderFailureDirectBusiness.increment();
            return;
        }
        createOrderFailureDirectSystem.increment();
    }

    @Override
    public void recordCreateOrderDuration(String source, String outcome, long nanos) {
        Timer timer = meterRegistry.find("claudej_order_create_duration")
                .tag("source", source)
                .tag("outcome", outcome)
                .timer();
        if (timer == null) {
            return;
        }
        timer.record(nanos, java.util.concurrent.TimeUnit.NANOSECONDS);
    }
}
