package com.claudej.adapter.auth.web.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 短信登录请求
 */
@Data
public class SmsLoginRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式无效")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    private String verificationCode;

    private boolean rememberMe;
}
