# 任务执行计划 — 022-auth-authorization

## 任务状态跟踪

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Domain: Role 枚举定义 + 测试 | dev | ✅ 完成 | RoleTest 5 tests passed |
| 2 | Domain: User 聚合根增加 roles 字段 + 测试 | dev | ✅ 完成 | UserRolesTest 9 tests passed |
| 3 | Infrastructure: t_user 表新增 roles 字段 + Flyway V9 迁移 | dev | ✅ 完成 | V9__add_user_roles.sql |
| 4 | Application: JwtTokenProvider 改造（claims 携带 roles） | dev | ✅ 完成 | JwtTokenServiceImpl 添加 roles claim |
| 5 | Adapter: JwtAuthenticationFilter 改造（构建 GrantedAuthority） | dev | ✅ 完成 | 从 token 提取 roles 构建 authorities |
| 6 | Adapter: SecurityConfig 配置（SecurityFilterChain + 异常处理） | dev | ✅ 完成 | @Profile("!test") 区分测试环境 |
| 7 | Adapter: 无权限响应处理（401/403 自定义） | dev | ✅ 完成 | JwtAuthenticationEntryPoint + JwtAccessDeniedHandler |
| 8 | Adapter: 现有 Controller 权限标注（@PreAuthorize） | dev | ✅ 完成 | 所有 Controller 已添加注解 |
| 9 | Start: 全量 mvn test + checkstyle + entropy-check | dev | ✅ 完成 | 三项预飞全部通过 |
| 10 | Docs: 授权体系文档 + 端点权限映射表 | dev | ⏸ 暂缓 | 非核心需求，可在 QA 验收后补充 |
| 11 | QA: 测试用例设计 + 验收测试 | qa | ⚠️ 验收打回 | 3 高严重度问题缺失测试 |
| 12 | QA: 代码审查 + 手动渗透测试 | qa | ✅ 完成 | 代码审查通过，三项预飞通过 |
| 13 | **Fix: JwtTokenServiceImplTest（infrastructure 层）** | dev | ✅ 完成 | 7 tests passed |
| 14 | **Fix: JwtAuthenticationFilterTest（adapter 层）** | dev | ✅ 完成 | 6 tests passed |
| 15 | **Fix: UserControllerSecurityTest（adapter 层）** | dev | ✅ 完成 | 5 tests passed |
| 16 | **Fix: 三项预飞重新验证** | dev | ✅ 完成 | mvn test: 59 passed, checkstyle: 0, entropy: PASS |
| 17 | **QA: 重新验收** | qa | ✅ 完成 | 3 高严重度问题已修复，测试覆盖满足 AC 要求，三项预飞通过 |

## 执行顺序

domain → infrastructure → application → adapter → start → 全量测试 → QA 验收 → **Fix → QA 重新验收**

## 测试策略

- **单元测试**：RoleTest, UserTest, JwtTokenProviderTest, JwtAuthenticationFilterTest
- **WebMvcTest**：各 Controller 安全测试（带不同角色 token 测试访问控制）
- **集成测试**：现有集成测试全部不破坏（59 tests passed）
