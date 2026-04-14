package com.claudej.adapter.user.web.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户响应
 */
@Data
public class UserResponse {

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
