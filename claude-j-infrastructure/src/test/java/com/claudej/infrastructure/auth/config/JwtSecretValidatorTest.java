package com.claudej.infrastructure.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * JwtSecretValidator 单元测试
 */
class JwtSecretValidatorTest {

    private JwtSecretValidator validator = new JwtSecretValidator();

    private void setSecret(String secret) throws Exception {
        Field field = JwtSecretValidator.class.getDeclaredField("jwtSecret");
        field.setAccessible(true);
        field.set(validator, secret);
    }

    @Test
    void should_throw_when_secret_is_null() throws Exception {
        // Arrange
        setSecret(null);
        DefaultApplicationArguments args = new DefaultApplicationArguments();

        // Act & Assert
        assertThatThrownBy(() -> validator.run(args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET environment variable is required");
    }

    @Test
    void should_throw_when_secret_is_empty() throws Exception {
        // Arrange
        setSecret("");
        DefaultApplicationArguments args = new DefaultApplicationArguments();

        // Act & Assert
        assertThatThrownBy(() -> validator.run(args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET environment variable is required");
    }

    @Test
    void should_throw_when_secret_length_less_than_32() throws Exception {
        // Arrange
        setSecret("short-secret-16");
        DefaultApplicationArguments args = new DefaultApplicationArguments();

        // Act & Assert
        assertThatThrownBy(() -> validator.run(args))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET must be at least 32 characters");
    }

    @Test
    void should_pass_when_secret_is_valid() throws Exception {
        // Arrange
        setSecret("valid-secret-key-at-least-32-bytes-long-ok");
        DefaultApplicationArguments args = new DefaultApplicationArguments();

        // Act & Assert
        assertThatNoException().isThrownBy(() -> validator.run(args));
    }
}
