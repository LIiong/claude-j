package com.claudej.domain.common.model.valobj;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class PageTest {

    @Test
    void should_createPage_when_validParamsProvided() {
        // Given
        String item1 = "Item1";
        String item2 = "Item2";

        // When
        Page<String> page = new Page<String>(
                Arrays.asList(item1, item2),
                100L,
                0,
                20
        );

        // Then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(100L);
        assertThat(page.getPage()).isEqualTo(0);
        assertThat(page.getSize()).isEqualTo(20);
    }

    @Test
    void should_calculateTotalPages_correctly() {
        // Given - 100 total elements, 20 per page = 5 pages
        Page<String> page = new Page<String>(
                Arrays.asList("item1", "item2"),
                100L,
                0,
                20
        );

        // Then
        assertThat(page.getTotalPages()).isEqualTo(5);
    }

    @Test
    void should_calculateTotalPages_withRemainder() {
        // Given - 105 total elements, 20 per page = 6 pages
        Page<String> page = new Page<String>(
                Arrays.asList("item1", "item2"),
                105L,
                0,
                20
        );

        // Then
        assertThat(page.getTotalPages()).isEqualTo(6);
    }

    @Test
    void should_haveOneTotalPage_when_totalLessThanSize() {
        // Given - 15 total elements, 20 per page = 1 page
        Page<String> page = new Page<String>(
                Arrays.asList("item1", "item2"),
                15L,
                0,
                20
        );

        // Then
        assertThat(page.getTotalPages()).isEqualTo(1);
    }

    @Test
    void should_haveZeroTotalPages_when_totalIsZero() {
        // Given - 0 total elements
        Page<String> page = new Page<String>(
                Collections.emptyList(),
                0L,
                0,
                20
        );

        // Then
        assertThat(page.getTotalPages()).isEqualTo(0);
    }

    @Test
    void should_identifyFirstPage_when_pageIsZero() {
        // Given
        Page<String> page = new Page<String>(
                Arrays.asList("item1"),
                100L,
                0,
                20
        );

        // Then
        assertThat(page.isFirst()).isTrue();
    }

    @Test
    void should_notBeFirstPage_when_pageIsNotZero() {
        // Given
        Page<String> page = new Page<String>(
                Arrays.asList("item1"),
                100L,
                1,
                20
        );

        // Then
        assertThat(page.isFirst()).isFalse();
    }

    @Test
    void should_identifyLastPage_when_pageIsTotalPagesMinusOne() {
        // Given - 100 total, 20 per page = 5 pages, page 4 is last
        Page<String> page = new Page<String>(
                Arrays.asList("item1"),
                100L,
                4,
                20
        );

        // Then
        assertThat(page.isLast()).isTrue();
    }

    @Test
    void should_notBeLastPage_when_pageIsNotLast() {
        // Given
        Page<String> page = new Page<String>(
                Arrays.asList("item1"),
                100L,
                0,
                20
        );

        // Then
        assertThat(page.isLast()).isFalse();
    }

    @Test
    void should_beLastPage_when_totalPagesIsOne() {
        // Given - single page
        Page<String> page = new Page<String>(
                Arrays.asList("item1"),
                15L,
                0,
                20
        );

        // Then
        assertThat(page.isLast()).isTrue();
    }

    @Test
    void should_beEmpty_when_contentIsEmpty() {
        // Given
        Page<String> page = new Page<String>(
                Collections.emptyList(),
                0L,
                0,
                20
        );

        // Then
        assertThat(page.isEmpty()).isTrue();
    }

    @Test
    void should_notBeEmpty_when_contentHasItems() {
        // Given
        Page<String> page = new Page<String>(
                Arrays.asList("item1"),
                100L,
                0,
                20
        );

        // Then
        assertThat(page.isEmpty()).isFalse();
    }

    @Test
    void should_haveNext_when_notLastPage() {
        // Given
        Page<String> page = new Page<String>(
                Arrays.asList("item1"),
                100L,
                0,
                20
        );

        // Then
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void should_notHaveNext_when_lastPage() {
        // Given
        Page<String> page = new Page<String>(
                Arrays.asList("item1"),
                100L,
                4,
                20
        );

        // Then
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void should_havePrevious_when_notFirstPage() {
        // Given
        Page<String> page = new Page<String>(
                Arrays.asList("item1"),
                100L,
                1,
                20
        );

        // Then
        assertThat(page.hasPrevious()).isTrue();
    }

    @Test
    void should_notHavePrevious_when_firstPage() {
        // Given
        Page<String> page = new Page<String>(
                Arrays.asList("item1"),
                100L,
                0,
                20
        );

        // Then
        assertThat(page.hasPrevious()).isFalse();
    }

    @Test
    void should_createEmptyPage() {
        // When
        Page<String> page = Page.empty(0, 20);

        // Then
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(0L);
        assertThat(page.getTotalPages()).isEqualTo(0);
        assertThat(page.getPage()).isEqualTo(0);
        assertThat(page.getSize()).isEqualTo(20);
        assertThat(page.isEmpty()).isTrue();
        assertThat(page.isFirst()).isTrue();
        assertThat(page.isLast()).isTrue();
    }

    @Test
    void should_beEqual_when_sameValues() {
        // Given
        Page<String> page1 = new Page<String>(
                Arrays.asList("item1", "item2"),
                100L,
                0,
                20
        );
        Page<String> page2 = new Page<String>(
                Arrays.asList("item1", "item2"),
                100L,
                0,
                20
        );

        // Then
        assertThat(page1).isEqualTo(page2);
        assertThat(page1.hashCode()).isEqualTo(page2.hashCode());
    }

    @Test
    void should_notBeEqual_when_differentContent() {
        // Given
        Page<String> page1 = new Page<String>(
                Arrays.asList("item1"),
                100L,
                0,
                20
        );
        Page<String> page2 = new Page<String>(
                Arrays.asList("item2"),
                100L,
                0,
                20
        );

        // Then
        assertThat(page1).isNotEqualTo(page2);
    }

    @Test
    void should_notBeEqual_when_differentTotalElements() {
        // Given
        Page<String> page1 = new Page<String>(
                Arrays.asList("item1"),
                100L,
                0,
                20
        );
        Page<String> page2 = new Page<String>(
                Arrays.asList("item1"),
                200L,
                0,
                20
        );

        // Then
        assertThat(page1).isNotEqualTo(page2);
    }

    @Test
    void should_notBeEqual_when_differentPage() {
        // Given
        Page<String> page1 = new Page<String>(
                Arrays.asList("item1"),
                100L,
                0,
                20
        );
        Page<String> page2 = new Page<String>(
                Arrays.asList("item1"),
                100L,
                1,
                20
        );

        // Then
        assertThat(page1).isNotEqualTo(page2);
    }
}