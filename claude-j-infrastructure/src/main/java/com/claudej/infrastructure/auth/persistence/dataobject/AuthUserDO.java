package com.claudej.infrastructure.auth.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 认证用户数据对象
 */
@Data
@TableName("t_auth_user")
public class AuthUserDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 密码哈希（BCrypt）
     */
    private String passwordHash;

    /**
     * 邮箱是否验证：0-未验证，1-已验证
     */
    private Boolean emailVerified;

    /**
     * 手机是否验证：0-未验证，1-已验证
     */
    private Boolean phoneVerified;

    /**
     * 状态：ACTIVE/LOCKED/DISABLED
     */
    private String status;

    /**
     * 连续登录失败次数
     */
    private Integer failedLoginAttempts;

    /**
     * 锁定截止时间
     */
    private LocalDateTime lockedUntil;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginAt;

    /**
     * 密码修改时间
     */
    private LocalDateTime passwordChangedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}
