package com.claudej.config;

import com.claudej.ClaudeJApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ClaudeJApplication.class, properties = {
        "app.security.cors.enabled=true",
        "app.security.cors.allowed-origins[0]=http://localhost:3000",
        "app.security.cors.allowed-origins[1]=http://127.0.0.1:3000",
        "app.security.cors.allowed-methods[0]=GET",
        "app.security.cors.allowed-methods[1]=POST",
        "app.security.cors.allowed-methods[2]=OPTIONS",
        "app.security.cors.allowed-headers[0]=Authorization",
        "app.security.cors.allowed-headers[1]=Content-Type",
        "app.security.cors.allowed-headers[2]=Accept",
        "app.security.cors.allowed-headers[3]=Origin"
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CorsSecurityIntegrationTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";
    private static final String BLOCKED_ORIGIN = "http://evil.example.com";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_return_cors_headers_when_preflight_request_from_allowed_origin() throws Exception {
        mockMvc.perform(options("/api/v1/users/UR1234567890ABCDEF")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void should_return_401_with_cors_header_when_request_without_jwt_from_allowed_origin() throws Exception {
        mockMvc.perform(get("/api/v1/users/UR1234567890ABCDEF")
                        .header("Origin", ALLOWED_ORIGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }

    @Test
    void should_reject_preflight_when_request_from_blocked_origin() throws Exception {
        mockMvc.perform(options("/api/v1/users/UR1234567890ABCDEF")
                        .header("Origin", BLOCKED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
