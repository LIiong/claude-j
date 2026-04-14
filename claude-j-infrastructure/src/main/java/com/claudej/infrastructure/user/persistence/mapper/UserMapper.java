package com.claudej.infrastructure.user.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.claudej.infrastructure.user.persistence.dataobject.UserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

    /**
     * 根据用户ID查询
     */
    @Select("SELECT * FROM t_user WHERE user_id = #{userId} AND deleted = 0")
    UserDO selectByUserId(@Param("userId") String userId);

    /**
     * 根据用户名查询
     */
    @Select("SELECT * FROM t_user WHERE username = #{username} AND deleted = 0")
    UserDO selectByUsername(@Param("username") String username);

    /**
     * 根据邀请码查询
     */
    @Select("SELECT * FROM t_user WHERE invite_code = #{inviteCode} AND deleted = 0")
    UserDO selectByInviteCode(@Param("inviteCode") String inviteCode);

    /**
     * 根据邀请人ID查询列表
     */
    @Select("SELECT * FROM t_user WHERE inviter_id = #{inviterId} AND deleted = 0")
    List<UserDO> selectByInviterId(@Param("inviterId") String inviterId);

    /**
     * 检查用户名是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM t_user WHERE username = #{username} AND deleted = 0")
    boolean existsByUsername(@Param("username") String username);

    /**
     * 检查邀请码是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM t_user WHERE invite_code = #{inviteCode} AND deleted = 0")
    boolean existsByInviteCode(@Param("inviteCode") String inviteCode);
}
