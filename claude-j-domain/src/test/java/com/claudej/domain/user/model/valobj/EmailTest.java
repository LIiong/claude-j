package com.claudej.domain.user.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void should_createEmail_when_validValue() {
        // Arrange
        String validEmail = "user@example.com";

        // Act
        Email email = new Email(validEmail);

        // Assert
        assertThat(email.getValue()).isEqualTo(validEmail);
    }

    @Test
    void should_trimValue_when_containsWhitespace() {
        // Arrange
        String valueWithWhitespace = "  user@example.com  ";

        // Act
        Email email = new Email(valueWithWhitespace);

        // Assert
        assertThat(email.getValue()).isEqualTo("user@example.com");
    }

    @Test
    void should_throwException_when_valueIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮箱不能为空");
    }

    @Test
    void should_throwException_when_valueIsEmpty() {
        // Act & Assert
        assertThatThrownBy(() -> new Email(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮箱不能为空");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid",
            "@example.com",
            "user@",
            "user@@example.com",
            "user@.com",
            "user@example.",
            "user name@example.com"
    })
    void should_throwException_when_invalidFormat(String invalidEmail) {
        // Act & Assert
        assertThatThrownBy(() -> new Email(invalidEmail))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮箱格式无效");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "user@example.com",
            "user.name@example.com",
            "user+tag@example.com",
            "user@sub.example.com",
            "user123@example.co.uk"
    })
    void should_acceptValidEmails(String validEmail) {
        // Act
        Email email = new Email(validEmail);

        // Assert
        assertThat(email.getValue()).isEqualTo(validEmail);
    }

    @Test
    void should_beEqual_when_sameValue() {
        // Arrange
        Email email1 = new Email("user@example.com");
        Email email2 = new Email("user@example.com");

        // Assert
        assertThat(email1).isEqualTo(email2);
        assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
    }

    @Test
    void should_notBeEqual_when_differentValue() {
        // Arrange
        Email email1 = new Email("user1@example.com");
        Email email2 = new Email("user2@example.com");

        // Assert
        assertThat(email1).isNotEqualTo(email2);
    }
}
