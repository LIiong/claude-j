package com.claudej.domain.auth.repository;

import com.claudej.domain.auth.model.entity.UserSession;
import com.claudej.domain.auth.model.valobj.SessionId;
import com.claudej.domain.user.model.valobj.UserId;

import java.util.List;
import java.util.Optional;

/**
 * 用户会话 Repository 端口接口
 */
public interface UserSessionRepository {

    /**
     * 保存会话
     */
    UserSession save(UserSession session);

    /**
     * 根据会话ID查找
     */
    Optional<UserSession> findBySessionId(SessionId sessionId);

    /**
     * 根据刷新令牌查找
     */
    Optional<UserSession> findByRefreshToken(String refreshToken);

    /**
     * 查询用户的所有会话
     */
    List<UserSession> findByUserId(UserId userId);

    /**
     * 删除会话
     */
    void deleteBySessionId(SessionId sessionId);

    /**
     * 删除用户的所有会话
     */
    void deleteByUserId(UserId userId);

    /**
     * 删除过期会话
     */
    void deleteExpiredSessions();
}
