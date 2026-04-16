package com.claudej.domain.auth.repository;

import com.claudej.domain.auth.model.entity.LoginLog;
import com.claudej.domain.user.model.valobj.UserId;

import java.util.List;

/**
 * 登录日志 Repository 端口接口
 */
public interface LoginLogRepository {

    /**
     * 保存登录日志
     */
    LoginLog save(LoginLog loginLog);

    /**
     * 查询用户的登录日志
     */
    List<LoginLog> findByUserId(UserId userId);

    /**
     * 查询用户的最近N条登录日志
     */
    List<LoginLog> findRecentByUserId(UserId userId, int limit);
}
