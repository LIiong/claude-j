-- User table
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(32) NOT NULL UNIQUE COMMENT '用户唯一标识',
    username VARCHAR(20) NOT NULL COMMENT '用户名',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态：ACTIVE/INACTIVE/FROZEN',
    invite_code VARCHAR(6) NOT NULL UNIQUE COMMENT '邀请码',
    inviter_id VARCHAR(32) COMMENT '邀请人用户ID',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_inviter_id (inviter_id),
    INDEX idx_invite_code (invite_code),
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- Order table
CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL COMMENT '订单号',
    customer_id VARCHAR(64) NOT NULL COMMENT '客户ID',
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT '订单状态',
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '订单总金额',
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_order_id (order_id)
);

-- Order item table
CREATE TABLE IF NOT EXISTS t_order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL COMMENT '订单号',
    product_id VARCHAR(64) NOT NULL COMMENT '商品ID',
    product_name VARCHAR(256) NOT NULL COMMENT '商品名称',
    quantity INT NOT NULL COMMENT '数量',
    unit_price DECIMAL(12,2) NOT NULL COMMENT '单价',
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    subtotal DECIMAL(12,2) NOT NULL COMMENT '小计',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_order_id (order_id)
);

-- Short link table (designed for 10M+ records)
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
);

-- Link management table
CREATE TABLE IF NOT EXISTS t_link (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '链接名称',
    url VARCHAR(500) NOT NULL COMMENT '链接地址',
    description VARCHAR(500) COMMENT '链接描述',
    category VARCHAR(50) COMMENT '分类',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_category (category)
);

-- Coupon table
CREATE TABLE IF NOT EXISTS t_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id VARCHAR(32) NOT NULL COMMENT '优惠券唯一业务标识',
    name VARCHAR(50) NOT NULL COMMENT '优惠券名称',
    discount_type VARCHAR(20) NOT NULL COMMENT '折扣类型：FIXED_AMOUNT/PERCENTAGE',
    discount_value DECIMAL(12,2) NOT NULL COMMENT '折扣值',
    min_order_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '最低订单金额门槛',
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE/USED/EXPIRED',
    user_id VARCHAR(64) NOT NULL COMMENT '所属用户ID',
    valid_from TIMESTAMP NOT NULL COMMENT '有效期开始',
    valid_until TIMESTAMP NOT NULL COMMENT '有效期截止',
    used_time TIMESTAMP NULL COMMENT '使用时间',
    used_order_id VARCHAR(64) NULL COMMENT '关联订单号',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    UNIQUE KEY uk_coupon_id (coupon_id),
    KEY idx_user_id (user_id),
    KEY idx_user_status (user_id, status)
);

-- Cart table
CREATE TABLE IF NOT EXISTS t_cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '购物车总金额',
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_user_id (user_id)
);

-- Cart item table
CREATE TABLE IF NOT EXISTS t_cart_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL COMMENT '购物车ID',
    product_id VARCHAR(64) NOT NULL COMMENT '商品ID',
    product_name VARCHAR(256) NOT NULL COMMENT '商品名称',
    quantity INT NOT NULL COMMENT '数量',
    unit_price DECIMAL(12,2) NOT NULL COMMENT '单价',
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    subtotal DECIMAL(12,2) NOT NULL COMMENT '小计',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_cart_id (cart_id),
    KEY idx_cart_product (cart_id, product_id)
);

-- Auth user table
CREATE TABLE IF NOT EXISTS t_auth_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(32) NOT NULL UNIQUE COMMENT '用户ID',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希（BCrypt）',
    email_verified TINYINT DEFAULT 0 COMMENT '邮箱是否验证：0-未验证，1-已验证',
    phone_verified TINYINT DEFAULT 0 COMMENT '手机是否验证：0-未验证，1-已验证',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/LOCKED/DISABLED',
    failed_login_attempts INT DEFAULT 0 COMMENT '连续登录失败次数',
    locked_until TIMESTAMP NULL COMMENT '锁定截止时间',
    last_login_at TIMESTAMP NULL COMMENT '最后登录时间',
    password_changed_at TIMESTAMP NULL COMMENT '密码修改时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status)
) COMMENT='认证用户表';

-- User session table
CREATE TABLE IF NOT EXISTS t_user_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE COMMENT '会话ID',
    user_id VARCHAR(32) NOT NULL COMMENT '用户ID',
    refresh_token VARCHAR(500) NOT NULL COMMENT '刷新令牌',
    device_info VARCHAR(500) COMMENT '设备信息（JSON）',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_user_id (user_id),
    INDEX idx_refresh_token (refresh_token)
) COMMENT='用户会话表';

-- Login log table
CREATE TABLE IF NOT EXISTS t_login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(32) COMMENT '用户ID（可能为空，如未注册用户尝试登录）',
    login_type VARCHAR(50) NOT NULL COMMENT '登录类型：PASSWORD/SMS/EMAIL/OAUTH',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    device_info VARCHAR(500) COMMENT '设备信息（JSON）',
    status VARCHAR(20) NOT NULL COMMENT '状态：SUCCESS/FAILED',
    fail_reason VARCHAR(255) COMMENT '失败原因',
    deleted INT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log_user_id (user_id),
    INDEX idx_create_time (create_time)
) COMMENT='登录日志表';

