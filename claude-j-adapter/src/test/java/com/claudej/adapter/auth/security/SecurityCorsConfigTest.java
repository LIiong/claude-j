package com.claudej.adapter.auth.security;

import com.claudej.adapter.user.web.UserController;
import com.claudej.application.user.service.UserApplicationService;
import com.claudej.domain.auth.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class, SecurityCorsConfigTest.TestCorsConfig.class})
@ActiveProfiles("security-test")
class SecurityCorsConfigTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";
    private static final String BLOCKED_ORIGIN = "http://evil.example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserApplicationService userApplicationService;

    @MockBean
    private TokenService tokenService;

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
    void should_not_allow_when_origin_not_whitelisted() throws Exception {
        mockMvc.perform(options("/api/v1/users/UR1234567890ABCDEF")
                        .header("Origin", BLOCKED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void should_return_401_when_cross_origin_request_without_jwt() throws Exception {
        mockMvc.perform(get("/api/v1/users/UR1234567890ABCDEF")
                        .header("Origin", ALLOWED_ORIGIN)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }

    @Configuration
    static class TestCorsConfig {

        @Bean
        public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
            org.springframework.web.cors.CorsConfiguration configuration =
                    new org.springframework.web.cors.CorsConfiguration();
            configuration.setAllowCredentials(true);
            configuration.addAllowedOrigin(ALLOWED_ORIGIN);
            configuration.addAllowedMethod("GET");
            configuration.addAllowedMethod("POST");
            configuration.addAllowedMethod("OPTIONS");
            configuration.addAllowedHeader("Authorization");
            configuration.addAllowedHeader("Content-Type");
            org.springframework.web.cors.UrlBasedCorsConfigurationSource source =
                    new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", configuration);
            return source;
        }
    }
}
