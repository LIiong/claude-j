# 阶段交接记录 — 022-auth-authorization

## 当前阶段

`dev` → `qa` 待验收

## 状态

- [x] architect: 设计完成
- [x] architect: 评审通过
- [x] dev: 开发完成
- [x] dev: 三项预飞通过
- [x] qa: 测试验收完成（第一次）
- [x] qa: 代码审查完成
- [x] qa: 问题反馈完成
- [x] **dev: 问题修复完成**
- [ ] **qa: 重新验收**（待 @qa 处理）

## 设计摘要

见 requirement-design.md：Spring Security 5.x + JWT RBAC 授权体系，含角色模型、JWT claims 扩展、URL 级与方法级授权、现有端点权限梳理。

## 交接人

- 从: dev (Claude Code)
- 至: qa (Claude Code QA)
- 日期: 2026-04-25

## QA 第一次验收打回问题

### 问题清单（3 高 + 3 中/低）

| # | 严重度 | 描述 |
|---|--------|------|
| 1 | **高** | AC5 要求的 `JwtAuthenticationFilterTest` 缺失 |
| 2 | **高** | AC5 要求的 `UserControllerSecurityTest` 缺失 |
| 3 | **高** | AC2 要求的 `JwtTokenServiceImpl.extractRolesFromToken` 测试缺失 |

## dev 修复完成

### 修复 1：JwtTokenServiceImplTest（infrastructure 层）

新增测试文件：`claude-j-infrastructure/src/test/java/com/claudej/infrastructure/auth/token/JwtTokenServiceImplTest.java`

测试用例：
- `should_extractRolesFromValidToken` - 验证从有效 token 提取 USER/ADMIN 角色
- `should_returnDefaultUserRole_when_tokenHasNoRoles` - 验证无角色 token 返回默认 USER 角色
- `should_returnDefaultUserRole_when_tokenInvalid` - 验证无效 token 返回默认 USER 角色
- 其他辅助测试（validateAccessToken、extractUserIdFromToken）

### 修复 2：JwtAuthenticationFilterTest（adapter 层）

新增测试文件：`claude-j-adapter/src/test/java/com/claudej/adapter/auth/security/JwtAuthenticationFilterTest.java`

测试用例：
- `should_buildGrantedAuthorities_when_tokenContainsRoles` - 验证 token 角色转换为 GrantedAuthority
- `should_defaultToUserRole_when_tokenHasNoRoles` - 验证无角色 token 默认为 ROLE_USER
- `should_clearSecurityContext_when_tokenInvalid` - 验证无效 token 清空 SecurityContext
- 其他辅助测试

### 修复 3：UserControllerSecurityTest（adapter 层）

新增测试文件：`claude-j-adapter/src/test/java/com/claudej/adapter/auth/security/UserControllerSecurityTest.java`

测试用例：
- `should_return403_when_userAccessAdminEndpoint` - 验证 USER 角色 403 Forbidden
- `should_return200_when_adminAccessAdminEndpoint` - 验证 ADMIN 角色 200 OK
- `should_return401_when_noTokenProvided` - 验证无 token 401 Unauthorized
- `should_return401_when_invalidTokenProvided` - 验证无效 token 401 Unauthorized
- `should_return200_when_userAccessUserEndpoint` - 验证 USER 角色 200 OK

技术挑战：
- 配置 ObjectMapper 处理 LocalDateTime 序列化（JavaTimeModule）
- 添加 @ExceptionHandler 处理方法级安全抛出的 AccessDeniedException

## 三项预飞（修复后重新验证）

| 检查项 | 命令 | 结果 | 证据 |
|--------|------|------|------|
| mvn test | `mvn test 2>&1 | tail -30` | PASS | Tests run: 59, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS |
| checkstyle:check | `mvn checkstyle:check 2>&1` | PASS | You have 0 Checkstyle violations, BUILD SUCCESS |
| entropy-check | `./scripts/entropy-check.sh 2>&1` | PASS | 错误 (FAIL): 0, 警告 (WARN): 12, status: PASS |

## 等待 qa 重新验收

请验证新增的 3 个测试文件是否满足 AC2/AC5 要求。

---

## 关键文件变更

新增测试文件：
- `claude-j-infrastructure/src/test/java/com/claudej/infrastructure/auth/token/JwtTokenServiceImplTest.java`
- `claude-j-adapter/src/test/java/com/claudej/adapter/auth/security/JwtAuthenticationFilterTest.java`
- `claude-j-adapter/src/test/java/com/claudej/adapter/auth/security/UserControllerSecurityTest.java`