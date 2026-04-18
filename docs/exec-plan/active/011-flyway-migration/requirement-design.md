# 需求拆分设计 — 011-flyway-migration

## 需求描述

引入 Flyway 替代手写 schema.sql，实现数据库版本化管理：
1. 父 pom 添加 flyway-core（兼容 Spring Boot 2.7）
2. 将现有 schema.sql 按聚合切分为 V1-V7 migration 文件放入 db/migration/
3. 修改 application-dev.yml 启用 flyway，移除 spring.sql.init 配置
4. 新增 docs/ops/db-migrations.md 说明命名规范与新增聚合模板
5. CI 新增 flyway:validate job

## 领域分析

本任务不涉及业务领域模型变更，属于基础设施迁移。需要分析现有 schema.sql 的表归属：

### 聚合映射分析

| 版本 | 聚合 | 包含表 | 说明 |
|------|------|--------|------|
| V1 | user | t_user | 用户基础信息 |
| V2 | order | t_order, t_order_item | 订单与订单项 |
| V3 | shortlink | t_short_link | 短链接服务 |
| V4 | link | t_link | 链接管理 |
| V5 | coupon | t_coupon | 优惠券 |
| V6 | cart | t_cart, t_cart_item | 购物车与购物项 |
| V7 | auth | t_auth_user, t_user_session, t_login_log | 认证与会话 |

### 表依赖关系
- 无跨聚合外键依赖（符合 DDD 聚合边界原则）
- t_order_item 依赖 t_order（order_id，业务字段，非 DB 外键）
- t_cart_item 依赖 t_cart（cart_id，业务字段，非 DB 外键）
- t_user_session 依赖 t_auth_user（user_id，业务字段）

## 关键算法/技术方案

### Flyway 版本策略

采用**线性版本号**（V1, V2...V7），按聚合初始化顺序排列：
- V1__user_init.sql
- V2__order_init.sql
- V3__shortlink_init.sql
- V4__link_init.sql
- V5__coupon_init.sql
- V6__cart_init.sql
- V7__auth_init.sql

### 命名规范

迁移文件命名遵循 Flyway 标准：`V{version}__{description}.sql`
- version: 整数，递增
- description: 下划线分隔的简短描述

### 未来新增聚合模板

新增聚合应在现有 V7 基础上递增：
```
V8__inventory_init.sql
V9__payment_init.sql
```

### Spring Boot 2.7 Flyway 配置

Spring Boot 2.7 内置 Flyway 7.x，配置项：
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
```

### 配置迁移策略

1. 移除 `spring.sql.init.mode=always`（避免重复执行 schema.sql）
2. 保留 H2 console 配置用于开发调试
3. Flyway 默认在 Spring Boot 启动时自动执行

## API 设计

本任务不涉及 REST API 变更。

## 数据库设计（DDL 迁移）

### V1__user_init.sql
```sql
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
```

### V2__order_init.sql
```sql
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
```

### V3__shortlink_init.sql
```sql
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
```

### V4__link_init.sql
```sql
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='链接管理表';
```

### V5__coupon_init.sql
```sql
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';
```

### V6__cart_init.sql
```sql
CREATE TABLE IF NOT EXISTS t_cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '购物车总金额',
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车项表';
```

### V7__auth_init.sql
```sql
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
```

## 影响范围

### claude-j-start（修改）
- `pom.xml` - 添加 flyway-core 依赖
- `src/main/resources/application-dev.yml` - 启用 flyway，移除 sql.init
- `src/main/resources/db/schema.sql` - 删除（迁移到 db/migration/）
- `src/main/resources/db/migration/` - 新增 V1-V7 迁移文件

### docs/ops/（新增）
- `db-migrations.md` - 迁移规范文档

### CI/CD（新增）
- GitHub Actions workflow 添加 flyway:validate job

## 验收标准

1. 空库启动自动建表：删除 H2 内存库后重启应用，所有表自动创建
2. flyway_schema_history 表可见 7 条成功记录
3. 修改历史 migration 后 validate 失败：人为修改 V1 文件内容，启动时报错
4. 三项预飞检查通过：mvn test / checkstyle / entropy-check
