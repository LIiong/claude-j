# 需求拆分设计 — 005-user-management

## 需求描述

为用户系统新增用户管理功能，支持通过短链邀请用户注册、用户查看自己的订单列表、以及分享商品订单链接。

## 领域分析

### 聚合根: User

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 数据库自增ID |
| userId | UserId | 用户唯一标识（值对象） |
| username | Username | 用户名（值对象） |
| email | Email | 邮箱（值对象，可选） |
| phone | Phone | 手机号（值对象，可选） |
| status | UserStatus | 用户状态（值对象） |
| inviteCode | InviteCode | 邀请码（值对象） |
| inviterId | UserId | 邀请人ID（值对象，可选） |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

### 值对象

- **UserId**: 用户唯一标识，String 类型，不可变，格式：UR + 16位随机字母数字
- **Username**: 用户名，2-20字符，不可变
- **Email**: 邮箱地址，符合邮箱格式，可选
- **Phone**: 手机号，11位数字，可选
- **UserStatus**: 用户状态枚举，ACTIVE/INACTIVE/FROZEN
- **InviteCode**: 邀请码，6位字母数字组合，唯一，不可变

### 领域服务（如有）

- **InviteCodeGenerator**: 邀请码生成服务接口
  - `String generate()`: 生成唯一邀请码

- **UserInviteService**: 用户邀请领域服务
  - `void recordInvitation(User invitee, UserId inviterId)`: 记录邀请关系

### 端口接口

- **UserRepository**: 用户仓储接口
  - `User save(User user)`: 保存用户
  - `User findById(UserId userId)`: 根据ID查询
  - `User findByUsername(Username username)`: 根据用户名查询
  - `User findByInviteCode(InviteCode inviteCode)`: 根据邀请码查询
  - `List<User> findByInviterId(UserId inviterId)`: 查询邀请的用户列表
  - `boolean existsByUsername(Username username)`: 检查用户名是否存在
  - `boolean existsByInviteCode(InviteCode inviteCode)`: 检查邀请码是否存在

- **InviteCodeGenerator**: 邀请码生成器（领域服务端口）
  - `String generate()`: 生成唯一邀请码

## 关键算法/技术方案

### 邀请码生成策略

采用6位字母数字组合（排除易混淆字符如0/O, 1/I/l），保证唯一性：

1. 使用 Base32 编码（字符集：23456789ABCDEFGHJKLMNPQRSTUVWXYZ）
2. 生成后检查数据库是否已存在，如存在则重新生成
3. 用户创建时自动生成唯一邀请码

### 邀请链接生成

1. 用户调用生成邀请链接 API
2. 系统生成携带用户邀请码的原始URL（如：`https://example.com/register?code=ABC123`）
3. 调用 ShortLink 服务生成短链
4. 返回短链给用户

### 用户订单查询

1. 用户通过 userId 查询自己的订单列表
2. 调用 Order 聚合的查询接口（通过 application 层协调）
3. 返回订单列表（不包含其他用户订单）

### 订单链接分享

1. 用户选择要分享的订单
2. 生成订单详情页原始URL（如：`https://example.com/orders/{orderId}`）
3. 调用 ShortLink 服务生成短链
4. 返回短链给用户

## API 设计

### 用户管理 API

| 方法 | 路径 | 描述 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| POST | /api/v1/users | 注册用户 | `{ "username": "xxx", "email": "xxx", "phone": "xxx", "inviteCode": "xxx" }` | `{ "success": true, "data": { "userId": "...", "username": "...", "inviteCode": "..." } }` |
| GET | /api/v1/users/{userId} | 查询用户 | — | `{ "success": true, "data": { "userId": "...", "username": "...", "status": "..." } }` |
| POST | /api/v1/users/{userId}/invite-link | 生成邀请链接 | — | `{ "success": true, "data": { "shortCode": "...", "shortUrl": "..." } }` |

### 用户订单管理 API

| 方法 | 路径 | 描述 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| GET | /api/v1/users/{userId}/orders | 查询用户订单列表 | — | `{ "success": true, "data": [ { "orderId": "...", "status": "...", "totalAmount": "..." } ] }` |
| GET | /api/v1/users/{userId}/orders/{orderId} | 查询订单详情 | — | `{ "success": true, "data": { "orderId": "...", "items": [...], "status": "..." } }` |
| POST | /api/v1/users/{userId}/orders/{orderId}/share | 生成订单分享链接 | — | `{ "success": true, "data": { "shortCode": "...", "shortUrl": "..." } }` |

### 邀请用户 API

| 方法 | 路径 | 描述 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| POST | /api/v1/invites/validate | 验证邀请码 | `{ "inviteCode": "xxx" }` | `{ "success": true, "data": { "valid": true, "inviter": {...} } }` |
| GET | /api/v1/invites/{inviteCode}/users | 查询被邀请用户列表 | — | `{ "success": true, "data": [ { "userId": "...", "username": "...", "createTime": "..." } ] }` |

## 数据库设计

```sql
-- 用户表
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

## 影响范围

### domain
- `com.claudej.domain.user.model.aggregate.User` - 用户聚合根
- `com.claudej.domain.user.model.valobj.UserId` - 用户ID值对象
- `com.claudej.domain.user.model.valobj.Username` - 用户名值对象
- `com.claudej.domain.user.model.valobj.Email` - 邮箱值对象
- `com.claudej.domain.user.model.valobj.Phone` - 手机号值对象
- `com.claudej.domain.user.model.valobj.UserStatus` - 用户状态枚举
- `com.claudej.domain.user.model.valobj.InviteCode` - 邀请码值对象
- `com.claudej.domain.user.repository.UserRepository` - 用户仓储接口
- `com.claudej.domain.user.service.InviteCodeGenerator` - 邀请码生成器接口

### application
- `com.claudej.domain.user.command.CreateUserCommand` - 创建用户命令
- `com.claudej.domain.user.command.GenerateInviteLinkCommand` - 生成邀请链接命令
- `com.claudej.domain.user.dto.UserDTO` - 用户DTO
- `com.claudej.domain.user.dto.UserOrderDTO` - 用户订单DTO
- `com.claudej.domain.user.assembler.UserAssembler` - 用户组装器
- `com.claudej.domain.user.service.UserApplicationService` - 用户应用服务
- `com.claudej.domain.user.service.UserOrderQueryService` - 用户订单查询服务

### infrastructure
- `com.claudej.infrastructure.user.persistence.dataobject.UserDO` - 用户DO
- `com.claudej.infrastructure.user.persistence.mapper.UserMapper` - 用户Mapper
- `com.claudej.infrastructure.user.persistence.converter.UserConverter` - 用户转换器
- `com.claudej.infrastructure.user.persistence.repository.UserRepositoryImpl` - 用户仓储实现
- `com.claudej.infrastructure.user.service.InviteCodeGeneratorImpl` - 邀请码生成器实现

### adapter
- `com.claudej.adapter.user.web.UserController` - 用户控制器
- `com.claudej.adapter.user.web.request.CreateUserRequest` - 创建用户请求
- `com.claudej.adapter.user.web.request.ValidateInviteCodeRequest` - 验证邀请码请求
- `com.claudej.adapter.user.web.response.UserResponse` - 用户响应
- `com.claudej.adapter.user.web.response.InviteLinkResponse` - 邀请链接响应

### start
- `claude-j-start/src/main/resources/db/schema.sql` - 添加用户表DDL

## 与现有聚合的交互

### 与 ShortLink 聚合
- 生成邀请链接时，调用 ShortLinkApplicationService 创建短链
- 分享订单链接时，调用 ShortLinkApplicationService 创建短链

### 与 Order 聚合
- 查询用户订单时，通过 UserOrderQueryService 调用 OrderApplicationService 查询（非直接访问 Repository）
- 订单分享时验证订单存在性和归属
- **注意**：Order 中的 CustomerId 应与 User 中的 UserId 对应，通过 customerId.getValue().equals(userId.getValue()) 进行匹配

## 架构评审

### 评审结论：changes-requested → approved（已修正）

#### 评审发现及修正

| # | 问题 | 状态 | 修正内容 |
|---|------|------|----------|
| 1 | 邀请码字段长度不匹配 | ✅ 已修正 | DDL 中 invite_code VARCHAR(10) → VARCHAR(6)，与设计文档一致 |
| 2 | 缺少逻辑删除字段 | ✅ 已修正 | 添加 deleted INT NOT NULL DEFAULT 0，与其他表保持一致 |
| 3 | 跨聚合查询方式 | ✅ 已澄清 | UserOrderQueryService 仅作为查询服务，通过 OrderApplicationService 查询订单，不直接访问 OrderRepository |
| 4 | CustomerId 与 UserId 映射 | ✅ 已确认 | Order.customerId 与 User.userId 为同一业务标识，通过值对象 equals 比较 |

#### 值对象实现要求

参考现有聚合实现：
- 所有值对象使用 `final class` + `final` 字段
- UserStatus 参考 OrderStatus 添加状态转换检查方法（如 toFrozen() 等）
- 值对象需重写 equals/hashCode

#### 新增 ErrorCode

需在 `ErrorCode.java` 中添加：
- USER_NOT_FOUND("U001", "用户不存在")
- USERNAME_ALREADY_EXISTS("U002", "用户名已存在")
- INVALID_INVITE_CODE("U003", "无效的邀请码")
- INVITE_CODE_ALREADY_USED("U004", "邀请码已被使用")
- USER_ALREADY_REGISTERED("U005", "用户已注册")

#### 邀请码生成策略确认

- Base32 字符集：23456789ABCDEFGHJKLMNPQRSTUVWXYZ（排除 0/O, 1/I/l）
- 生成逻辑：循环生成 → 检查唯一性 → 数据库唯一索引兜底
- 实现位置：InviteCodeGeneratorImpl（infrastructure 层）

#### 总体评价

设计符合 DDD 六边形架构要求，聚合边界清晰，与 ShortLink、Order 聚合的交互设计合理。已修正 DDL 问题，可以进入编码阶段。
