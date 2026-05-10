package com.claudej.infrastructure.notification.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.claudej.infrastructure.notification.persistence.dataobject.NotificationDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationMapper extends BaseMapper<NotificationDO> {
}
