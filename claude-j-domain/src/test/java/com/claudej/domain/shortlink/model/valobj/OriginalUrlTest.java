package com.claudej.domain.shortlink.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OriginalUrlTest {

    @Test
    void should_createOriginalUrl_when_validHttpsUrl() {
        OriginalUrl url = new OriginalUrl("https://www.example.com/path");
        assertThat(url.getValue()).isEqualTo("https://www.example.com/path");
    }

    @Test
    void should_createOriginalUrl_when_validHttpUrl() {
        OriginalUrl url = new OriginalUrl("http://example.com");
        assertThat(url.getValue()).isEqualTo("http://example.com");
    }

    @Test
    void should_throwException_when_urlIsNull() {
        assertThatThrownBy(() -> new OriginalUrl(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    void should_throwException_when_urlIsBlank() {
        assertThatThrownBy(() -> new OriginalUrl("   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    void should_throwException_when_urlDoesNotStartWithHttp() {
        assertThatThrownBy(() -> new OriginalUrl("ftp://example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("http://或https://");
    }

    @Test
    void should_throwException_when_urlExceeds2048Chars() {
        StringBuilder sb = new StringBuilder("https://example.com/");
        for (int i = 0; i < 2040; i++) {
            sb.append("a");
        }
        assertThatThrownBy(() -> new OriginalUrl(sb.toString()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2048");
    }

    @Test
    void should_beEqual_when_sameValue() {
        OriginalUrl url1 = new OriginalUrl("https://example.com");
        OriginalUrl url2 = new OriginalUrl("https://example.com");
        assertThat(url1).isEqualTo(url2);
        assertThat(url1.hashCode()).isEqualTo(url2.hashCode());
    }
}
