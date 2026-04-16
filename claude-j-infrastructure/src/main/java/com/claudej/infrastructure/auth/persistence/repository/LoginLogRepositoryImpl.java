package com.claudej.infrastructure.auth.persistence.repository;

import com.claudej.domain.auth.model.entity.LoginLog;
import com.claudej.domain.auth.repository.LoginLogRepository;
import com.claudej.domain.user.model.valobj.UserId;
import com.claudej.infrastructure.auth.persistence.converter.LoginLogConverter;
import com.claudej.infrastructure.auth.persistence.dataobject.LoginLogDO;
import com.claudej.infrastructure.auth.persistence.mapper.LoginLogMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 登录日志 Repository 实现
 */
@Repository
public class LoginLogRepositoryImpl implements LoginLogRepository {

    private final LoginLogMapper loginLogMapper;
    private final LoginLogConverter loginLogConverter;

    public LoginLogRepositoryImpl(LoginLogMapper loginLogMapper, LoginLogConverter loginLogConverter) {
        this.loginLogMapper = loginLogMapper;
        this.loginLogConverter = loginLogConverter;
    }

    @Override
    public LoginLog save(LoginLog loginLog) {
        LoginLogDO loginLogDO = loginLogConverter.toDO(loginLog);
        loginLogMapper.insert(loginLogDO);
        loginLog.setId(loginLogDO.getId());
        return loginLog;
    }

    @Override
    public List<LoginLog> findByUserId(UserId userId) {
        List<LoginLogDO> loginLogDOList = loginLogMapper.selectByUserId(userId.getValue());
        return loginLogDOList.stream()
                .map(loginLogConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<LoginLog> findRecentByUserId(UserId userId, int limit) {
        List<LoginLogDO> loginLogDOList = loginLogMapper.selectRecentByUserId(userId.getValue(), limit);
        return loginLogDOList.stream()
                .map(loginLogConverter::toDomain)
                .collect(Collectors.toList());
    }
}
