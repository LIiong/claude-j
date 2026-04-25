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
