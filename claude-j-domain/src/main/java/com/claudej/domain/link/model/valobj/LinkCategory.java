package com.claudej.domain.link.model.valobj;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 链接分类值对象
 */
@Getter
@EqualsAndHashCode
@ToString
public final class LinkCategory {

    private final String value;

    public LinkCategory(String value) {
        if (value == null || value.trim().isEmpty()) {
            this.value = "default";
        } else {
            String trimmed = value.trim();
            if (trimmed.length() > 50) {
                throw new IllegalArgumentException("Category length must not exceed 50");
            }
            this.value = trimmed.toLowerCase();
        }
    }
}
