package com.claudej.adapter.auth.web.response;

import lombok.Data;

/**
 * 认证用户响应
 */
@Data
public class AuthUserResponse {

    private String userId;
    private String username;
    private String email;
    private String phone;
    private boolean emailVerified;
    private boolean phoneVerified;
    private String status;
    private int failedLoginAttempts;
}
