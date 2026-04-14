package com.claudej.domain.user.repository;

import com.claudej.domain.user.model.aggregate.User;
import com.claudej.domain.user.model.valobj.InviteCode;
import com.claudej.domain.user.model.valobj.UserId;
import com.claudej.domain.user.model.valobj.Username;

import java.util.List;
import java.util.Optional;

/**
 * 用户 Repository 端口接口
 */
public interface UserRepository {

    /**
     * 保存用户
     */
    User save(User user);

    /**
     * 根据用户ID查找用户
     */
    Optional<User> findByUserId(UserId userId);

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(Username username);

    /**
     * 根据邀请码查找用户
     */
    Optional<User> findByInviteCode(InviteCode inviteCode);

    /**
     * 查询被邀请的用户列表
     */
    List<User> findByInviterId(UserId inviterId);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(Username username);

    /**
     * 检查邀请码是否存在
     */
    boolean existsByInviteCode(InviteCode inviteCode);
}
