package com.claudej.application.auth.command;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 短信登录命令
 */
@Data
public class SmsLoginCommand {
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    private String verificationCode;

    private boolean rememberMe;
    private String ipAddress;
    private String userAgent;
}
