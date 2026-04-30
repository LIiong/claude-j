package com.claudej.actuator;

import com.claudej.application.order.port.OrderMetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.metrics.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Actuator prometheus endpoint integration tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMetrics
@ActiveProfiles("dev")
class ActuatorPrometheusIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderMetricsPort orderMetricsPort;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void should_return_200_when_actuator_prometheus_endpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void should_include_order_metrics_when_actuator_prometheus_endpoint() {
        assertThat(orderMetricsPort.getClass().getName())
                .contains("MicrometerOrderMetricsRecorder");
        assertThat(meterRegistry.getClass().getName())
                .contains("PrometheusMeterRegistry");

        orderMetricsPort.recordCreateOrderSuccess("direct");
        orderMetricsPort.recordCreateOrderFailure("direct", "business");
        orderMetricsPort.recordCreateOrderDuration("direct", "success", 1L);

        assertThat(meterRegistry.find("claudej_order_create_total").tag("source", "direct").counter())
                .isNotNull();
        assertThat(meterRegistry.find("claudej_order_create_failure_total")
                .tags("source", "direct", "reason_type", "business").counter())
                .isNotNull();
        assertThat(meterRegistry.find("claudej_order_create_duration")
                .tags("source", "direct", "outcome", "success").timer())
                .isNotNull();

        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("claudej_order_create_total");
        assertThat(response.getBody()).contains("claudej_order_create_failure_total");
        assertThat(response.getBody()).contains("claudej_order_create_duration_seconds");
    }
}
