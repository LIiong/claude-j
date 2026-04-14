package com.claudej.application.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户 DTO
 */
@Data
public class UserDTO {

    private String userId;
    private String username;
    private String email;
    private String phone;
    private String status;
    private String inviteCode;
    private String inviterId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
