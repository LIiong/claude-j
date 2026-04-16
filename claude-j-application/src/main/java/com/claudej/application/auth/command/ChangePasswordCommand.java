package com.claudej.application.auth.command;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 修改密码命令
 */
@Data
public class ChangePasswordCommand {
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
