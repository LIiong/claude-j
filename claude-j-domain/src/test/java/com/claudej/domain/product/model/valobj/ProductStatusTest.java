package com.claudej.domain.product.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductStatusTest {

    @Test
    void should_convertToActive_when_fromDraft() {
        // When
        ProductStatus newStatus = ProductStatus.DRAFT.toActive();

        // Then
        assertThat(newStatus).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void should_convertToInactive_when_fromActive() {
        // When
        ProductStatus newStatus = ProductStatus.ACTIVE.toInactive();

        // Then
        assertThat(newStatus).isEqualTo(ProductStatus.INACTIVE);
    }

    @Test
    void should_convertToActive_when_fromInactive() {
        // When
        ProductStatus newStatus = ProductStatus.INACTIVE.toActive();

        // Then
        assertThat(newStatus).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void should_throwException_when_convertToInactive_fromDraft() {
        // When & Then
        assertThatThrownBy(() -> ProductStatus.DRAFT.toInactive())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许下架");
    }

    @Test
    void should_throwException_when_convertToActive_fromActive() {
        // When & Then
        assertThatThrownBy(() -> ProductStatus.ACTIVE.toActive())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许上架");
    }

    @Test
    void should_throwException_when_convertToInactive_fromInactive() {
        // When & Then
        assertThatThrownBy(() -> ProductStatus.INACTIVE.toInactive())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许下架");
    }

    @Test
    void should_canActivate_when_draftOrInactive() {
        // Then
        assertThat(ProductStatus.DRAFT.canActivate()).isTrue();
        assertThat(ProductStatus.ACTIVE.canActivate()).isFalse();
        assertThat(ProductStatus.INACTIVE.canActivate()).isTrue();
    }

    @Test
    void should_canDeactivate_when_activeOnly() {
        // Then
        assertThat(ProductStatus.DRAFT.canDeactivate()).isFalse();
        assertThat(ProductStatus.ACTIVE.canDeactivate()).isTrue();
        assertThat(ProductStatus.INACTIVE.canDeactivate()).isFalse();
    }

    @Test
    void should_getDescription_when_statusCreated() {
        // Then
        assertThat(ProductStatus.DRAFT.getDescription()).isEqualTo("草稿");
        assertThat(ProductStatus.ACTIVE.getDescription()).isEqualTo("已上架");
        assertThat(ProductStatus.INACTIVE.getDescription()).isEqualTo("已下架");
    }

    @Test
    void should_haveCorrectStatusValues() {
        // Then
        assertThat(ProductStatus.values()).hasSize(3);
        assertThat(ProductStatus.DRAFT).isNotNull();
        assertThat(ProductStatus.ACTIVE).isNotNull();
        assertThat(ProductStatus.INACTIVE).isNotNull();
    }
}