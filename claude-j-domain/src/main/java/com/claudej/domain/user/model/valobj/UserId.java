package com.claudej.domain.user.model.valobj;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 用户ID值对象 - 业务用户标识
 * 格式：UR + 16位随机字母数字
 */
@Getter
@EqualsAndHashCode
@ToString
public final class UserId {

    private static final String PREFIX = "UR";
    private static final int RANDOM_PART_LENGTH = 16;
    private static final String ALLOWED_CHARS = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    private final String value;

    public UserId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户ID不能为空");
        }
        String trimmed = value.trim();
        if (!isValidFormat(trimmed)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户ID格式无效，应为UR开头+16位字母数字");
        }
        this.value = trimmed;
    }

    /**
     * 生成新的用户ID
     */
    public static UserId generate() {
        StringBuilder sb = new StringBuilder(PREFIX);
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < RANDOM_PART_LENGTH; i++) {
            sb.append(ALLOWED_CHARS.charAt(random.nextInt(ALLOWED_CHARS.length())));
        }
        return new UserId(sb.toString());
    }

    /**
     * 验证格式
     */
    private boolean isValidFormat(String value) {
        if (!value.startsWith(PREFIX)) {
            return false;
        }
        String randomPart = value.substring(PREFIX.length());
        if (randomPart.length() != RANDOM_PART_LENGTH) {
            return false;
        }
        for (char c : randomPart.toCharArray()) {
            if (ALLOWED_CHARS.indexOf(c) < 0) {
                return false;
            }
        }
        return true;
    }
}
