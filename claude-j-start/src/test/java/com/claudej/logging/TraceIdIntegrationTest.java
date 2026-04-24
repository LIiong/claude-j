package com.claudej.logging;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TraceId integration tests.
 * Validates requestId generation and X-Request-Id header propagation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class TraceIdIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void should_return_x_request_id_header_when_http_request() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().containsKey("X-Request-Id")).isTrue();
        String requestId = response.getHeaders().getFirst("X-Request-Id");
        assertThat(requestId).hasSize(32);
        assertThat(requestId).matches("[a-f0-9]{32}");
    }

    @Test
    void should_have_different_request_id_for_different_requests() {
        ResponseEntity<String> response1 = restTemplate.getForEntity("/actuator/health", String.class);
        ResponseEntity<String> response2 = restTemplate.getForEntity("/actuator/health", String.class);
        String requestId1 = response1.getHeaders().getFirst("X-Request-Id");
        String requestId2 = response2.getHeaders().getFirst("X-Request-Id");
        assertThat(requestId1).isNotEqualTo(requestId2);
    }
}