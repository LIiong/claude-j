package com.claudej.domain.common.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRequestTest {

    @Test
    void should_createPageRequest_when_validParamsProvided() {
        // When
        PageRequest pageRequest = new PageRequest(0, 20, "createTime", SortDirection.DESC);

        // Then
        assertThat(pageRequest.getPage()).isEqualTo(0);
        assertThat(pageRequest.getSize()).isEqualTo(20);
        assertThat(pageRequest.getSortField()).isEqualTo("createTime");
        assertThat(pageRequest.getSortDirection()).isEqualTo(SortDirection.DESC);
    }

    @Test
    void should_useDefaultValues_when_nullParamsProvided() {
        // When
        PageRequest pageRequest = PageRequest.of(null, null, null, null);

        // Then
        assertThat(pageRequest.getPage()).isEqualTo(0);
        assertThat(pageRequest.getSize()).isEqualTo(20);
        assertThat(pageRequest.getSortField()).isNull();
        assertThat(pageRequest.getSortDirection()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void should_throwException_when_pageIsNegative() {
        // When & Then
        assertThatThrownBy(() -> new PageRequest(-1, 20, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("页码不能为负数");
    }

    @Test
    void should_throwException_when_sizeIsZero() {
        // When & Then
        assertThatThrownBy(() -> new PageRequest(0, 0, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("每页条数必须在1-100之间");
    }

    @Test
    void should_throwException_when_sizeIsNegative() {
        // When & Then
        assertThatThrownBy(() -> new PageRequest(0, -1, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("每页条数必须在1-100之间");
    }

    @Test
    void should_throwException_when_sizeExceedsMax() {
        // When & Then
        assertThatThrownBy(() -> new PageRequest(0, 101, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("每页条数必须在1-100之间");
    }

    @Test
    void should_createPageRequest_when_sizeAtMax() {
        // When
        PageRequest pageRequest = new PageRequest(0, 100, null, null);

        // Then
        assertThat(pageRequest.getSize()).isEqualTo(100);
    }

    @Test
    void should_createPageRequest_when_sizeAtMin() {
        // When
        PageRequest pageRequest = new PageRequest(0, 1, null, null);

        // Then
        assertThat(pageRequest.getSize()).isEqualTo(1);
    }

    @Test
    void should_trimSortField_when_hasWhitespace() {
        // When
        PageRequest pageRequest = new PageRequest(0, 20, "  createTime  ", null);

        // Then
        assertThat(pageRequest.getSortField()).isEqualTo("createTime");
    }

    @Test
    void should_setSortFieldToNull_when_empty() {
        // When
        PageRequest pageRequest = new PageRequest(0, 20, "", null);

        // Then
        assertThat(pageRequest.getSortField()).isNull();
    }

    @Test
    void should_setSortFieldToNull_when_blank() {
        // When
        PageRequest pageRequest = new PageRequest(0, 20, "   ", null);

        // Then
        assertThat(pageRequest.getSortField()).isNull();
    }

    @Test
    void should_useDefaultSortDirection_when_null() {
        // When
        PageRequest pageRequest = new PageRequest(0, 20, "createTime", null);

        // Then
        assertThat(pageRequest.getSortDirection()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void should_calculateOffset_correctly() {
        // Given
        PageRequest pageRequest = new PageRequest(0, 20, null, null);
        PageRequest pageRequest2 = new PageRequest(2, 10, null, null);
        PageRequest pageRequest3 = new PageRequest(5, 50, null, null);

        // Then
        assertThat(pageRequest.getOffset()).isEqualTo(0);
        assertThat(pageRequest2.getOffset()).isEqualTo(20);
        assertThat(pageRequest3.getOffset()).isEqualTo(250);
    }

    @Test
    void should_createDefaultPageRequest() {
        // When
        PageRequest pageRequest = PageRequest.defaultPage();

        // Then
        assertThat(pageRequest.getPage()).isEqualTo(0);
        assertThat(pageRequest.getSize()).isEqualTo(20);
        assertThat(pageRequest.getSortField()).isNull();
        assertThat(pageRequest.getSortDirection()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void should_beEqual_when_sameValues() {
        // Given
        PageRequest request1 = new PageRequest(0, 20, "createTime", SortDirection.ASC);
        PageRequest request2 = new PageRequest(0, 20, "createTime", SortDirection.ASC);

        // Then
        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    void should_notBeEqual_when_differentPage() {
        // Given
        PageRequest request1 = new PageRequest(0, 20, null, null);
        PageRequest request2 = new PageRequest(1, 20, null, null);

        // Then
        assertThat(request1).isNotEqualTo(request2);
    }

    @Test
    void should_notBeEqual_when_differentSize() {
        // Given
        PageRequest request1 = new PageRequest(0, 20, null, null);
        PageRequest request2 = new PageRequest(0, 10, null, null);

        // Then
        assertThat(request1).isNotEqualTo(request2);
    }

    @Test
    void should_notBeEqual_when_differentSortField() {
        // Given
        PageRequest request1 = new PageRequest(0, 20, "createTime", null);
        PageRequest request2 = new PageRequest(0, 20, "updateTime", null);

        // Then
        assertThat(request1).isNotEqualTo(request2);
    }

    @Test
    void should_notBeEqual_when_differentSortDirection() {
        // Given
        PageRequest request1 = new PageRequest(0, 20, "createTime", SortDirection.ASC);
        PageRequest request2 = new PageRequest(0, 20, "createTime", SortDirection.DESC);

        // Then
        assertThat(request1).isNotEqualTo(request2);
    }
}