package com.claudej.domain.auth.service;

/**
 * 密码加密服务端口
 */
public interface PasswordEncoder {

    /**
     * 加密密码
     */
    String encode(String rawPassword);

    /**
     * 验证密码
     */
    boolean matches(String rawPassword, String encodedPassword);
}
