package com.claudej.domain.user.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.regex.Pattern;

/**
 * 邀请码值对象 - 6位字母数字组合
 * 使用Base32字符集，排除易混淆字符(0/O, 1/I/l)
 */
@Getter
@EqualsAndHashCode
@ToString
public final class InviteCode {

    private static final int LENGTH = 6;
    private static final String ALLOWED_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final Pattern INVITE_CODE_PATTERN = Pattern.compile(
            "^[" + ALLOWED_CHARS + "]{" + LENGTH + "}$");

    private final String value;

    public InviteCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INVITE_CODE, "邀请码不能为空");
        }
        String trimmed = value.trim().toUpperCase();
        if (!INVITE_CODE_PATTERN.matcher(trimmed).matches()) {
            throw new BusinessException(ErrorCode.INVALID_INVITE_CODE,
                    "邀请码必须为6位字母数字组合(排除0/O/1/I/l)");
        }
        this.value = trimmed;
    }

    /**
     * 生成随机邀请码
     */
    public static InviteCode generate() {
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALLOWED_CHARS.charAt(random.nextInt(ALLOWED_CHARS.length())));
        }
        return new InviteCode(sb.toString());
    }
}
