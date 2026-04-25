CREATE TABLE IF NOT EXISTS t_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(64) NOT NULL COMMENT '商品唯一业务标识',
    name VARCHAR(100) NOT NULL COMMENT '商品名称',
    description VARCHAR(500) NULL COMMENT '商品描述',
    sku_code VARCHAR(32) NOT NULL COMMENT 'SKU编码',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    original_price DECIMAL(19,2) NOT NULL COMMENT '原价',
    promotional_price DECIMAL(19,2) NULL COMMENT '促销价',
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/ACTIVE/INACTIVE',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    UNIQUE KEY uk_product_id (product_id),
    KEY idx_status (status),
    KEY idx_sku_code (sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';