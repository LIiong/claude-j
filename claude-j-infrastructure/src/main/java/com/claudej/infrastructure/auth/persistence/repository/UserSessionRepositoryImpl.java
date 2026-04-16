package com.claudej.infrastructure.auth.persistence.repository;

import com.claudej.domain.auth.model.entity.UserSession;
import com.claudej.domain.auth.model.valobj.SessionId;
import com.claudej.domain.auth.repository.UserSessionRepository;
import com.claudej.domain.user.model.valobj.UserId;
import com.claudej.infrastructure.auth.persistence.converter.UserSessionConverter;
import com.claudej.infrastructure.auth.persistence.dataobject.UserSessionDO;
import com.claudej.infrastructure.auth.persistence.mapper.UserSessionMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户会话 Repository 实现
 */
@Repository
public class UserSessionRepositoryImpl implements UserSessionRepository {

    private final UserSessionMapper userSessionMapper;
    private final UserSessionConverter userSessionConverter;

    public UserSessionRepositoryImpl(UserSessionMapper userSessionMapper, UserSessionConverter userSessionConverter) {
        this.userSessionMapper = userSessionMapper;
        this.userSessionConverter = userSessionConverter;
    }

    @Override
    public UserSession save(UserSession session) {
        UserSessionDO userSessionDO = userSessionConverter.toDO(session);
        if (session.getId() == null) {
            userSessionMapper.insert(userSessionDO);
            session.setId(userSessionDO.getId());
        } else {
            userSessionMapper.updateById(userSessionDO);
        }
        return session;
    }

    @Override
    public Optional<UserSession> findBySessionId(SessionId sessionId) {
        UserSessionDO userSessionDO = userSessionMapper.selectBySessionId(sessionId.getValue());
        return Optional.ofNullable(userSessionConverter.toDomain(userSessionDO));
    }

    @Override
    public Optional<UserSession> findByRefreshToken(String refreshToken) {
        UserSessionDO userSessionDO = userSessionMapper.selectByRefreshToken(refreshToken);
        return Optional.ofNullable(userSessionConverter.toDomain(userSessionDO));
    }

    @Override
    public List<UserSession> findByUserId(UserId userId) {
        List<UserSessionDO> userSessionDOList = userSessionMapper.selectByUserId(userId.getValue());
        return userSessionDOList.stream()
                .map(userSessionConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteBySessionId(SessionId sessionId) {
        userSessionMapper.deleteBySessionId(sessionId.getValue());
    }

    @Override
    public void deleteByUserId(UserId userId) {
        userSessionMapper.deleteByUserId(userId.getValue());
    }

    @Override
    public void deleteExpiredSessions() {
        userSessionMapper.deleteExpiredSessions(LocalDateTime.now());
    }
}
