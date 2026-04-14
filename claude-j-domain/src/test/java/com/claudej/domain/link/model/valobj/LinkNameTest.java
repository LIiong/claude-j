package com.claudej.domain.link.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LinkNameTest {

    @Test
    void should_createLinkName_when_validValueProvided() {
        // When
        LinkName linkName = new LinkName("Test Link");

        // Then
        assertThat(linkName.getValue()).isEqualTo("Test Link");
    }

    @Test
    void should_trimWhitespace_when_valueHasLeadingOrTrailingSpaces() {
        // When
        LinkName linkName = new LinkName("  Test Link  ");

        // Then
        assertThat(linkName.getValue()).isEqualTo("Test Link");
    }

    @Test
    void should_throwException_when_valueIsNull() {
        // When & Then
        assertThatThrownBy(() -> new LinkName(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("链接名称不能为空");
    }

    @Test
    void should_throwException_when_valueIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> new LinkName(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("链接名称不能为空");
    }

    @Test
    void should_throwException_when_valueIsBlank() {
        // When & Then
        assertThatThrownBy(() -> new LinkName("   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("链接名称不能为空");
    }

    @Test
    void should_throwException_when_valueExceedsMaxLength() {
        // Given
        String longName = "a".repeat(101);

        // When & Then
        assertThatThrownBy(() -> new LinkName(longName))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("链接名称长度不能超过100");
    }

    @Test
    void should_createLinkName_when_valueAtMaxLength() {
        // Given
        String maxLengthName = "a".repeat(100);

        // When
        LinkName linkName = new LinkName(maxLengthName);

        // Then
        assertThat(linkName.getValue()).hasSize(100);
    }

    @Test
    void should_beEqual_when_sameValue() {
        // Given
        LinkName name1 = new LinkName("Test");
        LinkName name2 = new LinkName("Test");

        // Then
        assertThat(name1).isEqualTo(name2);
        assertThat(name1.hashCode()).isEqualTo(name2.hashCode());
    }

    @Test
    void should_notBeEqual_when_differentValue() {
        // Given
        LinkName name1 = new LinkName("Test1");
        LinkName name2 = new LinkName("Test2");

        // Then
        assertThat(name1).isNotEqualTo(name2);
    }
}
