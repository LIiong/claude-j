package com.claudej.infrastructure.notification.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.claudej.domain.notification.model.aggregate.Notification;
import com.claudej.domain.notification.model.valueobject.NotificationChannel;
import com.claudej.domain.notification.repository.NotificationRepository;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.infrastructure.notification.persistence.converter.NotificationConverter;
import com.claudej.infrastructure.notification.persistence.dataobject.NotificationDO;
import com.claudej.infrastructure.notification.persistence.mapper.NotificationMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationMapper notificationMapper;
    private final NotificationConverter notificationConverter;

    public NotificationRepositoryImpl(NotificationMapper notificationMapper,
                                      NotificationConverter notificationConverter) {
        this.notificationMapper = notificationMapper;
        this.notificationConverter = notificationConverter;
    }

    @Override
    public Notification save(Notification notification) {
        NotificationDO notificationDO = notificationConverter.toDO(notification);
        if (notificationDO.getId() == null) {
            notificationMapper.insert(notificationDO);
            notification.setId(notificationDO.getId());
            return notification;
        }
        notificationMapper.updateById(notificationDO);
        return notification;
    }

    @Override
    public Optional<Notification> findByOrderIdAndChannel(OrderId orderId, NotificationChannel channel) {
        LambdaQueryWrapper<NotificationDO> queryWrapper = new LambdaQueryWrapper<NotificationDO>()
                .eq(NotificationDO::getOrderId, orderId.getValue())
                .eq(NotificationDO::getChannel, channel.name())
                .last("limit 1");
        NotificationDO notificationDO = notificationMapper.selectOne(queryWrapper);
        if (notificationDO == null) {
            return Optional.empty();
        }
        return Optional.of(notificationConverter.toDomain(notificationDO));
    }
}
