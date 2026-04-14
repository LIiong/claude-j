package com.claudej.infrastructure.link.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 链接数据对象
 */
@Data
@TableName("t_link")
public class LinkDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String url;

    private String description;

    private String category;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
