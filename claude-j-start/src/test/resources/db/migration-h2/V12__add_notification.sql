-- Notification DDL (H2 compatible)
CREATE TABLE IF NOT EXISTS t_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id VARCHAR(64) NOT NULL UNIQUE,
    order_id VARCHAR(64) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    payload_json CLOB NOT NULL,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_notification_order_channel ON t_notification(order_id, channel);
