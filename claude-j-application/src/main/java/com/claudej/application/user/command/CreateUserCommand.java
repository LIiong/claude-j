package com.claudej.application.user.command;

import lombok.Data;

/**
 * 创建用户命令
 */
@Data
public class CreateUserCommand {

    private String username;
    private String email;
    private String phone;
    private String inviteCode;
}
