package com.claudej.adapter.auth.web.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 用户注册请求
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度应为2-20字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 128, message = "密码长度应为8-128字符")
    private String password;

    private String email;
    private String phone;
    private String verificationCode;
    private String inviteCode;
}
