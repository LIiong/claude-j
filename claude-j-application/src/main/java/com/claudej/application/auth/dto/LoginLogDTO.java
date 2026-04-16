package com.claudej.application.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志DTO
 */
@Data
public class LoginLogDTO {
    private Long id;
    private String userId;
    private String loginType;
    private String ipAddress;
    private String deviceType;
    private String status;
    private String failReason;
    private LocalDateTime createTime;
}
