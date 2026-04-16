package com.claudej.application.auth.command;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录命令
 */
@Data
public class LoginCommand {
    @NotBlank(message = "邮箱或手机号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;

    private boolean rememberMe;
    private String ipAddress;
    private String userAgent;
}
