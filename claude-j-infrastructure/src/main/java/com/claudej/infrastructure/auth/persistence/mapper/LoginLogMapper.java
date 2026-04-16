package com.claudej.infrastructure.auth.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.claudej.infrastructure.auth.persistence.dataobject.LoginLogDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 登录日志 Mapper
 */
public interface LoginLogMapper extends BaseMapper<LoginLogDO> {

    /**
     * 根据用户ID查询登录日志
     */
    @Select("SELECT * FROM t_login_log WHERE user_id = #{userId} AND deleted = 0 ORDER BY create_time DESC")
    List<LoginLogDO> selectByUserId(@Param("userId") String userId);

    /**
     * 查询用户的最近N条登录日志
     */
    @Select("SELECT * FROM t_login_log WHERE user_id = #{userId} AND deleted = 0 ORDER BY create_time DESC LIMIT #{limit}")
    List<LoginLogDO> selectRecentByUserId(@Param("userId") String userId, @Param("limit") int limit);
}
