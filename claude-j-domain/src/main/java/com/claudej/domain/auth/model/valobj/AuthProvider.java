package com.claudej.domain.auth.model.valobj;

/**
 * 认证方式枚举
 */
public enum AuthProvider {
    PASSWORD("密码登录"),
    SMS("短信验证码登录"),
    EMAIL("邮箱验证码登录"),
    GOOGLE("Google OAuth"),
    GITHUB("GitHub OAuth"),
    WECHAT("微信OAuth");

    private final String description;

    AuthProvider(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
