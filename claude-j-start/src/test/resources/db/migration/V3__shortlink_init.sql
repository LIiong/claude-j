CREATE TABLE IF NOT EXISTS t_short_link (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_code VARCHAR(6) COMMENT '短链接码',
    original_url VARCHAR(2048) NOT NULL COMMENT '原始URL',
    original_url_hash VARCHAR(64) NOT NULL COMMENT '原始URL的SHA-256哈希，用于去重',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    expire_time TIMESTAMP NULL COMMENT '过期时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_short_code (short_code),
    KEY idx_original_url_hash (original_url_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短链接表';
