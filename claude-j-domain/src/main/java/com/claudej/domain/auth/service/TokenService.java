package com.claudej.domain.auth.service;

import com.claudej.domain.auth.model.valobj.JwtToken;
import com.claudej.domain.user.model.valobj.Role;
import com.claudej.domain.user.model.valobj.UserId;

import java.util.Set;

/**
 * Token服务端口
 */
public interface TokenService {

    /**
     * 生成JWT令牌对
     */
    JwtToken generateTokenPair(UserId userId);

    /**
     * 生成JWT令牌对（携带角色）
     */
    JwtToken generateTokenPair(UserId userId, Set<Role> roles);

    /**
     * 验证访问令牌
     */
    boolean validateAccessToken(String accessToken);

    /**
     * 验证刷新令牌
     */
    boolean validateRefreshToken(String refreshToken);

    /**
     * 从访问令牌提取用户ID
     */
    UserId extractUserIdFromToken(String accessToken);

    /**
     * 从访问令牌提取角色
     */
    Set<Role> extractRolesFromToken(String accessToken);

    /**
     * 刷新访问令牌
     */
    JwtToken refreshAccessToken(String refreshToken);
}
