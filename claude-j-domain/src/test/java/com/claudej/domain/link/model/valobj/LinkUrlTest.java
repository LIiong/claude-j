package com.claudej.domain.link.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LinkUrlTest {

    @Test
    void should_createLinkUrl_when_validHttpUrlProvided() {
        // When
        LinkUrl linkUrl = new LinkUrl("https://example.com");

        // Then
        assertThat(linkUrl.getValue()).isEqualTo("https://example.com");
    }

    @Test
    void should_createLinkUrl_when_validFtpUrlProvided() {
        // When
        LinkUrl linkUrl = new LinkUrl("ftp://files.example.com");

        // Then
        assertThat(linkUrl.getValue()).isEqualTo("ftp://files.example.com");
    }

    @Test
    void should_trimWhitespace_when_valueHasLeadingOrTrailingSpaces() {
        // When
        LinkUrl linkUrl = new LinkUrl("  https://example.com  ");

        // Then
        assertThat(linkUrl.getValue()).isEqualTo("https://example.com");
    }

    @Test
    void should_throwException_when_valueIsNull() {
        // When & Then
        assertThatThrownBy(() -> new LinkUrl(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("链接地址不能为空");
    }

    @Test
    void should_throwException_when_valueIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> new LinkUrl(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("链接地址不能为空");
    }

    @Test
    void should_throwException_when_urlIsInvalid() {
        // When & Then
        assertThatThrownBy(() -> new LinkUrl("not-a-valid-url"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("链接地址格式无效");
    }

    @Test
    void should_throwException_when_urlExceedsMaxLength() {
        // Given
        String longUrl = "https://example.com/" + "a".repeat(500);

        // When & Then
        assertThatThrownBy(() -> new LinkUrl(longUrl))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("链接地址长度不能超过500");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://example.com",
            "http://localhost:8080",
            "https://example.com/path?query=value",
            "http://192.168.1.1:8080/api"
    })
    void should_acceptValidUrls(String url) {
        // When
        LinkUrl linkUrl = new LinkUrl(url);

        // Then
        assertThat(linkUrl.getValue()).isEqualTo(url);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "just-text",
            "//missing-protocol.com",
            "ftp:",
            "http://"
    })
    void should_rejectInvalidUrls(String url) {
        // When & Then
        assertThatThrownBy(() -> new LinkUrl(url))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("链接地址格式无效");
    }

    @Test
    void should_beEqual_when_sameValue() {
        // Given
        LinkUrl url1 = new LinkUrl("https://example.com");
        LinkUrl url2 = new LinkUrl("https://example.com");

        // Then
        assertThat(url1).isEqualTo(url2);
        assertThat(url1.hashCode()).isEqualTo(url2.hashCode());
    }
}
