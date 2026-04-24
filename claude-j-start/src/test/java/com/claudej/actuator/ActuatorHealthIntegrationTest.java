package com.claudej.actuator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Actuator health endpoint integration tests.
 * Validates health, liveness, and readiness endpoints for K8s probes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class ActuatorHealthIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void should_return_200_when_actuator_health_endpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("status");
    }

    @Test
    void should_return_200_when_actuator_health_liveness_endpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health/liveness", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("status");
    }

    @Test
    void should_return_200_when_actuator_health_readiness_endpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health/readiness", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("status");
    }

    @Test
    void should_show_details_when_dev_environment_health_endpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("components");
    }

    @Test
    void should_return_200_when_actuator_info_endpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/info", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void should_return_200_when_actuator_metrics_endpoint_in_dev() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/metrics", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}