# ADR-004: Refresh Token 数据库存储方案

**日期**：2026-04-16
**状态**：已接受
**决策者**：@architect

## 背景

在实现 JWT Token 认证系统时，需要决策 Refresh Token 的存储策略：

1. 纯无状态方案（Token 自包含所有信息）无法支持 Token 撤销
2. 需要支持用户主动登出、强制下线等安全场景
3. 需要支持多设备同时登录管理

## 决策

**Refresh Token 存储于数据库（t_user_session 表），而非纯内存或 Redis 方案。**

技术方案：
- Refresh Token 字符串存储于 `t_user_session.refresh_token` 字段
- Token 元数据（过期时间、设备信息、IP）一并存储
- 登出时删除会话记录实现 Token 撤销

## 备选方案

| 方案 | 优点 | 缺点 | 排除原因 |
|------|------|------|----------|
| 纯无状态 JWT | 无需存储、水平扩展简单 | 无法撤销、无法强制下线、无法管理多设备 | 不满足安全需求 |
| Redis 存储 | 性能高、过期自动清理 | 增加基础设施依赖、数据持久化风险 | 项目当前技术栈以数据库为主 |
| Token 黑名单 | 可撤销 Access Token | 黑名单无限增长、需要持久化 | 复杂度高于收益 |
| 仅存储 Token Hash | 安全性略高 | 无法直接验证 Token 有效性 | 查询复杂度增加 |

## 影响

### 正向影响
- **支持 Token 撤销**：用户登出即删除会话记录
- **支持强制下线**：可删除用户所有会话
- **支持多设备**：每个设备独立会话记录
- **审计追踪**：设备信息、IP 地址可记录

### 负向影响
- 每次 Token 刷新需要数据库查询
- 需要定期清理过期会话（可异步任务处理）

### 缓解措施
- 会话表添加过期时间索引，便于清理
- Access Token 仍保持无状态，减少数据库压力
- 考虑未来引入 Redis 作为二级缓存（可选优化）

## 技术实现

```sql
CREATE TABLE t_user_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    user_id VARCHAR(32) NOT NULL,
    refresh_token VARCHAR(500) NOT NULL,
    device_info VARCHAR(500),
    ip_address VARCHAR(45),
    expires_at TIMESTAMP NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_refresh_token (refresh_token)
);
```

## 相关代码

- `com.claudej.domain.auth.model.entity.UserSession`
- `com.claudej.domain.auth.repository.UserSessionRepository`
- `com.claudej.infrastructure.auth.persistence.UserSessionDO`
