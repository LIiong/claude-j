package com.claudej.adapter.user.web.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 创建用户请求
 */
@Data
public class CreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度必须在2-20字符之间")
    private String username;

    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "邮箱格式无效")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式无效，应为11位数字")
    private String phone;

    @Pattern(regexp = "^[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{6}$", message = "邀请码格式无效")
    private String inviteCode;
}
