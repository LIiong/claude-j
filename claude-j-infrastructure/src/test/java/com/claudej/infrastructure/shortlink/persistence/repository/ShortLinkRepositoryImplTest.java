package com.claudej.infrastructure.shortlink.persistence.repository;

import com.claudej.domain.shortlink.model.aggregate.ShortLink;
import com.claudej.domain.shortlink.model.valobj.OriginalUrl;
import com.claudej.domain.shortlink.model.valobj.ShortCode;
import com.claudej.infrastructure.shortlink.persistence.converter.ShortLinkConverter;
import com.claudej.infrastructure.shortlink.persistence.mapper.ShortLinkMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:shortlink_repo_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@Transactional
class ShortLinkRepositoryImplTest {

    @SpringBootApplication(scanBasePackageClasses = {
            ShortLinkRepositoryImpl.class,
            ShortLinkConverter.class,
            ShortLinkMapper.class
    })
    @org.mybatis.spring.annotation.MapperScan(basePackageClasses = ShortLinkMapper.class)
    static class TestConfig {
    }

    @Autowired
    private ShortLinkRepositoryImpl shortLinkRepository;

    @Test
    void should_saveAndRetrieveByShortCode() {
        // Arrange
        ShortLink shortLink = ShortLink.create(new OriginalUrl("https://www.example.com/test1"));
        shortLink = shortLinkRepository.save(shortLink);

        assertThat(shortLink.getId()).isNotNull();

        // Assign short code and save again
        shortLink.assignShortCode(new ShortCode("test01"));
        shortLinkRepository.save(shortLink);

        // Act
        Optional<ShortLink> found = shortLinkRepository.findByShortCode(new ShortCode("test01"));

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getOriginalUrlValue()).isEqualTo("https://www.example.com/test1");
        assertThat(found.get().getShortCode().getValue()).isEqualTo("test01");
    }

    @Test
    void should_findByOriginalUrl_when_hashMatches() {
        // Arrange
        ShortLink shortLink = ShortLink.create(new OriginalUrl("https://www.example.com/test2"));
        shortLink = shortLinkRepository.save(shortLink);
        shortLink.assignShortCode(new ShortCode("test02"));
        shortLinkRepository.save(shortLink);

        // Act
        Optional<ShortLink> found = shortLinkRepository.findByOriginalUrl(
                new OriginalUrl("https://www.example.com/test2"));

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getShortCode().getValue()).isEqualTo("test02");
    }

    @Test
    void should_returnEmpty_when_shortCodeNotFound() {
        Optional<ShortLink> found = shortLinkRepository.findByShortCode(new ShortCode("zzzzzz"));
        assertThat(found).isEmpty();
    }

    @Test
    void should_returnEmpty_when_originalUrlNotFound() {
        Optional<ShortLink> found = shortLinkRepository.findByOriginalUrl(
                new OriginalUrl("https://nonexistent.example.com"));
        assertThat(found).isEmpty();
    }
}
