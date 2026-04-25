package com.claudej.domain.user.model.aggregate;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.user.model.valobj.Email;
import com.claudej.domain.user.model.valobj.InviteCode;
import com.claudej.domain.user.model.valobj.Phone;
import com.claudej.domain.user.model.valobj.Role;
import com.claudej.domain.user.model.valobj.UserId;
import com.claudej.domain.user.model.valobj.UserStatus;
import com.claudej.domain.user.model.valobj.Username;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户聚合根 - 封装用户业务不变量
 */
@Getter
public class User {

    private Long id;
    private UserId userId;
    private Username username;
    private Email email;
    private Phone phone;
    private UserStatus status;
    private InviteCode inviteCode;
    private UserId inviterId;
    private Set<Role> roles;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private User(Username username, LocalDateTime createTime) {
        this.username = username;
        this.status = UserStatus.ACTIVE;
        this.roles = new HashSet<>(Arrays.asList(Role.USER));
        this.createTime = createTime;
        this.updateTime = createTime;
    }

    /**
     * 工厂方法：创建新用户
     */
    public static User create(Username username, InviteCode inviteCode) {
        if (username == null) {
            throw new BusinessException(ErrorCode.INVALID_USERNAME, "用户名不能为空");
        }
        if (inviteCode == null) {
            throw new BusinessException(ErrorCode.INVALID_INVITE_CODE, "邀请码不能为空");
        }
        User user = new User(username, LocalDateTime.now());
        user.userId = UserId.generate();
        user.inviteCode = inviteCode;
        return user;
    }

    /**
     * 从持久化层重建聚合根
     */
    public static User reconstruct(Long id, UserId userId, Username username,
                                   Email email, Phone phone, UserStatus status,
                                   InviteCode inviteCode, UserId inviterId, Set<Role> roles,
                                   LocalDateTime createTime, LocalDateTime updateTime) {
        User user = new User(username, createTime);
        user.id = id;
        user.userId = userId;
        user.email = email;
        user.phone = phone;
        user.status = status;
        user.inviteCode = inviteCode;
        user.inviterId = inviterId;
        user.roles = roles != null ? new HashSet<>(roles) : new HashSet<>(Arrays.asList(Role.USER));
        user.updateTime = updateTime;
        return user;
    }

    /**
     * 设置邮箱
     */
    public void setEmail(Email email) {
        this.email = email;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 设置手机号
     */
    public void setPhone(Phone phone) {
        this.phone = phone;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 设置邀请人
     */
    public void setInviter(UserId inviterId) {
        this.inviterId = inviterId;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 冻结用户
     */
    public void freeze() {
        this.status = this.status.toFrozen();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 解冻用户
     */
    public void unfreeze() {
        this.status = this.status.toActive();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 停用用户
     */
    public void deactivate() {
        this.status = this.status.toInactive();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 激活用户
     */
    public void activate() {
        this.status = this.status.toActive();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 设置数据库自增ID（持久化后回填）
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 便捷获取用户ID字符串值
     */
    public String getUserIdValue() {
        return userId.getValue();
    }

    /**
     * 便捷获取邀请码字符串值
     */
    public String getInviteCodeValue() {
        return inviteCode.getValue();
    }

    /**
     * 是否活跃
     */
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    /**
     * 是否冻结
     */
    public boolean isFrozen() {
        return status == UserStatus.FROZEN;
    }

    /**
     * 是否被邀请注册
     */
    public boolean isInvited() {
        return inviterId != null;
    }

    /**
     * 获取邀请人ID字符串值
     */
    public String getInviterIdValue() {
        return inviterId != null ? inviterId.getValue() : null;
    }

    /**
     * 是否拥有指定角色
     */
    public boolean hasRole(Role role) {
        return roles != null && roles.contains(role);
    }

    /**
     * 是否为管理员
     */
    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }

    /**
     * 添加角色
     */
    public void addRole(Role role) {
        if (roles == null) {
            roles = new HashSet<>();
        }
        roles.add(role);
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 移除角色（但不能移除最后一个角色）
     */
    public void removeRole(Role role) {
        if (roles == null || roles.size() <= 1) {
            // 用户必须至少有一个角色
            return;
        }
        roles.remove(role);
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 获取角色字符串（逗号分隔）
     */
    public String getRolesAsString() {
        if (roles == null || roles.isEmpty()) {
            return Role.USER.name();
        }
        return roles.stream()
                .map(Role::name)
                .collect(Collectors.joining(","));
    }
}
