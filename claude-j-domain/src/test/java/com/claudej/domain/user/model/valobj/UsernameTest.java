package com.claudej.domain.user.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsernameTest {

    @Test
    void should_createUsername_when_validValue() {
        // Arrange
        String validUsername = "testuser";

        // Act
        Username username = new Username(validUsername);

        // Assert
        assertThat(username.getValue()).isEqualTo(validUsername);
    }

    @Test
    void should_trimValue_when_containsWhitespace() {
        // Arrange
        String valueWithWhitespace = "  testuser  ";

        // Act
        Username username = new Username(valueWithWhitespace);

        // Assert
        assertThat(username.getValue()).isEqualTo("testuser");
    }

    @Test
    void should_throwException_when_valueIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> new Username(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名不能为空");
    }

    @Test
    void should_throwException_when_valueIsEmpty() {
        // Act & Assert
        assertThatThrownBy(() -> new Username(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名不能为空");
    }

    @Test
    void should_throwException_when_valueIsBlank() {
        // Act & Assert
        assertThatThrownBy(() -> new Username("   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名不能为空");
    }

    @Test
    void should_throwException_when_valueTooShort() {
        // Act & Assert
        assertThatThrownBy(() -> new Username("a"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名长度必须在2-20字符之间");
    }

    @Test
    void should_throwException_when_valueTooLong() {
        // Act & Assert
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 21; i++) {
            sb.append("a");
        }
        assertThatThrownBy(() -> new Username(sb.toString()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名长度必须在2-20字符之间");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "username", "aaaaaaaaaaaaaaaaaaaa"})
    void should_acceptValidLengthUsernames(String value) {
        // Act
        Username username = new Username(value);

        // Assert
        assertThat(username.getValue()).isEqualTo(value);
    }

    @Test
    void should_beEqual_when_sameValue() {
        // Arrange
        Username username1 = new Username("testuser");
        Username username2 = new Username("testuser");

        // Assert
        assertThat(username1).isEqualTo(username2);
        assertThat(username1.hashCode()).isEqualTo(username2.hashCode());
    }

    @Test
    void should_notBeEqual_when_differentValue() {
        // Arrange
        Username username1 = new Username("user1");
        Username username2 = new Username("user2");

        // Assert
        assertThat(username1).isNotEqualTo(username2);
    }
}
