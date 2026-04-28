package com.claudej.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class CorsPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CorsPropertiesTestConfig.class);

    @Test
    void should_bind_dev_origins_when_profile_config_present() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=dev",
                        "app.security.cors.enabled=true",
                        "app.security.cors.allow-credentials=true",
                        "app.security.cors.max-age=1800",
                        "app.security.cors.allowed-origins[0]=http://localhost:3000",
                        "app.security.cors.allowed-origins[1]=http://127.0.0.1:3000",
                        "app.security.cors.allowed-origins[2]=http://localhost:5173",
                        "app.security.cors.allowed-origins[3]=http://127.0.0.1:5173",
                        "app.security.cors.allowed-methods[0]=GET",
                        "app.security.cors.allowed-methods[1]=POST",
                        "app.security.cors.allowed-methods[2]=PUT",
                        "app.security.cors.allowed-methods[3]=DELETE",
                        "app.security.cors.allowed-methods[4]=PATCH",
                        "app.security.cors.allowed-methods[5]=OPTIONS",
                        "app.security.cors.allowed-headers[0]=Authorization",
                        "app.security.cors.allowed-headers[1]=Content-Type",
                        "app.security.cors.allowed-headers[2]=Accept",
                        "app.security.cors.allowed-headers[3]=Origin"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    CorsProperties properties = context.getBean(CorsProperties.class);
                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.getAllowedOrigins())
                            .containsExactly(
                                    "http://localhost:3000",
                                    "http://127.0.0.1:3000",
                                    "http://localhost:5173",
                                    "http://127.0.0.1:5173"
                            );
                    assertThat(properties.getAllowedMethods())
                            .containsExactly("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
                    assertThat(properties.getAllowedHeaders())
                            .containsExactly("Authorization", "Content-Type", "Accept", "Origin");
                    assertThat(properties.getAllowCredentials()).isTrue();
                    assertThat(properties.getMaxAge()).isEqualTo(1800L);
                });
    }

    @Test
    void should_fail_when_allowed_origins_missing_in_enabled_mode() {
        contextRunner
                .withPropertyValues(
                        "app.security.cors.enabled=true",
                        "app.security.cors.allow-credentials=true",
                        "app.security.cors.max-age=1800",
                        "app.security.cors.allowed-methods[0]=GET",
                        "app.security.cors.allowed-methods[1]=OPTIONS",
                        "app.security.cors.allowed-headers[0]=Authorization"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("allowedOrigins must not be empty when cors is enabled");
                });
    }

    @Configuration
    @EnableConfigurationProperties(CorsProperties.class)
    static class CorsPropertiesTestConfig {
    }
}
