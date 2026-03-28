package com.claudej.domain.shortlink.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public class OriginalUrl {

    private static final int MAX_LENGTH = 2048;

    private final String value;

    public OriginalUrl(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_ORIGINAL_URL, "原始URL不能为空");
        }
        if (value.length() > MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_ORIGINAL_URL,
                    "原始URL长度不能超过" + MAX_LENGTH + "个字符");
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            throw new BusinessException(ErrorCode.INVALID_ORIGINAL_URL,
                    "原始URL必须以http://或https://开头");
        }
        this.value = value;
    }
}
