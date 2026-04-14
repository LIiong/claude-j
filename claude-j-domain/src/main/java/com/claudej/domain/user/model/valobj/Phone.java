package com.claudej.domain.user.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.regex.Pattern;

/**
 * 手机号值对象 - 11位数字
 */
@Getter
@EqualsAndHashCode
@ToString
public final class Phone {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final String value;

    public Phone(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PHONE, "手机号不能为空");
        }
        String trimmed = value.trim();
        if (!PHONE_PATTERN.matcher(trimmed).matches()) {
            throw new BusinessException(ErrorCode.INVALID_PHONE, "手机号格式无效，应为11位数字");
        }
        this.value = trimmed;
    }
}
