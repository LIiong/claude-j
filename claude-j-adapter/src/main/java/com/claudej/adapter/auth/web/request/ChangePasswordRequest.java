package com.claudej.adapter.auth.web.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 修改密码请求
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 128, message = "新密码长度应为8-128字符")
    private String newPassword;
}
