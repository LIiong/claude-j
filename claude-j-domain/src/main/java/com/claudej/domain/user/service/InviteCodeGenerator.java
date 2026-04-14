package com.claudej.domain.user.service;

/**
 * 邀请码生成器 - 领域服务端口
 */
public interface InviteCodeGenerator {

    /**
     * 生成唯一邀请码
     *
     * @return 6位字母数字组合
     */
    String generate();
}
