package com.claudej.infrastructure.auth.persistence.converter;

import com.claudej.domain.auth.model.aggregate.AuthUser;
import com.claudej.domain.auth.model.valobj.AuthStatus;
import com.claudej.domain.user.model.valobj.UserId;
import com.claudej.infrastructure.auth.persistence.dataobject.AuthUserDO;
import org.springframework.stereotype.Component;

/**
 * 认证用户转换器
 */
@Component
public class AuthUserConverter {

    /**
     * AuthUser DO 转 Domain
     */
    public AuthUser toDomain(AuthUserDO authUserDO) {
        if (authUserDO == null) {
            return null;
        }

        return AuthUser.reconstruct(
                authUserDO.getId(),
                new UserId(authUserDO.getUserId()),
                authUserDO.getPasswordHash(),
                Boolean.TRUE.equals(authUserDO.getEmailVerified()),
                Boolean.TRUE.equals(authUserDO.getPhoneVerified()),
                AuthStatus.valueOf(authUserDO.getStatus()),
                authUserDO.getFailedLoginAttempts() != null ? authUserDO.getFailedLoginAttempts() : 0,
                authUserDO.getLockedUntil(),
                authUserDO.getLastLoginAt(),
                authUserDO.getPasswordChangedAt(),
                authUserDO.getCreateTime(),
                authUserDO.getUpdateTime()
        );
    }

    /**
     * AuthUser Domain 转 DO
     */
    public AuthUserDO toDO(AuthUser authUser) {
        if (authUser == null) {
            return null;
        }

        AuthUserDO authUserDO = new AuthUserDO();
        authUserDO.setId(authUser.getId());
        authUserDO.setUserId(authUser.getUserIdValue());
        authUserDO.setPasswordHash(authUser.getPasswordHash());
        authUserDO.setEmailVerified(authUser.isEmailVerified());
        authUserDO.setPhoneVerified(authUser.isPhoneVerified());
        authUserDO.setStatus(authUser.getStatus().name());
        authUserDO.setFailedLoginAttempts(authUser.getFailedLoginAttempts());
        authUserDO.setLockedUntil(authUser.getLockedUntil());
        authUserDO.setLastLoginAt(authUser.getLastLoginAt());
        authUserDO.setPasswordChangedAt(authUser.getPasswordChangedAt());
        authUserDO.setCreateTime(authUser.getCreateTime());
        authUserDO.setUpdateTime(authUser.getUpdateTime());
        authUserDO.setDeleted(0);
        return authUserDO;
    }
}
