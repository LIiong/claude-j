package com.claudej.domain.auth.repository;

import com.claudej.domain.auth.model.aggregate.AuthUser;
import com.claudej.domain.user.model.valobj.UserId;

import java.util.Optional;

/**
 * 认证用户 Repository 端口接口
 */
public interface AuthUserRepository {

    /**
     * 保存认证用户
     */
    AuthUser save(AuthUser authUser);

    /**
     * 根据用户ID查找
     */
    Optional<AuthUser> findByUserId(UserId userId);

    /**
     * 更新认证用户
     */
    void update(AuthUser authUser);

    /**
     * 删除认证用户
     */
    void deleteByUserId(UserId userId);
}
