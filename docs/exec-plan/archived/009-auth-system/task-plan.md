# 任务执行计划 — 009-auth-system

## 任务状态跟踪

<!-- 状态流转：待办 → 进行中 → 单测通过 → 待验收 → 验收通过 / 待修复 -->

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Domain: AuthUser 聚合根 + 值对象 + 测试 | dev | 已完成 | 代码已存在 |
| 2 | Domain: UserSession 实体 + 测试 | dev | 已完成 | 代码已存在 |
| 3 | Domain: LoginLog 实体 + 测试 | dev | 已完成 | 代码已存在 |
| 4 | Domain: Repository 端口 (AuthUser/UserSession/LoginLog) | dev | 已完成 | 代码已存在 |
| 5 | Domain: 领域服务端口 (PasswordEncoder/TokenService) | dev | 已完成 | 代码已存在 |
| 6 | Application: Command + DTO + Assembler | dev | 已完成 | 代码已存在 |
| 7 | Application: AuthApplicationService 接口 | dev | 已完成 | 代码已存在 |
| 8 | Infrastructure: DO 对象 (AuthUserDO/UserSessionDO/LoginLogDO) | dev | 已完成 | |
| 9 | Infrastructure: MyBatis Mapper | dev | 已完成 | |
| 10 | Infrastructure: Converter (DO ↔ Domain) | dev | 已完成 | |
| 11 | Infrastructure: RepositoryImpl | dev | 已完成 | |
| 12 | Infrastructure: PasswordEncoderImpl (BCrypt) | dev | 已完成 | |
| 13 | Infrastructure: TokenServiceImpl (JWT) | dev | 已完成 | |
| 14 | Infrastructure: 持久化层测试 | dev | 已完成 | |
| 15 | Adapter: AuthController + Request/Response | dev | 已完成 | |
| 16 | Adapter: 全局异常处理 | dev | 已完成 | |
| 17 | Adapter: Web 层测试 (MockMvc) | dev | 已完成 | |
| 18 | Start: schema.sql DDL 更新 | dev | 已完成 | |
| 19 | Start: application.yml JWT 配置 | dev | 已完成 | |
| 20 | 全量 mvn test | dev | 已完成 | 44/44 通过 |
| 21 | QA: 测试用例设计 | qa | 已完成 | 见 test-report.md |
| 22 | QA: 验收测试 + 代码审查 | qa | 已完成 | 见 test-report.md |
| 23 | QA: 接口集成测试 | qa | 已完成 | 见 test-report.md |

## 执行顺序

```
Phase 1: Infrastructure 实现
  - DO 对象设计
  - MyBatis Mapper
  - Converter 转换器
  - RepositoryImpl 实现
  - PasswordEncoderImpl (BCrypt)
  - TokenServiceImpl (JWT)

Phase 2: Adapter 实现
  - AuthController REST API
  - Request/Response 对象
  - 全局异常处理器

Phase 3: Start 配置
  - schema.sql DDL 脚本
  - application.yml JWT 配置

Phase 4: 测试验证
  - Domain 层单测
  - Application 层单测
  - Infrastructure 层集成测试
  - Adapter 层 Web 测试
  - 全量 mvn test

Phase 5: QA 验收
  - 测试用例设计
  - 验收测试执行
  - 代码审查
```

## 技术依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| jjwt-api/impl/jackson | 0.12.3 | JWT Token 生成与验证 |
| spring-security-crypto | 5.7.x | BCrypt 密码加密 |

## 风险点与应对

| 风险点 | 影响 | 应对措施 |
|--------|------|----------|
| JWT 版本差异 | 中 | pom.xml 已配置 0.12.3，需按新版 API 编写 |
| Spring Security 依赖缺失 | 中 | 需在 infrastructure pom 中添加依赖 |
| User 聚合与 Auth 聚合关联 | 低 | 已通过 UserId 值对象解耦 |
| Token 刷新并发问题 | 低 | 通过数据库唯一约束控制 |

## 开发完成记录

<!-- dev 完成后填写 -->
- 全量 `mvn clean test`：x/x 用例通过
- 架构合规检查：
- 通知 @qa 时间：

## QA 验收记录

- 全量测试（含集成测试）：44/44 用例通过
- 代码审查结果：PASS - 分层架构合规，Domain 纯净性良好
- 代码风格检查：PASS - 0 violations
- 问题清单：3 个 Minor 问题，详见 test-report.md
- **最终状态**：验收通过，进入 Ship 阶段
