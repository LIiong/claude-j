package com.claudej.domain.shortlink.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShortCodeTest {

    @Test
    void should_createShortCode_when_valid6CharAlphanumeric() {
        ShortCode code = new ShortCode("a1B2c3");
        assertThat(code.getValue()).isEqualTo("a1B2c3");
    }

    @Test
    void should_throwException_when_codeIsNull() {
        assertThatThrownBy(() -> new ShortCode(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    void should_throwException_when_codeIsEmpty() {
        assertThatThrownBy(() -> new ShortCode(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    void should_throwException_when_codeLengthIsNot6() {
        assertThatThrownBy(() -> new ShortCode("abc"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("6位");
    }

    @Test
    void should_throwException_when_codeContainsInvalidChars() {
        assertThatThrownBy(() -> new ShortCode("ab-c!3"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Base62");
    }

    @Test
    void should_beEqual_when_sameValue() {
        ShortCode code1 = new ShortCode("abc123");
        ShortCode code2 = new ShortCode("abc123");
        assertThat(code1).isEqualTo(code2);
        assertThat(code1.hashCode()).isEqualTo(code2.hashCode());
    }

    @Test
    void should_notBeEqual_when_differentValue() {
        ShortCode code1 = new ShortCode("abc123");
        ShortCode code2 = new ShortCode("xyz789");
        assertThat(code1).isNotEqualTo(code2);
    }
}
