package com.claudej.application.auth.command;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 重置密码命令
 */
@Data
public class ResetPasswordCommand {
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String verificationCode;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
