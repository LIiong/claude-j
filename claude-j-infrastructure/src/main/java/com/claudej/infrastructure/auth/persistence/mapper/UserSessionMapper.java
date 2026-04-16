package com.claudej.infrastructure.auth.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.claudej.infrastructure.auth.persistence.dataobject.UserSessionDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户会话 Mapper
 */
public interface UserSessionMapper extends BaseMapper<UserSessionDO> {

    /**
     * 根据会话ID查询
     */
    @Select("SELECT * FROM t_user_session WHERE session_id = #{sessionId} AND deleted = 0")
    UserSessionDO selectBySessionId(@Param("sessionId") String sessionId);

    /**
     * 根据刷新令牌查询
     */
    @Select("SELECT * FROM t_user_session WHERE refresh_token = #{refreshToken} AND deleted = 0")
    UserSessionDO selectByRefreshToken(@Param("refreshToken") String refreshToken);

    /**
     * 根据用户ID查询所有会话
     */
    @Select("SELECT * FROM t_user_session WHERE user_id = #{userId} AND deleted = 0")
    List<UserSessionDO> selectByUserId(@Param("userId") String userId);

    /**
     * 根据会话ID删除
     */
    @Delete("UPDATE t_user_session SET deleted = 1 WHERE session_id = #{sessionId}")
    int deleteBySessionId(@Param("sessionId") String sessionId);

    /**
     * 根据用户ID删除所有会话
     */
    @Delete("UPDATE t_user_session SET deleted = 1 WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") String userId);

    /**
     * 删除过期会话
     */
    @Delete("UPDATE t_user_session SET deleted = 1 WHERE expires_at < #{now}")
    int deleteExpiredSessions(@Param("now") LocalDateTime now);
}
