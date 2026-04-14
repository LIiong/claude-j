package com.claudej.infrastructure.link.persistence.repository;

import com.claudej.domain.link.model.aggregate.Link;
import com.claudej.domain.link.model.valobj.LinkCategory;
import com.claudej.domain.link.model.valobj.LinkName;
import com.claudej.domain.link.model.valobj.LinkUrl;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LinkRepositoryImplTest {

    @SpringBootApplication(scanBasePackages = "com.claudej.infrastructure")
    @MapperScan("com.claudej.infrastructure.**.mapper")
    static class TestConfig {
    }

    @Autowired
    private LinkRepositoryImpl linkRepository;

    @Test
    void should_saveNewLink_when_linkHasNoId() {
        // Given
        Link link = Link.create(
                new LinkName("Test Link"),
                new LinkUrl("https://example.com"),
                "Description",
                new LinkCategory("tech")
        );

        // When
        Link savedLink = linkRepository.save(link);

        // Then
        assertThat(savedLink.getId()).isNotNull();
        assertThat(savedLink.getNameValue()).isEqualTo("Test Link");
    }

    @Test
    void should_updateLink_when_linkHasId() {
        // Given
        Link link = Link.create(
                new LinkName("Original Name"),
                new LinkUrl("https://original.com"),
                "Original Desc",
                new LinkCategory("tech")
        );
        Link saved = linkRepository.save(link);

        // When
        Link toUpdate = Link.reconstruct(
                saved.getId(),
                new LinkName("Updated Name"),
                new LinkUrl("https://updated.com"),
                "Updated Desc",
                new LinkCategory("tech"),
                saved.getCreateTime(),
                saved.getUpdateTime()
        );
        Link updated = linkRepository.save(toUpdate);

        // Then
        assertThat(updated.getNameValue()).isEqualTo("Updated Name");
        assertThat(updated.getUrlValue()).isEqualTo("https://updated.com");
    }

    @Test
    void should_findLinkById_when_linkExists() {
        // Given
        Link link = Link.create(
                new LinkName("Test Link"),
                new LinkUrl("https://example.com"),
                null,
                null
        );
        Link saved = linkRepository.save(link);

        // When
        Optional<Link> found = linkRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getNameValue()).isEqualTo("Test Link");
        assertThat(found.get().getUrlValue()).isEqualTo("https://example.com");
    }

    @Test
    void should_returnEmpty_when_linkNotFound() {
        // When
        Optional<Link> found = linkRepository.findById(9999L);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void should_returnEmpty_when_linkIsSoftDeleted() {
        // Given
        Link link = Link.create(
                new LinkName("To Delete"),
                new LinkUrl("https://delete.com"),
                null,
                null
        );
        Link saved = linkRepository.save(link);

        // When
        linkRepository.deleteById(saved.getId());
        Optional<Link> found = linkRepository.findById(saved.getId());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void should_softDeleteLink_when_deleteById() {
        // Given
        Link link = Link.create(
                new LinkName("To Delete"),
                new LinkUrl("https://delete.com"),
                null,
                null
        );
        Link saved = linkRepository.save(link);

        // When
        linkRepository.deleteById(saved.getId());

        // Then
        assertThat(linkRepository.existsById(saved.getId())).isFalse();
    }

    @Test
    void should_returnAllActiveLinks_when_findAll() {
        // Given
        Link link1 = Link.create(
                new LinkName("Link 1"),
                new LinkUrl("https://link1.com"),
                null,
                new LinkCategory("tech")
        );
        Link link2 = Link.create(
                new LinkName("Link 2"),
                new LinkUrl("https://link2.com"),
                null,
                new LinkCategory("tech")
        );
        linkRepository.save(link1);
        linkRepository.save(link2);

        // When
        List<Link> allLinks = linkRepository.findAll();

        // Then
        assertThat(allLinks).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void should_findLinksByCategory_when_categoryMatches() {
        // Given
        Link link = Link.create(
                new LinkName("Tech Link"),
                new LinkUrl("https://tech.com"),
                null,
                new LinkCategory("technology")
        );
        linkRepository.save(link);

        // When
        List<Link> links = linkRepository.findByCategory(new LinkCategory("technology"));

        // Then
        assertThat(links).isNotEmpty();
        assertThat(links.get(0).getCategoryValue()).isEqualTo("technology");
    }

    @Test
    void should_returnTrue_when_linkExists() {
        // Given
        Link link = Link.create(
                new LinkName("Test"),
                new LinkUrl("https://test.com"),
                null,
                null
        );
        Link saved = linkRepository.save(link);

        // When & Then
        assertThat(linkRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    void should_returnFalse_when_linkDoesNotExist() {
        // When & Then
        assertThat(linkRepository.existsById(9999L)).isFalse();
    }
}
