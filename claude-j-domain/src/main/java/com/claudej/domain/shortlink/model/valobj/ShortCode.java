package com.claudej.domain.shortlink.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.regex.Pattern;

@Getter
@EqualsAndHashCode
@ToString
public class ShortCode {

    private static final Pattern BASE62_PATTERN = Pattern.compile("^[0-9a-zA-Z]{6}$");

    private final String value;

    public ShortCode(String value) {
        if (value == null || value.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_SHORT_CODE, "短链码不能为空");
        }
        if (!BASE62_PATTERN.matcher(value).matches()) {
            throw new BusinessException(ErrorCode.INVALID_SHORT_CODE,
                    "短链码必须为6位Base62字符(0-9a-zA-Z)");
        }
        this.value = value;
    }
}
