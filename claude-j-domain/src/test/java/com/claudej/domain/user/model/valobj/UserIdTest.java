package com.claudej.domain.user.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdTest {

    @Test
    void should_createUserId_when_validValue() {
        // Arrange
        String validId = "UR1234567890ABCDEF";

        // Act
        UserId userId = new UserId(validId);

        // Assert
        assertThat(userId.getValue()).isEqualTo(validId);
    }

    @Test
    void should_trimValue_when_containsWhitespace() {
        // Arrange
        String valueWithWhitespace = "  UR1234567890ABCDEF  ";

        // Act
        UserId userId = new UserId(valueWithWhitespace);

        // Assert
        assertThat(userId.getValue()).isEqualTo("UR1234567890ABCDEF");
    }

    @Test
    void should_throwException_when_valueIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> new UserId(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户ID不能为空");
    }

    @Test
    void should_throwException_when_valueIsEmpty() {
        // Act & Assert
        assertThatThrownBy(() -> new UserId(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户ID不能为空");
    }

    @Test
    void should_throwException_when_valueDoesNotStartWithUR() {
        // Act & Assert
        assertThatThrownBy(() -> new UserId("AB1234567890ABCDEF"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户ID格式无效");
    }

    @Test
    void should_throwException_when_valueTooShort() {
        // Act & Assert
        assertThatThrownBy(() -> new UserId("UR12345"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户ID格式无效");
    }

    @Test
    void should_throwException_when_valueTooLong() {
        // Act & Assert
        assertThatThrownBy(() -> new UserId("UR1234567890ABCDEFG"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户ID格式无效");
    }

    @Test
    void should_throwException_when_valueContainsInvalidChars() {
        // Act & Assert
        assertThatThrownBy(() -> new UserId("UR1234567890ABCDEO")) // O is not allowed
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户ID格式无效");
    }

    @Test
    void should_generateValidUserId() {
        // Act
        UserId userId = UserId.generate();

        // Assert
        assertThat(userId.getValue()).startsWith("UR");
        assertThat(userId.getValue()).hasSize(18); // UR + 16 chars
        assertThat(userId.getValue().substring(2)).matches("^[0-9A-Z]+$");
    }

    @Test
    void should_beEqual_when_sameValue() {
        // Arrange
        String value = "UR1234567890ABCDEF";
        UserId userId1 = new UserId(value);
        UserId userId2 = new UserId(value);

        // Assert
        assertThat(userId1).isEqualTo(userId2);
        assertThat(userId1.hashCode()).isEqualTo(userId2.hashCode());
    }

    @Test
    void should_notBeEqual_when_differentValue() {
        // Arrange
        UserId userId1 = new UserId("UR1234567890ABCDEF");
        UserId userId2 = new UserId("UR1234567890ABCDEG");

        // Assert
        assertThat(userId1).isNotEqualTo(userId2);
    }
}
