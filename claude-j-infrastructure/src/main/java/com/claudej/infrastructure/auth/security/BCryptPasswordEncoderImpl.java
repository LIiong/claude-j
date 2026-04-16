package com.claudej.infrastructure.auth.security;

import com.claudej.domain.auth.service.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt 密码编码器实现
 */
@Component
public class BCryptPasswordEncoderImpl implements PasswordEncoder {

    private final BCryptPasswordEncoder delegate;

    public BCryptPasswordEncoderImpl() {
        // strength=10 是默认值，与 Spring Security 默认值一致
        this.delegate = new BCryptPasswordEncoder(10);
    }

    @Override
    public String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Raw password cannot be null or empty");
        }
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return delegate.matches(rawPassword, encodedPassword);
    }
}
