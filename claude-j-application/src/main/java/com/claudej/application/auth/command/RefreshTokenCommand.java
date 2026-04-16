package com.claudej.application.auth.command;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 刷新Token命令
 */
@Data
public class RefreshTokenCommand {
    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}
