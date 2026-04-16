# 登录系统功能需求文档

## 项目信息
- **项目名称**: Claude-j 项目
- **需求类型**: 新功能开发
- **优先级**: 高
- **提交时间**: 2026-04-15

---

## 1. 需求概述

### 1.1 背景
Claude-j 项目需要实现用户登录系统能力，以支持用户身份验证、权限管理和个性化服务。

### 1.2 目标
实现一个安全、可靠、易用的登录系统，包括用户注册、登录、登出、密码管理等核心功能。

---

## 2. 功能需求

### 2.1 用户注册
- [ ] 支持邮箱注册
- [ ] 支持手机号注册
- [ ] 邮箱/手机号验证
- [ ] 密码强度校验（至少8位，包含大小写字母、数字、特殊字符）
- [ ] 验证码防刷机制

### 2.2 用户登录
- [ ] 支持邮箱+密码登录
- [ ] 支持手机号+验证码登录
- [ ] 支持第三方登录（Google、GitHub、微信等）
- [ ] 记住我功能（Remember Me）
- [ ] 登录失败次数限制（防暴力破解）
- [ ] 登录日志记录

### 2.3 密码管理
- [ ] 密码加密存储（bcrypt/Argon2）
- [ ] 忘记密码（通过邮箱/手机重置）
- [ ] 修改密码
- [ ] 密码过期提醒（可选）

### 2.4 会话管理
- [ ] JWT Token 认证
- [ ] Token 自动续期
- [ ] 多设备登录支持
- [ ] 强制下线功能
- [ ] 会话超时处理

### 2.5 安全特性
- [ ] HTTPS 强制
- [ ] CSRF 防护
- [ ] XSS 防护
- [ ] SQL 注入防护
- [ ] 敏感操作二次验证
- [ ] 异地登录提醒

---

## 3. 技术架构

### 3.1 后端技术栈建议
```
- 框架: Node.js + Express / Python + FastAPI / Go + Gin
- 数据库: PostgreSQL / MySQL
- 缓存: Redis（会话存储、验证码缓存）
- 认证: JWT + Refresh Token
- 密码加密: bcrypt / Argon2
- ORM: Prisma / SQLAlchemy / GORM
```

### 3.2 前端技术栈建议
```
- 框架: React / Vue / Next.js
- UI组件库: Ant Design / Element Plus / shadcn/ui
- 状态管理: Redux / Pinia / Zustand
- HTTP客户端: Axios / Fetch
```

### 3.3 API 设计

#### 注册接口
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "phone": "+86138xxxxxxxx",
  "password": "SecurePass123!",
  "verificationCode": "123456"
}
```

#### 登录接口
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "rememberMe": true
}

Response:
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 3600,
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "name": "User Name"
  }
}
```

#### 登出接口
```http
POST /api/auth/logout
Authorization: Bearer {accessToken}
```

#### 刷新Token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

---

## 4. 数据库设计

### 4.1 用户表 (users)
```sql
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) UNIQUE,
  phone VARCHAR(20) UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  name VARCHAR(100),
  avatar_url VARCHAR(500),
  email_verified BOOLEAN DEFAULT FALSE,
  phone_verified BOOLEAN DEFAULT FALSE,
  status VARCHAR(20) DEFAULT 'active', -- active, inactive, suspended
  last_login_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 4.2 会话表 (sessions)
```sql
CREATE TABLE sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  refresh_token VARCHAR(500) NOT NULL,
  device_info JSONB,
  ip_address INET,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 4.3 登录日志表 (login_logs)
```sql
CREATE TABLE login_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  login_type VARCHAR(50), -- password, sms, oauth
  ip_address INET,
  device_info JSONB,
  status VARCHAR(20), -- success, failed
  fail_reason VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 5. 界面设计

### 5.1 登录页面
- 邮箱/手机号输入框
- 密码输入框（显示/隐藏切换）
- 记住我复选框
- 登录按钮
- 忘记密码链接
- 注册账号链接
- 第三方登录按钮

### 5.2 注册页面
- 邮箱/手机号输入框
- 验证码输入框（带发送按钮）
- 密码输入框（强度实时显示）
- 确认密码输入框
- 用户协议/隐私政策勾选
- 注册按钮
- 已有账号登录链接

### 5.3 忘记密码页面
- 邮箱/手机号输入框
- 验证码输入框
- 新密码输入框
- 确认新密码输入框
- 重置密码按钮

---

## 6. 安全要求

### 6.1 密码安全
- 最小长度：8位
- 必须包含：大写字母、小写字母、数字、特殊字符
- 不能使用常见密码（password, 123456等）
- 密码加密存储（bcrypt，cost factor 12+）

### 6.2 认证安全
- JWT 过期时间：Access Token 1小时，Refresh Token 7天
- Token 签名算法：HS256 或 RS256
- HTTPS 强制传输
- 敏感信息脱敏显示

### 6.3 防护机制
- 登录失败锁定：5次失败后锁定30分钟
- 验证码有效期：5分钟
- 验证码发送频率：1分钟间隔
- IP 限流：每IP每分钟最多10次请求

---

## 7. 测试要求

### 7.1 单元测试
- [ ] 密码加密/验证测试
- [ ] Token 生成/验证测试
- [ ] 用户注册逻辑测试
- [ ] 登录逻辑测试

### 7.2 集成测试
- [ ] 完整注册流程测试
- [ ] 完整登录流程测试
- [ ] Token 刷新流程测试
- [ ] 第三方登录流程测试

### 7.3 安全测试
- [ ] SQL 注入测试
- [ ] XSS 攻击测试
- [ ] CSRF 攻击测试
- [ ] 暴力破解防护测试

---

## 8. 验收标准

### 8.1 功能验收
- [ ] 用户可以成功注册账号
- [ ] 用户可以使用邮箱+密码登录
- [ ] 用户可以使用手机号+验证码登录
- [ ] 用户可以成功登出
- [ ] 用户可以重置密码
- [ ] Token 自动续期正常工作

### 8.2 性能验收
- [ ] 登录接口响应时间 < 500ms
- [ ] 注册接口响应时间 < 1s
- [ ] 支持并发用户 1000+

### 8.3 安全验收
- [ ] 密码加密存储，无法反解
- [ ] Token 无法伪造
- [ ] 登录失败锁定机制有效
- [ ] 敏感信息不泄露

---

## 9. 开发计划建议

### Phase 1: 基础功能（1-2周）
- 数据库设计实现
- 用户注册/登录 API
- 基础密码管理

### Phase 2: 安全加固（1周）
- JWT 认证实现
- 安全防护机制
- 日志记录

### Phase 3: 高级功能（1周）
- 第三方登录
- 多设备管理
- 用户界面优化

### Phase 4: 测试优化（1周）
- 全面测试
- 性能优化
- 文档完善

---

## 10. 参考资料

- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)

---

**提交人**: 用户  
**提交时间**: 2026-04-15  
**期望完成时间**: 建议4周内完成基础版本
