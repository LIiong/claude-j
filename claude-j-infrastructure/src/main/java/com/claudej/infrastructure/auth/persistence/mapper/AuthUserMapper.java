package com.claudej.infrastructure.auth.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.claudej.infrastructure.auth.persistence.dataobject.AuthUserDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 认证用户 Mapper
 */
public interface AuthUserMapper extends BaseMapper<AuthUserDO> {

    /**
     * 根据用户ID查询
     */
    @Select("SELECT * FROM t_auth_user WHERE user_id = #{userId} AND deleted = 0")
    AuthUserDO selectByUserId(@Param("userId") String userId);

    /**
     * 根据用户ID更新
     */
    @Update("UPDATE t_auth_user SET password_hash = #{passwordHash}, email_verified = #{emailVerified}, " +
            "phone_verified = #{phoneVerified}, status = #{status}, failed_login_attempts = #{failedLoginAttempts}, " +
            "locked_until = #{lockedUntil}, last_login_at = #{lastLoginAt}, password_changed_at = #{passwordChangedAt}, " +
            "update_time = NOW() WHERE user_id = #{userId}")
    int updateByUserId(AuthUserDO authUserDO);

    /**
     * 根据用户ID删除
     */
    @Update("UPDATE t_auth_user SET deleted = 1 WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") String userId);

    /**
     * 检查用户ID是否存在
     */
    @Select("SELECT COUNT(*) FROM t_auth_user WHERE user_id = #{userId} AND deleted = 0")
    boolean existsByUserId(@Param("userId") String userId);
}
