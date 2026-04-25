package com.claudej.infrastructure.auth.token;

import com.claudej.domain.auth.model.valobj.JwtToken;
import com.claudej.domain.auth.service.TokenService;
import com.claudej.domain.user.model.valobj.Role;
import com.claudej.domain.user.model.valobj.UserId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT Token 服务实现
 */
@Component
public class JwtTokenServiceImpl implements TokenService {

    private final SecretKey accessTokenKey;
    private final SecretKey refreshTokenKey;
    private final long accessTokenExpirationMinutes;
    private final long refreshTokenExpirationDays;

    public JwtTokenServiceImpl(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration:60}") long accessTokenExpirationMinutes,
            @Value("${jwt.refresh-token-expiration:7}") long refreshTokenExpirationDays) {
        this.accessTokenKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.refreshTokenKey = Keys.hmacShaKeyFor((secret + "refresh").getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    @Override
    public JwtToken generateTokenPair(UserId userId) {
        // 默认只有 USER 角色
        Set<Role> defaultRoles = new HashSet<>();
        defaultRoles.add(Role.USER);
        return generateTokenPair(userId, defaultRoles);
    }

    @Override
    public JwtToken generateTokenPair(UserId userId, Set<Role> roles) {
        Instant now = Instant.now();

        // 将角色转换为字符串列表
        List<String> roleNames = roles.stream()
                .map(Role::name)
                .collect(Collectors.toList());

        // Access Token - 1小时
        String accessToken = Jwts.builder()
                .subject(userId.getValue())
                .claim("typ", "access")
                .claim("roles", roleNames)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES)))
                .signWith(accessTokenKey)
                .compact();

        // Refresh Token - 7天
        String refreshToken = Jwts.builder()
                .subject(userId.getValue())
                .claim("typ", "refresh")
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTokenExpirationDays, ChronoUnit.DAYS)))
                .signWith(refreshTokenKey)
                .compact();

        return JwtToken.of(accessToken, refreshToken, accessTokenExpirationMinutes * 60);
    }

    @Override
    public boolean validateAccessToken(String accessToken) {
        try {
            Claims claims = parseToken(accessToken, accessTokenKey);
            String type = claims.get("typ", String.class);
            return "access".equals(type);
        } catch (ExpiredJwtException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean validateRefreshToken(String refreshToken) {
        try {
            Claims claims = parseToken(refreshToken, refreshTokenKey);
            String type = claims.get("typ", String.class);
            return "refresh".equals(type);
        } catch (ExpiredJwtException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public UserId extractUserIdFromToken(String accessToken) {
        try {
            Claims claims = parseToken(accessToken, accessTokenKey);
            String userId = claims.getSubject();
            if (userId == null || userId.isEmpty()) {
                return null;
            }
            return new UserId(userId);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Set<Role> extractRolesFromToken(String accessToken) {
        try {
            Claims claims = parseToken(accessToken, accessTokenKey);
            List<String> roleNames = claims.get("roles", List.class);
            if (roleNames == null || roleNames.isEmpty()) {
                // 默认 USER 角色
                Set<Role> defaultRoles = new HashSet<>();
                defaultRoles.add(Role.USER);
                return defaultRoles;
            }
            return roleNames.stream()
                    .map(Role::valueOf)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            // 解析失败返回默认角色
            Set<Role> defaultRoles = new HashSet<>();
            defaultRoles.add(Role.USER);
            return defaultRoles;
        }
    }

    @Override
    public JwtToken refreshAccessToken(String refreshToken) {
        if (!validateRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        try {
            Claims claims = parseToken(refreshToken, refreshTokenKey);
            String userId = claims.getSubject();
            if (userId == null || userId.isEmpty()) {
                throw new IllegalArgumentException("Invalid refresh token: no user ID");
            }
            return generateTokenPair(new UserId(userId));
        } catch (ExpiredJwtException e) {
            throw new IllegalArgumentException("Refresh token has expired");
        }
    }

    /**
     * 解析 Token
     */
    private Claims parseToken(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
