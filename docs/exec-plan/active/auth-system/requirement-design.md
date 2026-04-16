# 需求拆分设计 — 009-auth-system

## 需求描述

实现用户认证系统（Auth System），支持用户注册、登录（密码/短信）、登出、Token刷新、密码修改/重置等核心认证功能。基于 DDD + 六边形架构，与现有 user 聚合协作完成用户身份管理。

---

## 领域分析

### 聚合划分

**认证聚合 (auth)** - 新增聚合
- 与 **user 聚合** 通过 `UserId` 值对象关联（弱耦合）
- 职责边界：user 聚合管理用户基本信息，auth 聚合管理用户认证凭证和会话

### 聚合根: AuthUser

```java
// 核心属性
- id: Long                              // 数据库自增ID
- userId: UserId                        // 关联用户ID（值对象）
- passwordHash: String                   // 密码哈希（BCrypt）
- emailVerified: boolean                 // 邮箱是否验证
- phoneVerified: boolean                 // 手机是否验证
- status: AuthStatus                    // 状态（ACTIVE/LOCKED/DISABLED）
- failedLoginAttempts: int               // 连续登录失败次数
- lockedUntil: LocalDateTime            // 锁定截止时间
- lastLoginAt: LocalDateTime            // 最后登录时间
- passwordChangedAt: LocalDateTime      // 密码修改时间
```

**核心行为**：
- `create()` - 工厂方法创建认证用户
- `validateCanLogin()` - 验证是否允许登录（检查锁定/禁用状态）
- `recordLoginSuccess()` - 记录登录成功（重置失败计数）
- `recordLoginFailure()` - 记录登录失败（5次后锁定30分钟）
- `changePassword()` - 修改密码
- `verifyPassword()` - 验证密码哈希
- `unlock()` / `disable()` / `enable()` - 状态管理

### 实体: UserSession

```java
- id: Long
- sessionId: SessionId                  // 会话ID（值对象）
- userId: UserId                        // 所属用户
- refreshToken: String                   // 刷新令牌
- deviceInfo: DeviceInfo                // 设备信息（值对象）
- ipAddress: String                      // IP地址
- expiresAt: LocalDateTime              // 过期时间
- createTime: LocalDateTime             // 创建时间
```

**核心行为**：
- `create()` - 工厂方法创建会话
- `isExpired()` - 检查是否过期

### 实体: LoginLog

```java
- id: Long
- userId: UserId                        // 用户ID（可能为空）
- loginType: AuthProvider               // 登录方式
- ipAddress: String                      // IP地址
- deviceInfo: DeviceInfo                // 设备信息
- status: LoginStatus                   // 状态（SUCCESS/FAILED）
- failReason: String                     // 失败原因
- createTime: LocalDateTime             // 创建时间
```

### 值对象

| 值对象 | 约束 | 说明 |
|--------|------|------|
| **Password** | 8-128位，包含大小写字母+数字+特殊字符 | 仅用于验证明文密码强度，不存储 |
| **JwtToken** | accessToken + refreshToken + expiresIn | JWT 令牌对，不可变 |
| **UserId** | 已存在于 user 聚合 | 复用现有值对象 |
| **SessionId** | UUID 格式 | 会话唯一标识 |
| **DeviceInfo** | 设备类型、浏览器、OS | 设备指纹信息 |
| **AuthStatus** | ACTIVE / LOCKED / DISABLED | 用户状态枚举 |
| **AuthProvider** | PASSWORD/SMS/EMAIL/OAUTH | 认证方式枚举 |
| **LoginStatus** | SUCCESS / FAILED | 登录状态枚举 |

### 领域服务端口

**PasswordEncoder**（domain 定义，infrastructure 实现）
```java
- String encode(String rawPassword)
- boolean matches(String rawPassword, String encodedPassword)
```

**TokenService**（domain 定义，infrastructure 实现）
```java
- JwtToken generateTokenPair(UserId userId)
- boolean validateAccessToken(String accessToken)
- boolean validateRefreshToken(String refreshToken)
- UserId extractUserIdFromToken(String accessToken)
- JwtToken refreshAccessToken(String refreshToken)
```

### Repository 端口

**AuthUserRepository**
```java
- AuthUser save(AuthUser authUser)
- Optional<AuthUser> findByUserId(UserId userId)
- void update(AuthUser authUser)
- void deleteByUserId(UserId userId)
```

**UserSessionRepository**
```java
- UserSession save(UserSession session)
- Optional<UserSession> findByRefreshToken(String refreshToken)
- List<UserSession> findByUserId(UserId userId)
- void deleteBySessionId(SessionId sessionId)
- void deleteByUserId(UserId userId)
```

**LoginLogRepository**
```java
- LoginLog save(LoginLog loginLog)
- List<LoginLog> findByUserId(UserId userId)
```

---

## 关键算法/技术方案

### 1. 密码加密方案

**选型**：BCrypt (spring-security-crypto)
- Strength: 10（默认）
- 自动生成 salt，无需额外存储
- 与 Spring Security 兼容

**实现位置**：`infrastructure.auth.security.BCryptPasswordEncoderImpl`

### 2. JWT Token 方案

**选型**：jjwt 0.12.3（已在 pom.xml 中配置）

**Token 结构**：
- Access Token: 有效期 1 小时
- Refresh Token: 有效期 7 天
- 算法: HS256
- Claims: userId, iat, exp

**实现位置**：`infrastructure.auth.token.JwtTokenServiceImpl`

### 3. 登录失败锁定机制

**规则**：
- 连续失败 5 次后锁定账户
- 锁定持续时间：30 分钟
- 锁定期间所有登录尝试均拒绝
- 锁定到期后自动解锁

**实现位置**：`domain.auth.model.aggregate.AuthUser.recordLoginFailure()`

### 4. 会话管理

**规则**：
- 支持多设备同时登录
- Refresh Token 存储于数据库，可撤销
- 登出时删除会话记录
- 强制下线功能（删除用户所有会话）

---

## API 设计

### 认证相关

| 方法 | 路径 | 描述 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| POST | /api/v1/auth/register | 用户注册 | RegisterRequest | TokenResponse |
| POST | /api/v1/auth/login | 密码登录 | LoginRequest | TokenResponse |
| POST | /api/v1/auth/login/sms | 短信登录 | SmsLoginRequest | TokenResponse |
| POST | /api/v1/auth/logout | 用户登出 | LogoutRequest | void |
| POST | /api/v1/auth/refresh | 刷新Token | RefreshTokenRequest | TokenResponse |
| POST | /api/v1/auth/password/change | 修改密码 | ChangePasswordRequest | void |
| POST | /api/v1/auth/password/reset | 重置密码 | ResetPasswordRequest | void |

### 请求/响应结构

**RegisterRequest**
```json
{
  "email": "user@example.com",
  "phone": "13800138000",
  "password": "SecurePass123!",
  "verificationCode": "123456"
}
```

**LoginRequest**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "rememberMe": true
}
```

**TokenResponse**
```json
{
  "userId": "USR123456",
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 3600
}
```

---

## 数据库设计

### t_auth_user（认证用户表）

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
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认证用户表';
```

### t_user_session（用户会话表）

```sql
CREATE TABLE IF NOT EXISTS t_user_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE COMMENT '会话ID',
    user_id VARCHAR(32) NOT NULL COMMENT '用户ID',
    refresh_token VARCHAR(500) NOT NULL COMMENT '刷新令牌',
    device_info VARCHAR(500) COMMENT '设备信息（JSON）',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    INDEX idx_user_id (user_id),
    INDEX idx_refresh_token (refresh_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会话表';
```

### t_login_log（登录日志表）

```sql
CREATE TABLE IF NOT EXISTS t_login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(32) COMMENT '用户ID（可能为空，如未注册用户尝试登录）',
    login_type VARCHAR(50) NOT NULL COMMENT '登录类型：PASSWORD/SMS/EMAIL/OAUTH',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    device_info VARCHAR(500) COMMENT '设备信息（JSON）',
    status VARCHAR(20) NOT NULL COMMENT '状态：SUCCESS/FAILED',
    fail_reason VARCHAR(255) COMMENT '失败原因',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';
```

---

## 影响范围

### Domain 层（已存在）
- `com.claudej.domain.auth.model.aggregate.AuthUser`
- `com.claudej.domain.auth.model.entity.UserSession`
- `com.claudej.domain.auth.model.entity.LoginLog`
- `com.claudej.domain.auth.model.valobj.*`
- `com.claudej.domain.auth.repository.*`
- `com.claudej.domain.auth.service.PasswordEncoder`
- `com.claudej.domain.auth.service.TokenService`

### Application 层（已存在）
- `com.claudej.application.auth.service.AuthApplicationService`
- `com.claudej.application.auth.command.*`
- `com.claudej.application.auth.dto.*`
- `com.claudej.application.auth.assembler.*`

### Infrastructure 层（新增）
- `com.claudej.infrastructure.auth.persistence.AuthUserDO`
- `com.claudej.infrastructure.auth.persistence.UserSessionDO`
- `com.claudej.infrastructure.auth.persistence.LoginLogDO`
- `com.claudej.infrastructure.auth.persistence.AuthUserMapper`
- `com.claudej.infrastructure.auth.persistence.UserSessionMapper`
- `com.claudej.infrastructure.auth.persistence.LoginLogMapper`
- `com.claudej.infrastructure.auth.converter.AuthUserConverter`
- `com.claudej.infrastructure.auth.converter.UserSessionConverter`
- `com.claudej.infrastructure.auth.converter.LoginLogConverter`
- `com.claudej.infrastructure.auth.repository.AuthUserRepositoryImpl`
- `com.claudej.infrastructure.auth.repository.UserSessionRepositoryImpl`
- `com.claudej.infrastructure.auth.repository.LoginLogRepositoryImpl`
- `com.claudej.infrastructure.auth.security.BCryptPasswordEncoderImpl`
- `com.claudej.infrastructure.auth.token.JwtTokenServiceImpl`

### Adapter 层（新增）
- `com.claudej.adapter.auth.web.AuthController`
- `com.claudej.adapter.auth.web.request.RegisterRequest`
- `com.claudej.adapter.auth.web.request.LoginRequest`
- `com.claudej.adapter.auth.web.request.SmsLoginRequest`
- `com.claudej.adapter.auth.web.request.LogoutRequest`
- `com.claudej.adapter.auth.web.request.RefreshTokenRequest`
- `com.claudej.adapter.auth.web.request.ChangePasswordRequest`
- `com.claudej.adapter.auth.web.request.ResetPasswordRequest`
- `com.claudej.adapter.auth.web.response.TokenResponse`
- `com.claudej.adapter.auth.web.response.AuthUserResponse`
- `com.claudej.adapter.common.exception.GlobalExceptionHandler`（增强）

### Start 层（新增配置）
- `schema.sql` - 新增 auth 相关表 DDL
- `application.yml` - JWT 密钥与过期时间配置

---

## 技术适配说明

**原需求文档技术栈 vs 本项目技术栈**：

| 原建议 | 本项目实际 | 适配说明 |
|--------|-----------|----------|
| Node.js/Python/Go | Java 8 + Spring Boot 2.7.18 | 完全适配 |
| JWT + Refresh Token | jjwt 0.12.3 | 已配置在 pom.xml |
| bcrypt / Argon2 | spring-security-crypto BCrypt | 等效替代 |
| Prisma/SQLAlchemy | MyBatis-Plus 3.5.5 | 等效替代 |
| PostgreSQL/MySQL | H2(开发) / MySQL(生产) | 兼容 |

---

**设计人**: @dev
**设计时间**: 2026-04-16
**状态**: pending-review

---

## 架构评审

### 评审结论
通过，建议 minor 优化

### 评审检查项

#### 1. 聚合边界与循环依赖
- [x] **聚合根边界合理**：AuthUser 作为认证聚合根，职责清晰（管理凭证和状态）
- [x] **与 user 聚合解耦**：通过 UserId 值对象弱关联，符合 DDD 最佳实践
- [x] **无循环依赖**：auth 聚合仅依赖 user 聚合的 UserId VO，无反向依赖

#### 2. 六边形架构合规
- [x] **依赖方向正确**：adapter -> application -> domain <- infrastructure
- [x] **domain 纯净性**：AuthUser、UserSession、LoginLog 及值对象均无 Spring/MyBatis 依赖
- [x] **Repository 端口定义在 domain**：AuthUserRepository、UserSessionRepository、LoginLogRepository 均位于 domain.auth.repository
- [x] **领域服务端口正确定义**：PasswordEncoder、TokenService 在 domain.auth.service 定义，由 infrastructure 实现

#### 3. 对象边界
- [x] **Request/Response 仅在 adapter**：设计文档中明确列在 adapter 层
- [x] **DO 仅在 infrastructure**：设计文档中 DO 对象明确归属 infrastructure
- [x] **Domain 对象不直接暴露**：TokenResponseDTO 用于 API 响应
- [x] **DTO 转换链完整**：Request/Response <-> DTO <-> Domain <-> DO

#### 4. 领域建模
- [x] **聚合根封装不变量**：AuthUser 封装了登录锁定、密码修改、状态流转等所有业务规则
- [x] **值对象不可变**：Password、JwtToken、SessionId 均为 final 字段，通过工厂方法创建
- [x] **值对象相等语义**：均使用 @EqualsAndHashCode，符合要求
- [x] **登录失败锁定在 domain 实现**：recordLoginFailure() 方法在聚合根内实现，正确

#### 5. Repository 设计
- [x] **方法粒度合适**：save/findByUserId/update/delete 覆盖基本 CRUD 需求
- [ ] **建议**：AuthUserRepository.findByUserId 返回 Optional<AuthUser>，建议增加 findById(Long id) 用于内部关联查询

#### 6. DDL 设计
- [x] **表名规范**：t_auth_user、t_user_session、t_login_log 符合 t_{entity} 命名
- [x] **列名规范**：snake_case 命名
- [x] **索引合理**：idx_status、idx_user_id、idx_refresh_token、idx_create_time 覆盖查询场景
- [x] **字段映射**：与 Domain 模型字段对应清晰

#### 7. API 设计
- [x] **RESTful 规范**：路径清晰，HTTP 方法使用 POST（操作型语义）
- [x] **版本控制**：/api/v1 前缀
- [ ] **建议**：短信登录路径 /api/v1/auth/login/sms 符合资源语义，但可考虑统一为 /api/v1/auth/login?type=sms

#### 8. 安全性设计
- [x] **密码加密**：BCrypt strength=10 符合业界标准（默认即 10）
- [x] **JWT 配置**：HS256 算法，Access 1h / Refresh 7d 合理
- [x] **Refresh Token 存储**：存储于数据库支持撤销和多设备管理，是可接受的方案
- [x] **密码强度验证**：8-128位，大小写+数字+特殊字符，在 Password VO 中实现

### 评审意见

| 序号 | 问题 | 建议 | 严重程度 |
|------|------|------|----------|
| 1 | 暂无 UserSession 的更新方法 | UserSessionRepository 建议增加 update/deleteByRefreshToken 用于 Token 刷新场景 | minor |
| 2 | AuthUser.verifyPassword 实现 | 当前比较明文哈希，实际应由 PasswordEncoder.matches 在 application 层完成比较，设计正确 | info |
| 3 | JWT Token claims 设计 | 建议 access token claims 中包含 token 类型标识（typ: access）便于区分 | minor |

### ADR 建议

建议创建 ADR 记录以下架构决策：

1. **ADR-003: Auth 聚合与 User 聚合分离策略**
   - 决策：认证凭证与用户基本信息分属不同聚合
   - 理由：职责分离、独立演进、避免事务跨聚合

2. **ADR-004: Refresh Token 数据库存储方案**
   - 决策：Refresh Token 存储于数据库而非纯内存/Redis
   - 理由：支持 Token 撤销、多设备管理、审计追踪

### 评审人
architect

### 评审时间
2026-04-16
