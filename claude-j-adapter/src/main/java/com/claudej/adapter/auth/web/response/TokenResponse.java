package com.claudej.adapter.auth.web.response;

import lombok.Data;

/**
 * Token响应
 */
@Data
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private String userId;
    private String username;
    private String email;
    private String phone;
}
