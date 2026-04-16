package com.claudej.domain.auth.model.valobj;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * JWT令牌值对象
 */
@Getter
@EqualsAndHashCode
public class JwtToken {

    private final String accessToken;
    private final String refreshToken;
    private final long expiresIn;

    private JwtToken(String accessToken, String refreshToken, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    /**
     * 创建JWT令牌
     */
    public static JwtToken of(String accessToken, String refreshToken, long expiresIn) {
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalArgumentException("访问令牌不能为空");
        }
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalArgumentException("刷新令牌不能为空");
        }
        if (expiresIn <= 0) {
            throw new IllegalArgumentException("过期时间必须大于0");
        }
        return new JwtToken(accessToken, refreshToken, expiresIn);
    }
}
