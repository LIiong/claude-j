package com.claudej.application.auth.dto;

import lombok.Data;

/**
 * Token响应DTO
 */
@Data
public class TokenResponseDTO {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private String userId;
    private String username;
    private String email;
    private String phone;
}
