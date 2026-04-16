package com.claudej.application.auth.command;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登出命令
 */
@Data
public class LogoutCommand {
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    private String sessionId;
}
