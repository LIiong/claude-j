package com.claudej.infrastructure.link.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.claudej.infrastructure.link.persistence.dataobject.LinkDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 链接 Mapper
 */
@Mapper
public interface LinkMapper extends BaseMapper<LinkDO> {
}
