---
task-id: 009-auth-system
from: architect
to: dev
status: approved
---

## 任务背景

实现用户认证系统（Auth System），包括用户注册、登录、登出、JWT Token 认证等核心功能。

## 需求来源

docs/auth-requirements.md

## 设计摘要

### 聚合边界

**auth 聚合**（新增）：
- 聚合根：`AuthUser` - 管理用户认证凭证和状态
- 实体：`UserSession`、`LoginLog`
- 值对象：`Password`、`JwtToken`、`AuthStatus` 等

**与 user 聚合关系**：
- 通过 `UserId` 值对象弱关联
- user 聚合管理用户基本信息，auth 聚合管理认证凭证

### 技术方案

| 组件 | 选型 | 说明 |
|------|------|------|
| 密码加密 | Spring Security BCryptPasswordEncoder | strength=10 |
| JWT Token | jjwt 0.12.3（已配置） | HS256, AccessToken 1h, RefreshToken 7d |
| 会话管理 | 数据库存储 Refresh Token | 支持撤销和多设备 |

### 关键业务规则

1. **登录失败锁定**：5次失败后锁定30分钟
2. **密码强度**：8-128位，必须包含大小写字母+数字+特殊字符
3. **Token 过期**：Access Token 1小时，Refresh Token 7天

### API 清单

- `POST /api/v1/auth/register` - 用户注册
- `POST /api/v1/auth/login` - 密码登录
- `POST /api/v1/auth/login/sms` - 短信登录
- `POST /api/v1/auth/logout` - 用户登出
- `POST /api/v1/auth/refresh` - 刷新Token
- `POST /api/v1/auth/password/change` - 修改密码
- `POST /api/v1/auth/password/reset` - 重置密码

### 数据库表

- `t_auth_user` - 认证用户信息
- `t_user_session` - 用户会话管理
- `t_login_log` - 登录日志记录

## 评审关注点

1. **聚合边界**：auth 聚合与 user 聚合的边界划分是否合理
2. **领域服务端口**：PasswordEncoder、TokenService 定义为 domain 端口，infrastructure 实现，符合六边形架构
3. **会话管理**：Refresh Token 存储于数据库的方案是否合适
4. **安全性**：登录失败锁定机制在 domain 层实现，是否足够
5. **依赖方向**：AuthUser 依赖 user 聚合的 UserId，是否造成耦合

## 当前状态

| 层级 | 状态 |
|------|------|
| Domain | 已完成（代码已存在） |
| Application | 已完成（代码已存在） |
| Infrastructure | 待实现（DO、Mapper、RepositoryImpl、PasswordEncoderImpl、TokenServiceImpl） |
| Adapter | 待实现（Controller、Request/Response、异常处理） |
| Start | 待实现（schema.sql、application.yml） |

## 预飞检查结果

<!-- 架构评审前不执行，评审通过后执行 -->
- mvn test: N/A
- checkstyle: N/A
- entropy-check: N/A

## 评审结论

**状态**: approved

**评审意见摘要**:
- 聚合边界划分清晰，auth 与 user 聚合通过 UserId 弱关联，无循环依赖
- 六边形架构合规：依赖方向正确、domain 纯净、Repository 端口定义合理
- 对象边界完整：Request/Response 在 adapter、DO 在 infrastructure
- 领域建模良好：AuthUser 封装业务不变量、值对象不可变
- 安全性设计合理：BCrypt strength=10、JWT 配置恰当、Refresh Token 数据库方案可接受

**建议优化项** (minor):
1. UserSessionRepository 可增加 update/deleteByRefreshToken 用于 Token 刷新场景
2. JWT claims 建议包含 token 类型标识（typ: access/refresh）

**下一步**: 进入 Build 阶段，按 task-plan.md Phase 1-4 执行

## 附件

- requirement-design.md（完整设计文档，含架构评审章节）
- task-plan.md（任务分解与执行计划）
