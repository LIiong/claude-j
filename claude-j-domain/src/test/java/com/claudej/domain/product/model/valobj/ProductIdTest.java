package com.claudej.domain.product.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductIdTest {

    @Test
    void should_createProductId_when_valueIsValid() {
        // Given
        String validUuid = "product-uuid-12345";

        // When
        ProductId productId = new ProductId(validUuid);

        // Then
        assertThat(productId.getValue()).isEqualTo(validUuid);
    }

    @Test
    void should_throwException_when_valueIsNull() {
        // When & Then
        assertThatThrownBy(() -> new ProductId(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_ID_EMPTY);
    }

    @Test
    void should_throwException_when_valueIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> new ProductId(""))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_ID_EMPTY);
    }

    @Test
    void should_throwException_when_valueIsBlank() {
        // When & Then
        assertThatThrownBy(() -> new ProductId("   "))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_ID_EMPTY);
    }

    @Test
    void should_beEqual_when_valuesMatch() {
        // Given
        String value = "product-uuid-12345";
        ProductId productId1 = new ProductId(value);
        ProductId productId2 = new ProductId(value);

        // Then
        assertThat(productId1).isEqualTo(productId2);
        assertThat(productId1.hashCode()).isEqualTo(productId2.hashCode());
    }

    @Test
    void should_notBeEqual_when_valuesDiffer() {
        // Given
        ProductId productId1 = new ProductId("uuid-1");
        ProductId productId2 = new ProductId("uuid-2");

        // Then
        assertThat(productId1).isNotEqualTo(productId2);
    }

    @Test
    void should_trimValue_when_valueHasWhitespace() {
        // Given
        String valueWithWhitespace = "  product-uuid  ";

        // When
        ProductId productId = new ProductId(valueWithWhitespace);

        // Then
        assertThat(productId.getValue()).isEqualTo("product-uuid");
    }
}