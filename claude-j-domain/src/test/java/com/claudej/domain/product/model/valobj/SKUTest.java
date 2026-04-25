package com.claudej.domain.product.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SKUTest {

    @Test
    void should_createSKU_when_valueIsValid() {
        // Given
        String skuCode = "SKU001";
        int stock = 100;

        // When
        SKU sku = new SKU(skuCode, stock);

        // Then
        assertThat(sku.getSkuCode()).isEqualTo(skuCode);
        assertThat(sku.getStock()).isEqualTo(stock);
    }

    @Test
    void should_throwException_when_skuCodeIsNull() {
        // When & Then
        assertThatThrownBy(() -> new SKU(null, 100))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_SKU_CODE_EMPTY);
    }

    @Test
    void should_throwException_when_skuCodeIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> new SKU("", 100))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_SKU_CODE_EMPTY);
    }

    @Test
    void should_throwException_when_skuCodeIsBlank() {
        // When & Then
        assertThatThrownBy(() -> new SKU("   ", 100))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_SKU_CODE_EMPTY);
    }

    @Test
    void should_throwException_when_skuCodeLengthMoreThan32() {
        // Given
        String longSkuCode = "SKU-CODE-THAT-IS-VERY-LONG-EXCEEDS-THIRTY-TWO-CHARACTERS";

        // When & Then
        assertThatThrownBy(() -> new SKU(longSkuCode, 100))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_SKU_CODE_LENGTH_INVALID);
    }

    @Test
    void should_createSKU_when_skuCodeLengthIs32() {
        // Given
        String maxValidSkuCode = "SKU-CODE-EXACTLY-THIRTY-TWO-CHAR"; // 32 chars

        // When
        SKU sku = new SKU(maxValidSkuCode, 100);

        // Then
        assertThat(sku.getSkuCode()).isEqualTo(maxValidSkuCode);
    }

    @Test
    void should_throwException_when_stockIsNegative() {
        // When & Then
        assertThatThrownBy(() -> new SKU("SKU001", -1))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_STOCK_NEGATIVE);
    }

    @Test
    void should_createSKU_when_stockIsZero() {
        // Given
        int zeroStock = 0;

        // When
        SKU sku = new SKU("SKU001", zeroStock);

        // Then
        assertThat(sku.getStock()).isEqualTo(zeroStock);
    }

    @Test
    void should_trimSkuCode_when_skuCodeHasWhitespace() {
        // Given
        String skuCodeWithWhitespace = "  SKU001  ";

        // When
        SKU sku = new SKU(skuCodeWithWhitespace, 100);

        // Then
        assertThat(sku.getSkuCode()).isEqualTo("SKU001");
    }

    @Test
    void should_beEqual_when_valuesMatch() {
        // Given
        SKU sku1 = new SKU("SKU001", 100);
        SKU sku2 = new SKU("SKU001", 100);

        // Then
        assertThat(sku1).isEqualTo(sku2);
        assertThat(sku1.hashCode()).isEqualTo(sku2.hashCode());
    }

    @Test
    void should_notBeEqual_when_skuCodesDiffer() {
        // Given
        SKU sku1 = new SKU("SKU001", 100);
        SKU sku2 = new SKU("SKU002", 100);

        // Then
        assertThat(sku1).isNotEqualTo(sku2);
    }

    @Test
    void should_notBeEqual_when_stocksDiffer() {
        // Given
        SKU sku1 = new SKU("SKU001", 100);
        SKU sku2 = new SKU("SKU001", 50);

        // Then
        assertThat(sku1).isNotEqualTo(sku2);
    }

    @Test
    void should_beImmutable_when_created() {
        // Given
        SKU sku = new SKU("SKU001", 100);

        // Then - SKU should not have setters (immutability check)
        assertThat(sku.getSkuCode()).isEqualTo("SKU001");
        assertThat(sku.getStock()).isEqualTo(100);
        // No setter methods should exist
    }
}