package com.claudej.domain.product.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductNameTest {

    @Test
    void should_createProductName_when_valueIsValid() {
        // Given
        String validName = "Valid Product Name";

        // When
        ProductName productName = new ProductName(validName);

        // Then
        assertThat(productName.getValue()).isEqualTo(validName);
    }

    @Test
    void should_throwException_when_valueIsNull() {
        // When & Then
        assertThatThrownBy(() -> new ProductName(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_NAME_EMPTY);
    }

    @Test
    void should_throwException_when_valueIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> new ProductName(""))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_NAME_EMPTY);
    }

    @Test
    void should_throwException_when_valueIsBlank() {
        // When & Then
        assertThatThrownBy(() -> new ProductName("   "))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_NAME_EMPTY);
    }

    @Test
    void should_throwException_when_lengthLessThan2() {
        // Given
        String shortName = "A";

        // When & Then
        assertThatThrownBy(() -> new ProductName(shortName))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_NAME_LENGTH_INVALID);
    }

    @Test
    void should_throwException_when_lengthMoreThan100() {
        // Given
        String longName = "This is a very long product name that exceeds the maximum allowed length of one hundred characters for sure";

        // When & Then
        assertThatThrownBy(() -> new ProductName(longName))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_NAME_LENGTH_INVALID);
    }

    @Test
    void should_createProductName_when_lengthIs2() {
        // Given
        String minValidName = "AB";

        // When
        ProductName productName = new ProductName(minValidName);

        // Then
        assertThat(productName.getValue()).isEqualTo(minValidName);
    }

    @Test
    void should_createProductName_when_lengthIs100() {
        // Given
        String maxValidName = "This product name has exactly one hundred characters which is the maximum allowed length okay";

        // When
        ProductName productName = new ProductName(maxValidName);

        // Then
        assertThat(productName.getValue()).isEqualTo(maxValidName);
    }

    @Test
    void should_trimValue_when_valueHasWhitespace() {
        // Given
        String nameWithWhitespace = "  Valid Name  ";

        // When
        ProductName productName = new ProductName(nameWithWhitespace);

        // Then
        assertThat(productName.getValue()).isEqualTo("Valid Name");
    }

    @Test
    void should_beEqual_when_valuesMatch() {
        // Given
        String value = "Product Name";
        ProductName name1 = new ProductName(value);
        ProductName name2 = new ProductName(value);

        // Then
        assertThat(name1).isEqualTo(name2);
        assertThat(name1.hashCode()).isEqualTo(name2.hashCode());
    }

    @Test
    void should_notBeEqual_when_valuesDiffer() {
        // Given
        ProductName name1 = new ProductName("Product A");
        ProductName name2 = new ProductName("Product B");

        // Then
        assertThat(name1).isNotEqualTo(name2);
    }
}