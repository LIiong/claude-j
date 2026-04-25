package com.claudej.domain.inventory.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SkuCode 值对象测试
 */
class SkuCodeTest {

    @Test
    void should_throw_when_value_is_null() {
        assertThatThrownBy(() -> new SkuCode(null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SKU_CODE_EMPTY);
    }

    @Test
    void should_throw_when_value_is_empty() {
        assertThatThrownBy(() -> new SkuCode(""))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SKU_CODE_EMPTY);
    }

    @Test
    void should_throw_when_value_is_blank() {
        assertThatThrownBy(() -> new SkuCode("   "))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SKU_CODE_EMPTY);
    }

    @Test
    void should_throw_when_value_too_long() {
        assertThatThrownBy(() -> new SkuCode("SKU-CODE-THAT-IS-MORE-THAN-32-CHARACTERS"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SKU_CODE_TOO_LONG);
    }

    @Test
    void should_create_when_value_is_valid() {
        SkuCode skuCode = new SkuCode("SKU-001");
        assertThat(skuCode.getValue()).isEqualTo("SKU-001");
    }

    @Test
    void should_create_when_value_is_32_chars() {
        SkuCode skuCode = new SkuCode("SKU-CODE-EXACTLY-32-CHARACTERS");
        assertThat(skuCode.getValue()).isEqualTo("SKU-CODE-EXACTLY-32-CHARACTERS");
    }

    @Test
    void should_trim_when_value_has_whitespace() {
        SkuCode skuCode = new SkuCode("  SKU-001  ");
        assertThat(skuCode.getValue()).isEqualTo("SKU-001");
    }

    @Test
    void should_equal_when_values_match() {
        SkuCode code1 = new SkuCode("SKU-001");
        SkuCode code2 = new SkuCode("SKU-001");
        assertThat(code1).isEqualTo(code2);
        assertThat(code1.hashCode()).isEqualTo(code2.hashCode());
    }

    @Test
    void should_not_equal_when_values_differ() {
        SkuCode code1 = new SkuCode("SKU-001");
        SkuCode code2 = new SkuCode("SKU-002");
        assertThat(code1).isNotEqualTo(code2);
    }

    @Test
    void should_toString_contain_value() {
        SkuCode skuCode = new SkuCode("SKU-001");
        assertThat(skuCode.toString()).contains("SKU-001");
    }
}