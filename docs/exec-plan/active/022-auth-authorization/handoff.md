# 阶段交接记录 — 022-auth-authorization

## 当前阶段

`qa` → `dev` 待修复

## 状态

- [x] architect: 设计完成
- [x] architect: 评审通过
- [x] dev: 开发完成
- [x] dev: 三项预飞通过
- [x] qa: 测试验收完成
- [x] qa: 代码审查完成
- [ ] **qa: 问题修复完成**（待 @dev 处理）

## 设计摘要

见 requirement-design.md：Spring Security 5.x + JWT RBAC 授权体系，含角色模型、JWT claims 扩展、URL 级与方法级授权、现有端点权限梳理。

## 交接人

- 从: qa (Claude Code QA)
- 至: dev (Claude Code)
- 日期: 2026-04-25

## QA 验收结果

### 三项预飞独立验证（PASS）

| 检查项 | 命令 | 结果 | 证据 |
|--------|------|------|------|
| mvn test | `mvn test 2>&1 | tail -50` | PASS | Tests run: 59, Failures: 0, Errors: 0, BUILD SUCCESS |
| checkstyle:check | `mvn checkstyle:check 2>&1` | PASS | 0 Checkstyle violations, BUILD SUCCESS |
| entropy-check | `./scripts/entropy-check.sh 2>&1` | PASS | 0 FAIL, 12 WARN, status: PASS |

### 代码审查结果（PASS）

- Domain 纯净性 ✅
- 依赖方向正确 ✅
- DO 不泄漏 ✅
- Controller @PreAuthorize 标注完整 ✅
- SecurityConfig/JwtAuthenticationFilter/401/403 Handler 实现正确 ✅

### 问题清单（3 高 + 3 中/低）

| # | 严重度 | 描述 |
|---|--------|------|
| 1 | **高** | AC5 要求的 `JwtAuthenticationFilterTest` 缺失 — 无法验证 token 角色提取和 GrantedAuthority 构建 |
| 2 | **高** | AC5 要求的 `UserControllerSecurityTest` 缺失 — 无法验证普通用户访问 ADMIN 端点返回 403 |
| 3 | **高** | AC2 要求的 `JwtTokenServiceImpl.extractRolesFromToken` 测试缺失 — 无法验证角色解析逻辑 |
| 4 | **中** | WebMvcTest 使用 `addFilters = false` 绕过安全测试 — 现有 Controller 测试无法验证授权 |
| 5 | **中** | AC7 要求的 `docs/ops/authorization.md` 缺失 — 无端点权限映射文档 |
| 6 | **低** | entropy-check 12 WARN：缺少 auth 聚合测试文件 |
| 7 | **低** | ADR 缺少状态节（001-005） |
| 8 | **低** | TestSecurityConfig 使用已废弃的 WebSecurityConfigurerAdapter |

## 等待 dev 处理

### 必须修复（阻塞验收）

1. **补充 JwtAuthenticationFilterTest**（adapter 层）
   - 测试 should_buildGrantedAuthorities_when_tokenContainsRoles
   - 测试 should_defaultToUserRole_when_tokenHasNoRoles
   - 测试 should_clearSecurityContext_when_tokenInvalid

2. **补充 UserControllerSecurityTest**（adapter 层）
   - 使用 `@WebMvcTest` + **启用安全过滤器**
   - 测试 should_return403_when_userAccessAdminEndpoint
   - 测试 should_return401_when_noTokenProvided

3. **补充 JwtTokenServiceImpl.extractRolesFromToken 测试**（infrastructure 层）
   - 测试 should_extractRolesFromValidToken
   - 测试 should_returnDefaultUserRole_when_tokenHasNoRoles

### 建议修复（不阻塞）

4. 创建专门的安全测试配置（不使用 addFilters = false）
5. 补充 docs/ops/authorization.md（端点权限映射表）
6. 后续补充 auth 聚合其他测试文件

## 验收结论

❌ **待修复** — AC5 要求的安全测试缺失，无法证明授权功能在生产场景下有效工作。

修复完成后请重新提交验收。

---

## 关键文件变更（供 dev 参考）

需要新增的测试文件：
- `claude-j-adapter/src/test/java/com/claudej/adapter/auth/security/JwtAuthenticationFilterTest.java`
- `claude-j-adapter/src/test/java/com/claudej/adapter/user/web/UserControllerSecurityTest.java`
- `claude-j-infrastructure/src/test/java/com/claudej/infrastructure/auth/token/JwtTokenServiceImplTest.java`

可能需要修改的配置：
- `claude-j-adapter/src/test/java/com/claudej/adapter/test/TestSecurityConfig.java`（升级到 SecurityFilterChain Bean）

## 详细报告

见 `test-report.md`