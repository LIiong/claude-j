package com.claudej.infrastructure.auth.persistence.repository;

import com.claudej.domain.auth.model.aggregate.AuthUser;
import com.claudej.domain.auth.repository.AuthUserRepository;
import com.claudej.domain.user.model.valobj.UserId;
import com.claudej.infrastructure.auth.persistence.converter.AuthUserConverter;
import com.claudej.infrastructure.auth.persistence.dataobject.AuthUserDO;
import com.claudej.infrastructure.auth.persistence.mapper.AuthUserMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 认证用户 Repository 实现
 */
@Repository
public class AuthUserRepositoryImpl implements AuthUserRepository {

    private final AuthUserMapper authUserMapper;
    private final AuthUserConverter authUserConverter;

    public AuthUserRepositoryImpl(AuthUserMapper authUserMapper, AuthUserConverter authUserConverter) {
        this.authUserMapper = authUserMapper;
        this.authUserConverter = authUserConverter;
    }

    @Override
    public AuthUser save(AuthUser authUser) {
        AuthUserDO authUserDO = authUserConverter.toDO(authUser);
        if (authUser.getId() == null) {
            authUserMapper.insert(authUserDO);
            authUser.setId(authUserDO.getId());
        } else {
            authUserMapper.updateByUserId(authUserDO);
        }
        return authUser;
    }

    @Override
    public Optional<AuthUser> findByUserId(UserId userId) {
        AuthUserDO authUserDO = authUserMapper.selectByUserId(userId.getValue());
        return Optional.ofNullable(authUserConverter.toDomain(authUserDO));
    }

    @Override
    public void update(AuthUser authUser) {
        AuthUserDO authUserDO = authUserConverter.toDO(authUser);
        authUserMapper.updateByUserId(authUserDO);
    }

    @Override
    public void deleteByUserId(UserId userId) {
        authUserMapper.deleteByUserId(userId.getValue());
    }
}
