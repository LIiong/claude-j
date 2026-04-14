package com.claudej.domain.user.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InviteCodeTest {

    @Test
    void should_createInviteCode_when_validValue() {
        // Arrange
        String validCode = "ABC234";

        // Act
        InviteCode inviteCode = new InviteCode(validCode);

        // Assert
        assertThat(inviteCode.getValue()).isEqualTo("ABC234");
    }

    @Test
    void should_convertToUppercase_when_lowercaseValue() {
        // Arrange
        String lowercaseCode = "abc234";

        // Act
        InviteCode inviteCode = new InviteCode(lowercaseCode);

        // Assert
        assertThat(inviteCode.getValue()).isEqualTo("ABC234");
    }

    @Test
    void should_trimValue_when_containsWhitespace() {
        // Arrange
        String valueWithWhitespace = "  ABC234  ";

        // Act
        InviteCode inviteCode = new InviteCode(valueWithWhitespace);

        // Assert
        assertThat(inviteCode.getValue()).isEqualTo("ABC234");
    }

    @Test
    void should_throwException_when_valueIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> new InviteCode(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邀请码不能为空");
    }

    @Test
    void should_throwException_when_valueIsEmpty() {
        // Act & Assert
        assertThatThrownBy(() -> new InviteCode(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邀请码不能为空");
    }

    @Test
    void should_throwException_when_valueTooShort() {
        // Act & Assert
        assertThatThrownBy(() -> new InviteCode("ABC23"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邀请码必须为6位字母数字组合");
    }

    @Test
    void should_throwException_when_valueTooLong() {
        // Act & Assert
        assertThatThrownBy(() -> new InviteCode("ABC2345"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邀请码必须为6位字母数字组合");
    }

    @Test
    void should_throwException_when_valueContainsZero() {
        // Act & Assert
        assertThatThrownBy(() -> new InviteCode("ABC230"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邀请码必须为6位字母数字组合");
    }

    @Test
    void should_throwException_when_valueContainsLetterO() {
        // Act & Assert
        assertThatThrownBy(() -> new InviteCode("ABC23O"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邀请码必须为6位字母数字组合");
    }

    @Test
    void should_throwException_when_valueContainsOne() {
        // Act & Assert
        assertThatThrownBy(() -> new InviteCode("ABC231"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邀请码必须为6位字母数字组合");
    }

    @Test
    void should_generateValidInviteCode() {
        // Act
        InviteCode inviteCode = InviteCode.generate();

        // Assert
        assertThat(inviteCode.getValue()).hasSize(6);
        assertThat(inviteCode.getValue()).matches("^[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]+$");
    }

    @Test
    void should_beEqual_when_sameValue() {
        // Arrange
        InviteCode code1 = new InviteCode("ABC234");
        InviteCode code2 = new InviteCode("ABC234");

        // Assert
        assertThat(code1).isEqualTo(code2);
        assertThat(code1.hashCode()).isEqualTo(code2.hashCode());
    }

    @Test
    void should_beEqual_when_sameValueDifferentCase() {
        // Arrange
        InviteCode code1 = new InviteCode("ABC234");
        InviteCode code2 = new InviteCode("abc234");

        // Assert
        assertThat(code1).isEqualTo(code2);
    }

    @Test
    void should_notBeEqual_when_differentValue() {
        // Arrange
        InviteCode code1 = new InviteCode("ABC234");
        InviteCode code2 = new InviteCode("DEF567");

        // Assert
        assertThat(code1).isNotEqualTo(code2);
    }
}
