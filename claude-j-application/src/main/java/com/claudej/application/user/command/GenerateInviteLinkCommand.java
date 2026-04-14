package com.claudej.application.user.command;

import lombok.Data;

/**
 * 生成邀请链接命令
 */
@Data
public class GenerateInviteLinkCommand {

    private String userId;
}
