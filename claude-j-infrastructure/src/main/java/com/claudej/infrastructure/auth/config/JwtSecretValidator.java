package com.claudej.infrastructure.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * JWT Secret 启动校验器
 * 在应用启动时校验 JWT_SECRET 环境变量配置
 */
@Component
public class JwtSecretValidator implements ApplicationRunner {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public void run(ApplicationArguments args) {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT_SECRET environment variable is required");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters, current: " + jwtSecret.length());
        }
    }
}
