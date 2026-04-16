package com.claudej.infrastructure.auth.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志数据对象
 */
@Data
@TableName("t_login_log")
public class LoginLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（可能为空，如未注册用户尝试登录）
     */
    private String userId;

    /**
     * 登录类型：PASSWORD/SMS/EMAIL/OAUTH
     */
    private String loginType;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 设备信息（JSON）
     */
    private String deviceInfo;

    /**
     * 状态：SUCCESS/FAILED
     */
    private String status;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}
