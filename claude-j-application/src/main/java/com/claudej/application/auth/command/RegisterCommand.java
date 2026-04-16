package com.claudej.application.auth.command;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 用户注册命令
 */
@Data
public class RegisterCommand {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String email;
    private String phone;
    private String verificationCode;
    private String inviteCode;
    private String ipAddress;
    private String userAgent;
}
