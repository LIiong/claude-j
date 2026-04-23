package com.claudej.domain.common.model.valobj;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SortDirectionTest {

    @Test
    void should_have_asc_and_desc_values() {
        // Then
        assertThat(SortDirection.values()).hasSize(2);
        assertThat(SortDirection.ASC).isNotNull();
        assertThat(SortDirection.DESC).isNotNull();
    }

    @Test
    void should_return_asc_value() {
        // Then
        assertThat(SortDirection.ASC.getValue()).isEqualTo("ASC");
    }

    @Test
    void should_return_desc_value() {
        // Then
        assertThat(SortDirection.DESC.getValue()).isEqualTo("DESC");
    }

    @Test
    void should_parse_from_string_when_uppercase() {
        // When
        SortDirection asc = SortDirection.fromString("ASC");
        SortDirection desc = SortDirection.fromString("DESC");

        // Then
        assertThat(asc).isEqualTo(SortDirection.ASC);
        assertThat(desc).isEqualTo(SortDirection.DESC);
    }

    @Test
    void should_parse_from_string_when_lowercase() {
        // When
        SortDirection asc = SortDirection.fromString("asc");
        SortDirection desc = SortDirection.fromString("desc");

        // Then
        assertThat(asc).isEqualTo(SortDirection.ASC);
        assertThat(desc).isEqualTo(SortDirection.DESC);
    }

    @Test
    void should_parse_from_string_when_mixedCase() {
        // When
        SortDirection asc = SortDirection.fromString("AsC");
        SortDirection desc = SortDirection.fromString("DeSc");

        // Then
        assertThat(asc).isEqualTo(SortDirection.ASC);
        assertThat(desc).isEqualTo(SortDirection.DESC);
    }

    @Test
    void should_return_asc_when_null_string() {
        // When
        SortDirection result = SortDirection.fromString(null);

        // Then
        assertThat(result).isEqualTo(SortDirection.ASC);
    }

    @Test
    void should_return_asc_when_invalid_string() {
        // When
        SortDirection result = SortDirection.fromString("invalid");

        // Then
        assertThat(result).isEqualTo(SortDirection.ASC);
    }

    @Test
    void should_have_description() {
        // Then
        assertThat(SortDirection.ASC.getDescription()).isEqualTo("升序");
        assertThat(SortDirection.DESC.getDescription()).isEqualTo("降序");
    }
}