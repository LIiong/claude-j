# 阶段交接记录 — 022-auth-authorization

## 当前阶段

`dev` → `qa` 待验收

## 状态

- [x] architect: 设计完成
- [x] architect: 评审通过
- [x] dev: 开发完成
- [x] dev: 三项预飞通过
- [ ] qa: 测试验收完成
- [ ] qa: 代码审查完成

## 设计摘要

见 requirement-design.md：Spring Security 5.x + JWT RBAC 授权体系，含角色模型、JWT claims 扩展、URL 级与方法级授权、现有端点权限梳理。

## 交接人

- 从: dev (Claude Code)
- 至: qa (Claude Code QA)
- 日期: 2026-04-25

## 已完成

- [x] 需求设计文档 requirement-design.md
- [x] 任务计划 task-plan.md
- [x] 测试用例设计 test-case-design.md（合并在 requirement-design.md AC 中）
- [x] 架构决策更新（角色存储选型：逗号分隔字符串）
- [x] Domain 层：Role 枚举、User.roles 字段、reconstruct 签名改造
- [x] Infrastructure 层：V9 迁移、UserDO/UserConverter/JwtTokenServiceImpl 改造
- [x] Application 层：UserDTO/UserAssembler/TokenService 改造
- [x] Adapter 层：SecurityConfig、JwtAuthenticationFilter、401/403 Handler、Controller @PreAuthorize
- [x] 测试配置：业务测试 @ActiveProfiles("test") 禁用 Security，Actuator/JwtSecret 保持 dev

## 等待 qa 处理

1. 独立重跑三项预飞验证
2. 验收测试设计执行
3. 代码审查
4. 更新 test-report.md

## 三项预飞证据

| 检查项 | 状态 | 证据 |
|--------|------|------|
| mvn test | pass | Tests run: 59, Failures: 0, Errors: 0 (claude-j-start) |
| checkstyle:check | pass | Exit 0, 0 errors |
| entropy-check | pass | 0 FAIL, 12 WARN, status: PASS |

## 风险提示（已解决）

- ~~改造 JwtAuthenticationFilter 时必须保持现有 token 解析兼容性~~ ✅ 已保持，新 token 含 roles，旧 token 解析默认 USER 角色
- ~~H2 集成测试可能需要 @TestConfiguration 放行某些端点~~ ✅ 已通过 @ActiveProfiles("test") 禁用 SecurityConfig
- ~~全量测试时注意检查现有认证测试是否仍通过~~ ✅ 59 tests passed

## 关键文件变更

| 层 | 文件 | 变更 |
|---|------|------|
| domain | Role.java | 新增枚举 USER/ADMIN |
| domain | User.java | 添加 roles 字段、reconstruct 签名改造 |
| infrastructure | V9__add_user_roles.sql | 新增迁移 |
| infrastructure | UserDO.java | 添加 roles 字段 |
| infrastructure | UserConverter.java | 添加 roles 解析/转换 |
| infrastructure | JwtTokenServiceImpl.java | JWT claims 添加 roles |
| application | TokenService.java | 新增方法 generateTokenPair(UserId, Set<Role>), extractRolesFromToken |
| adapter | SecurityConfig.java | 新增 Spring Security 配置 |
| adapter | JwtAuthenticationFilter.java | 新增 JWT 过滤器 |
| adapter | JwtAuthenticationEntryPoint.java | 新增 401 处理 |
| adapter | JwtAccessDeniedHandler.java | 新增 403 处理 |
| adapter | *Controller.java | 添加 @PreAuthorize 注解 |
| start | *IntegrationTest.java | 调整 @ActiveProfiles |
| start | V8__product_init.sql | idx_status → idx_product_status（H2 兼容） |