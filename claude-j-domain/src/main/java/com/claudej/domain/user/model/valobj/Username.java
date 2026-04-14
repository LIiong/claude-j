package com.claudej.domain.user.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 用户名值对象 - 2-20字符
 */
@Getter
@EqualsAndHashCode
@ToString
public final class Username {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 20;

    private final String value;

    public Username(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_USERNAME, "用户名不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_USERNAME,
                    "用户名长度必须在" + MIN_LENGTH + "-" + MAX_LENGTH + "字符之间");
        }
        this.value = trimmed;
    }
}
