package com.claudej.domain.shortlink.model.aggregate;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.shortlink.model.valobj.OriginalUrl;
import com.claudej.domain.shortlink.model.valobj.ShortCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShortLinkTest {

    @Test
    void should_createShortLink_when_validOriginalUrl() {
        OriginalUrl url = new OriginalUrl("https://www.example.com");
        ShortLink shortLink = ShortLink.create(url);

        assertThat(shortLink.getOriginalUrl()).isEqualTo(url);
        assertThat(shortLink.getShortCode()).isNull();
        assertThat(shortLink.getId()).isNull();
        assertThat(shortLink.getCreateTime()).isNotNull();
    }

    @Test
    void should_assignShortCode_when_notAlreadyAssigned() {
        ShortLink shortLink = ShortLink.create(new OriginalUrl("https://example.com"));
        ShortCode code = new ShortCode("abc123");

        shortLink.assignShortCode(code);

        assertThat(shortLink.getShortCode()).isEqualTo(code);
    }

    @Test
    void should_throwException_when_assigningCodeTwice() {
        ShortLink shortLink = ShortLink.create(new OriginalUrl("https://example.com"));
        shortLink.assignShortCode(new ShortCode("abc123"));

        assertThatThrownBy(() -> shortLink.assignShortCode(new ShortCode("xyz789")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_reportNotExpired_when_expireTimeIsNull() {
        ShortLink shortLink = ShortLink.create(new OriginalUrl("https://example.com"));
        assertThat(shortLink.isExpired()).isFalse();
    }

    @Test
    void should_reportExpired_when_expireTimeInPast() {
        ShortLink shortLink = ShortLink.reconstruct(
                1L,
                new ShortCode("abc123"),
                new OriginalUrl("https://example.com"),
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1)
        );
        assertThat(shortLink.isExpired()).isTrue();
    }

    @Test
    void should_reportNotExpired_when_expireTimeInFuture() {
        ShortLink shortLink = ShortLink.reconstruct(
                1L,
                new ShortCode("abc123"),
                new OriginalUrl("https://example.com"),
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1)
        );
        assertThat(shortLink.isExpired()).isFalse();
    }

    @Test
    void should_getOriginalUrlValue_when_called() {
        ShortLink shortLink = ShortLink.create(new OriginalUrl("https://example.com/path"));
        assertThat(shortLink.getOriginalUrlValue()).isEqualTo("https://example.com/path");
    }

    @Test
    void should_reconstructShortLink_when_allFieldsProvided() {
        ShortLink shortLink = ShortLink.reconstruct(
                100L,
                new ShortCode("abc123"),
                new OriginalUrl("https://example.com"),
                LocalDateTime.of(2024, 1, 1, 0, 0),
                null
        );

        assertThat(shortLink.getId()).isEqualTo(100L);
        assertThat(shortLink.getShortCode().getValue()).isEqualTo("abc123");
        assertThat(shortLink.getOriginalUrlValue()).isEqualTo("https://example.com");
    }
}
