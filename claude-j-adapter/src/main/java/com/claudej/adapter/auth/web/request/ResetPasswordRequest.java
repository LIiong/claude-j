package com.claudej.adapter.auth.web.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 重置密码请求
 */
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String verificationCode;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 128, message = "新密码长度应为8-128字符")
    private String newPassword;
}
