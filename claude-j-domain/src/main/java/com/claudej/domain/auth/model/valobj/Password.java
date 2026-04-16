package com.claudej.domain.auth.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.regex.Pattern;

/**
 * 密码值对象
 */
@Getter
@EqualsAndHashCode
public class Password {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\|,.<>\\/?]");

    private final String rawPassword;

    private Password(String rawPassword) {
        this.rawPassword = rawPassword;
    }

    /**
     * 创建密码（仅用于验证，不存储明文）
     */
    public static Password of(String rawPassword) {
        validate(rawPassword);
        return new Password(rawPassword);
    }

    /**
     * 验证密码强度
     */
    public static void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD, "密码不能为空");
        }
        if (rawPassword.length() < MIN_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD, "密码长度至少" + MIN_LENGTH + "位");
        }
        if (rawPassword.length() > MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD, "密码长度不能超过" + MAX_LENGTH + "位");
        }
        if (!UPPERCASE_PATTERN.matcher(rawPassword).find()) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD, "密码必须包含大写字母");
        }
        if (!LOWERCASE_PATTERN.matcher(rawPassword).find()) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD, "密码必须包含小写字母");
        }
        if (!DIGIT_PATTERN.matcher(rawPassword).find()) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD, "密码必须包含数字");
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(rawPassword).find()) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD, "密码必须包含特殊字符");
        }
    }

    /**
     * 获取明文密码（用于加密）
     */
    public String getRawPassword() {
        return rawPassword;
    }
}
