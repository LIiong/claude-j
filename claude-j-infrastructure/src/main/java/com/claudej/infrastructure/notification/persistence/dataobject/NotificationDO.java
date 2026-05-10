package com.claudej.infrastructure.notification.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_notification")
public class NotificationDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String notificationId;
    private String orderId;
    private String channel;
    private String status;
    private String payloadJson;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
