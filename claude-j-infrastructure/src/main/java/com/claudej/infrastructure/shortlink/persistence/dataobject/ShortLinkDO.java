package com.claudej.infrastructure.shortlink.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_short_link")
public class ShortLinkDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String shortCode;

    private String originalUrl;

    private String originalUrlHash;

    private LocalDateTime createTime;

    private LocalDateTime expireTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
