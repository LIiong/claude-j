package com.claudej.domain.link.model.aggregate;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.link.model.valobj.LinkCategory;
import com.claudej.domain.link.model.valobj.LinkName;
import com.claudej.domain.link.model.valobj.LinkUrl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LinkTest {

    @Test
    void should_createLink_when_validInputProvided() {
        // Given
        LinkName name = new LinkName("Test Link");
        LinkUrl url = new LinkUrl("https://example.com");
        LinkCategory category = new LinkCategory("tech");

        // When
        Link link = Link.create(name, url, "Description", category);

        // Then
        assertThat(link).isNotNull();
        assertThat(link.getNameValue()).isEqualTo("Test Link");
        assertThat(link.getUrlValue()).isEqualTo("https://example.com");
        assertThat(link.getDescription()).isEqualTo("Description");
        assertThat(link.getCategoryValue()).isEqualTo("tech");
        assertThat(link.getCreateTime()).isNotNull();
        assertThat(link.getUpdateTime()).isNotNull();
    }

    @Test
    void should_throwException_when_creatingLinkWithNullName() {
        // When & Then
        assertThatThrownBy(() -> Link.create(null, new LinkUrl("https://example.com"), null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("链接名称不能为空");
    }

    @Test
    void should_throwException_when_creatingLinkWithNullUrl() {
        // When & Then
        assertThatThrownBy(() -> Link.create(new LinkName("Test"), null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("链接地址不能为空");
    }

    @Test
    void should_updateLink_when_newValuesProvided() {
        // Given
        Link link = Link.create(
                new LinkName("Old Name"),
                new LinkUrl("https://old.com"),
                "Old Desc",
                new LinkCategory("old")
        );

        // When
        link.update(
                new LinkName("New Name"),
                new LinkUrl("https://new.com"),
                "New Desc",
                new LinkCategory("new")
        );

        // Then
        assertThat(link.getNameValue()).isEqualTo("New Name");
        assertThat(link.getUrlValue()).isEqualTo("https://new.com");
        assertThat(link.getDescription()).isEqualTo("New Desc");
        assertThat(link.getCategoryValue()).isEqualTo("new");
    }

    @Test
    void should_partialUpdateLink_when_partialValuesProvided() {
        // Given
        Link link = Link.create(
                new LinkName("Original Name"),
                new LinkUrl("https://example.com"),
                "Original Desc",
                new LinkCategory("tech")
        );

        // When - only update name
        link.update(new LinkName("New Name"), null, null, null);

        // Then
        assertThat(link.getNameValue()).isEqualTo("New Name");
        assertThat(link.getUrlValue()).isEqualTo("https://example.com");
        assertThat(link.getDescription()).isEqualTo("Original Desc");
        assertThat(link.getCategoryValue()).isEqualTo("tech");
    }

    @Test
    void should_reconstructLinkFromPersistence() {
        // Given
        java.time.LocalDateTime createTime = java.time.LocalDateTime.now();
        java.time.LocalDateTime updateTime = java.time.LocalDateTime.now();

        // When
        Link link = Link.reconstruct(
                1L,
                new LinkName("Test"),
                new LinkUrl("https://example.com"),
                "Desc",
                new LinkCategory("tech"),
                createTime,
                updateTime
        );

        // Then
        assertThat(link.getId()).isEqualTo(1L);
        assertThat(link.getNameValue()).isEqualTo("Test");
        assertThat(link.getUrlValue()).isEqualTo("https://example.com");
        assertThat(link.getCreateTime()).isEqualTo(createTime);
        assertThat(link.getUpdateTime()).isEqualTo(updateTime);
    }

    @Test
    void should_setId_when_persisted() {
        // Given
        Link link = Link.create(
                new LinkName("Test"),
                new LinkUrl("https://example.com"),
                null,
                null
        );

        // When
        link.setId(100L);

        // Then
        assertThat(link.getId()).isEqualTo(100L);
    }
}
