package com.claudej.adapter.user.web.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 验证邀请码请求
 */
@Data
public class ValidateInviteCodeRequest {

    @NotBlank(message = "邀请码不能为空")
    @Pattern(regexp = "^[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{6}$", message = "邀请码格式无效")
    private String inviteCode;
}
