package com.claudej.adapter.auth.web.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登出请求
 */
@Data
public class LogoutRequest {

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    private String sessionId;
}
