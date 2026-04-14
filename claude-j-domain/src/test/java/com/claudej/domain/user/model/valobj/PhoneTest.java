package com.claudej.domain.user.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneTest {

    @Test
    void should_createPhone_when_validValue() {
        // Arrange
        String validPhone = "13800138000";

        // Act
        Phone phone = new Phone(validPhone);

        // Assert
        assertThat(phone.getValue()).isEqualTo(validPhone);
    }

    @Test
    void should_trimValue_when_containsWhitespace() {
        // Arrange
        String valueWithWhitespace = "  13800138000  ";

        // Act
        Phone phone = new Phone(valueWithWhitespace);

        // Assert
        assertThat(phone.getValue()).isEqualTo("13800138000");
    }

    @Test
    void should_throwException_when_valueIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> new Phone(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("手机号不能为空");
    }

    @Test
    void should_throwException_when_valueIsEmpty() {
        // Act & Assert
        assertThatThrownBy(() -> new Phone(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("手机号不能为空");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "12345678901",   // doesn't start with 1
            "1380013800",    // too short
            "138001380000",  // too long
            "11800138000",   // second digit not 3-9
            "1380013800a",   // contains letter
            "138-0013-8000"  // contains hyphen
    })
    void should_throwException_when_invalidFormat(String invalidPhone) {
        // Act & Assert
        assertThatThrownBy(() -> new Phone(invalidPhone))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("手机号格式无效");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "13800138000",
            "13900139000",
            "15000150000",
            "18800188000"
    })
    void should_acceptValidPhones(String validPhone) {
        // Act
        Phone phone = new Phone(validPhone);

        // Assert
        assertThat(phone.getValue()).isEqualTo(validPhone);
    }

    @Test
    void should_beEqual_when_sameValue() {
        // Arrange
        Phone phone1 = new Phone("13800138000");
        Phone phone2 = new Phone("13800138000");

        // Assert
        assertThat(phone1).isEqualTo(phone2);
        assertThat(phone1.hashCode()).isEqualTo(phone2.hashCode());
    }

    @Test
    void should_notBeEqual_when_differentValue() {
        // Arrange
        Phone phone1 = new Phone("13800138000");
        Phone phone2 = new Phone("13900139000");

        // Assert
        assertThat(phone1).isNotEqualTo(phone2);
    }
}
