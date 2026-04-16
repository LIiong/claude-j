package com.claudej.application.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 认证用户DTO
 */
@Data
public class AuthUserDTO {
    private String userId;
    private String username;
    private String email;
    private String phone;
    private boolean emailVerified;
    private boolean phoneVerified;
    private String status;
    private int failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastLoginAt;
    private LocalDateTime passwordChangedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
