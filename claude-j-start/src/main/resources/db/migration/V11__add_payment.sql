-- Payment DDL
CREATE TABLE IF NOT EXISTS t_payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id VARCHAR(64) NOT NULL UNIQUE COMMENT '业务支付ID',
    order_id VARCHAR(64) NOT NULL COMMENT '关联订单ID',
    customer_id VARCHAR(64) NOT NULL COMMENT '客户ID',
    amount DECIMAL(19,2) NOT NULL COMMENT '支付金额',
    currency VARCHAR(16) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    status VARCHAR(32) NOT NULL COMMENT '支付状态',
    method VARCHAR(32) NOT NULL COMMENT '支付方式',
    transaction_no VARCHAR(128) COMMENT '第三方交易号',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',

    INDEX idx_order_id (order_id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_transaction_no (transaction_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付表';