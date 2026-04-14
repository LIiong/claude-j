package com.claudej.domain.link.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 链接名称值对象
 */
@Getter
@EqualsAndHashCode
@ToString
public final class LinkName {

    private final String value;

    public LinkName(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.LINK_NAME_EMPTY);
        }
        if (value.length() > 100) {
            throw new BusinessException(ErrorCode.LINK_NAME_TOO_LONG);
        }
        this.value = value.trim();
    }
}
