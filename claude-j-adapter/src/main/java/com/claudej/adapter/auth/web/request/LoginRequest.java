package com.claudej.adapter.auth.web.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求
 */
@Data
public class LoginRequest {

    @NotBlank(message = "邮箱或手机号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;

    private boolean rememberMe;
}
