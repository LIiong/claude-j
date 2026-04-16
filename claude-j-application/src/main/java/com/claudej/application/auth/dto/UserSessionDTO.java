package com.claudej.application.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户会话DTO
 */
@Data
public class UserSessionDTO {
    private String sessionId;
    private String userId;
    private String refreshToken;
    private String deviceType;
    private String os;
    private String browser;
    private String ipAddress;
    private LocalDateTime expiresAt;
    private LocalDateTime createTime;
}
