package com.claudej.infrastructure.user.service;

import com.claudej.domain.user.service.InviteCodeGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 邀请码生成器实现
 * 使用Base32字符集（排除易混淆字符）
 */
@Component
public class InviteCodeGeneratorImpl implements InviteCodeGenerator {

    private static final String BASE32_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = random.nextInt(BASE32_CHARS.length());
            code.append(BASE32_CHARS.charAt(index));
        }
        return code.toString();
    }
}
