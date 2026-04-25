package com.claudej.application.user.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

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
    private Set<String> roles;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
