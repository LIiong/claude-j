package com.claudej.domain.link.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.regex.Pattern;

/**
 * 链接地址值对象
 */
@Getter
@EqualsAndHashCode
@ToString
public final class LinkUrl {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$", Pattern.CASE_INSENSITIVE);

    private final String value;

    public LinkUrl(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.LINK_URL_EMPTY);
        }
        String trimmed = value.trim();
        if (trimmed.length() > 500) {
            throw new BusinessException(ErrorCode.LINK_URL_TOO_LONG);
        }
        if (!URL_PATTERN.matcher(trimmed).matches()) {
            throw new BusinessException(ErrorCode.LINK_URL_INVALID);
        }
        this.value = trimmed;
    }
}
