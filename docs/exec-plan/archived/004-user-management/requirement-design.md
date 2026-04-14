# 需求拆分设计 — 004-user-management

## 需求描述
实现用户管理功能，支持用户的注册、登录、信息修改、密码重置。用于系统用户账号的生命周期管理。

## 领域分析

### 聚合根: User
- id (Long) — 数据库自增ID
- username (Username) — 用户名，必填，唯一，3-20字符
- email (Email) — 邮箱，必填，唯一，符合邮箱格式
- password (Password) — 密码，必填，加密存储
- status (UserStatus) — 状态：ACTIVE/INACTIVE/LOCKED
- createTime (LocalDateTime) — 创建时间
- updateTime (LocalDateTime) — 更新时间

### 值对象
- **Username**: 用户名，3-20字符，只允许字母数字下划线，唯一
- **Email**: 邮箱地址，符合RFC 5322格式，唯一
- **Password**: 密码，8-32字符，必须包含大小写字母和数字
- **UserStatus**: 枚举值：ACTIVE(正常)、INACTIVE(未激活)、LOCKED(锁定)

### 端口接口
- **UserRepository**:
  - `User save(User user)` — 保存用户
  - `Optional<User> findById(Long id)` — 根据ID查询
  - `Optional<User> findByUsername(Username username)` — 根据用户名查询
  - `Optional<User> findByEmail(Email email)` — 根据邮箱查询
  - `boolean existsByUsername(Username username)` — 检查用户名是否存在
  - `boolean existsByEmail(Email email)` — 检查邮箱是否存在

## 关键算法/技术方案
- 密码使用 BCrypt 加密存储
- 使用 JWT 进行登录态管理（Token 有效期 24 小时）
- 登录失败 5 次自动锁定账号 30 分钟

## API 设计

| 方法 | 路径 | 描述 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| POST | /api/v1/users | 注册用户 | `{ "username": "", "email": "", "password": "" }` | `{ "success": true, "data": {...} }` |
| POST | /api/v1/users/login | 用户登录 | `{ "username": "", "password": "" }` | `{ "success": true, "data": { "token": "..." } }` |
| GET | /api/v1/users/{id} | 查询用户 | — | `{ "success": true, "data": {...} }` |
| PUT | /api/v1/users/{id} | 更新信息 | `{ "email": "", ... }` | `{ "success": true, "data": {...} }` |
| POST | /api/v1/users/{id}/password | 修改密码 | `{ "oldPassword": "", "newPassword": "" }` | `{ "success": true }` |

## 数据库设计

```sql
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(20) NOT NULL COMMENT '用户名',
    email VARCHAR(100) NOT NULL COMMENT '邮箱',
    password_hash VARCHAR(100) NOT NULL COMMENT '密码哈希',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email)
);
```

## 影响范围
- **domain**: User、Username、Email、Password、UserStatus、UserRepository
- **application**: RegisterUserCommand、LoginCommand、UpdateUserCommand、ChangePasswordCommand、UserDTO、UserAssembler、UserApplicationService
- **infrastructure**: UserDO、UserMapper、UserConverter、UserRepositoryImpl、PasswordEncoder
- **adapter**: RegisterUserRequest、LoginRequest、UpdateUserRequest、UserResponse、UserController
- **start**: schema.sql