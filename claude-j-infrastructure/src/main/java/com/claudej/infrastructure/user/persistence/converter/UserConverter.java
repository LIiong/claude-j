package com.claudej.infrastructure.user.persistence.converter;

import com.claudej.domain.user.model.aggregate.User;
import com.claudej.domain.user.model.valobj.Email;
import com.claudej.domain.user.model.valobj.InviteCode;
import com.claudej.domain.user.model.valobj.Phone;
import com.claudej.domain.user.model.valobj.Role;
import com.claudej.domain.user.model.valobj.UserId;
import com.claudej.domain.user.model.valobj.UserStatus;
import com.claudej.domain.user.model.valobj.Username;
import com.claudej.infrastructure.user.persistence.dataobject.UserDO;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User 转换器
 */
@Component
public class UserConverter {

    /**
     * User DO 转 Domain
     */
    public User toDomain(UserDO userDO) {
        if (userDO == null) {
            return null;
        }

        Email email = userDO.getEmail() != null ? new Email(userDO.getEmail()) : null;
        Phone phone = userDO.getPhone() != null ? new Phone(userDO.getPhone()) : null;
        UserId inviterId = userDO.getInviterId() != null ? new UserId(userDO.getInviterId()) : null;
        Set<Role> roles = parseRoles(userDO.getRoles());

        return User.reconstruct(
                userDO.getId(),
                new UserId(userDO.getUserId()),
                new Username(userDO.getUsername()),
                email,
                phone,
                UserStatus.valueOf(userDO.getStatus()),
                new InviteCode(userDO.getInviteCode()),
                inviterId,
                roles,
                userDO.getCreateTime(),
                userDO.getUpdateTime()
        );
    }

    /**
     * 解析角色字符串为 Role 集合
     */
    private Set<Role> parseRoles(String rolesStr) {
        if (rolesStr == null || rolesStr.isEmpty()) {
            Set<Role> defaultRoles = new HashSet<>();
            defaultRoles.add(Role.USER);
            return defaultRoles;
        }
        return Arrays.stream(rolesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Role::valueOf)
                .collect(Collectors.toSet());
    }

    /**
     * User Domain 转 DO
     */
    public UserDO toDO(User user) {
        if (user == null) {
            return null;
        }
        UserDO userDO = new UserDO();
        userDO.setId(user.getId());
        userDO.setUserId(user.getUserIdValue());
        userDO.setUsername(user.getUsername().getValue());
        userDO.setEmail(user.getEmail() != null ? user.getEmail().getValue() : null);
        userDO.setPhone(user.getPhone() != null ? user.getPhone().getValue() : null);
        userDO.setStatus(user.getStatus().name());
        userDO.setInviteCode(user.getInviteCodeValue());
        userDO.setInviterId(user.getInviterIdValue());
        userDO.setRoles(user.getRolesAsString());
        userDO.setCreateTime(user.getCreateTime());
        userDO.setUpdateTime(user.getUpdateTime());
        userDO.setDeleted(0);
        return userDO;
    }
}
