# 开发日志 — 022-auth-authorization

## 任务概述
为 claude-j 建立 Spring Security + JWT RBAC 授权体系。

---

## 开发进度

### Phase 1: Domain 层开发

#### 1.1 Role 枚举定义
- **状态**: ✅ 完成
- **测试**: RoleTest - 5 tests passed

#### 1.2 User 聚合根增加 roles 字段
- **状态**: ✅ 完成
- **测试**: UserRolesTest - 9 tests passed, UserTest - roles 验证通过

---

### Phase 2: Infrastructure 层开发

#### 2.1 Flyway V9 迁移脚本
- **状态**: ✅ 完成
- **文件**: `claude-j-start/src/main/resources/db/migration/V9__add_user_roles.sql`

#### 2.2 UserDO 增加 roles 字段
- **状态**: ✅ 完成

#### 2.3 UserConverter 改造
- **状态**: ✅ 完成
- **测试**: UserRepositoryImplTest - 12 tests passed

---

### Phase 3: Application 层开发

#### 3.1 UserDTO 增加 roles 字段
- **状态**: ✅ 完成

#### 3.2 UserAssembler 改造
- **状态**: ✅ 完成

#### 3.3 TokenService 接口改造
- **状态**: ✅ 完成
- **新增方法**: `generateTokenPair(UserId, Set<Role>)`, `extractRolesFromToken(String)`

#### 3.4 JwtTokenServiceImpl 改造
- **状态**: ✅ 完成
- **JWT claims**: 添加 `roles` 字段

---

### Phase 4: Adapter 层开发

#### 4.1 Spring Security 配置
- **状态**: ✅ 完成
- **文件**: `SecurityConfig.java` - 使用 `@Profile("!test")` 区分测试环境

#### 4.2 JwtAuthenticationFilter
- **状态**: ✅ 完成
- **功能**: 从 JWT 提取 roles 构建 GrantedAuthority

#### 4.3 异常处理 (401/403)
- **状态**: ✅ 完成
- **文件**: `JwtAuthenticationEntryPoint.java`, `JwtAccessDeniedHandler.java`

#### 4.4 Controller 权限标注
- **状态**: ✅ 完成
- **涉及**: 所有 Controller 已添加 `@PreAuthorize` 注解

---

### Phase 5: 测试配置调整

#### 5.1 WebMvcTest 测试禁用 Security
- **状态**: ✅ 完成
- **方案**: 所有 WebMvcTest 添加 `@AutoConfigureMockMvc(addFilters = false)`

#### 5.2 集成测试禁用 Security
- **状态**: ✅ 完成
- **方案**: 所有 `@SpringBootTest` 添加 `@AutoConfigureMockMvc(addFilters = false)`

---

## 问题记录

| 问题 | 根因 | 解决方案 | 验证 |
|------|------|----------|------|
| Maven 代理连接失败 | settings.xml 配置了 127.0.0.1:7897 代理但服务未运行 | 创建临时 settings.xml 禁用代理 | mvn test 成功下载依赖 |
| ApiResult.error 方法不存在 | ApiResult 使用 `fail(String, String)` 而非 `error(int, String)` | 修改 JwtAuthenticationEntryPoint/JwtAccessDeniedHandler 使用正确方法 | 编译通过 |
| Spring Security 5.x API 差异 | `requestMatchers` 在 5.x 使用 `antMatchers` | 修改 SecurityConfig 使用 Spring Security 5.x API | 编译通过 |
| WebMvcTest 401/403 错误 | Security filters 在测试中启用 | 添加 `@AutoConfigureMockMvc(addFilters = false)` | 78 tests passed |
| 集成测试 401 错误 | Security filters 在测试中启用 | 添加 `@AutoConfigureMockMvc(addFilters = false)` | 59 tests passed |
| NoSuchMethodError User.reconstruct | 堆积的 class 文件与新签名不匹配 | mvn clean install 全量重编译 | Tests passed |
| AuthenticationCredentialsNotFoundException | `@ActiveProfiles("dev")` 启用 SecurityConfig，方法级安全生效 | 业务测试改为 `@ActiveProfiles("test")` 禁用 Security | Tests passed |
| Flyway V8 idx_status 冲突 | H2 索引名需全局唯一（V7/V8 同名） | V8 改为 `idx_product_status` | Flyway 迁移成功 |
| migration-h2 目录残留 | 删除后仍有残留导致重复 V8 | 删除 migration-h2 目录 | Flyway 迁移成功 |
| FlywayVerificationTest 预期数不对 | V9 新增后应为 9 条 | 修改测试预期为 9 条迁移 | Test passed |
| JwtTokenServiceImpl 无用导入 | `java.util.Arrays` 未使用 | 删除无用导入 | checkstyle: 0 errors |

---

## 与原设计变更

| 原设计 | 实际实现 | 变更原因 |
|--------|----------|----------|
| 测试环境使用 `@Profile("test")` | 业务测试改为 `@ActiveProfiles("test")`，Actuator/TraceId/JwtSecret 保持 dev | 业务测试需禁用 Security，Actuator/JwtSecret 测试需 dev 配置 |
| V8 idx_status 索引名 | 改为 `idx_product_status` | H2 要求索引名全局唯一 |

---

## QA 打回修复记录（2026-04-25）

### 问题 1：JwtTokenServiceImpl.extractRolesFromToken 测试缺失
- **根因**: QA 验收发现 AC2 要求的角色提取测试未实现
- **修复**: 补充 `JwtTokenServiceImplTest`（infrastructure 层）
- **新增测试**:
  - `should_extractRolesFromValidToken` - 验证从有效 token 提取 USER/ADMIN 角色
  - `should_returnDefaultUserRole_when_tokenHasNoRoles` - 验证无角色 token 返回默认 USER 角色
  - `should_returnDefaultUserRole_when_tokenInvalid` - 验证无效 token 返回默认 USER 角色
- **验证**: `mvn test -pl claude-j-infrastructure -Dtest=JwtTokenServiceImplTest` → 7 tests passed

### 问题 2：JwtAuthenticationFilterTest 缺失
- **根因**: QA 验收发现 AC5 要求的 Filter 测试未实现
- **修复**: 补充 `JwtAuthenticationFilterTest`（adapter 层）
- **新增测试**:
  - `should_buildGrantedAuthorities_when_tokenContainsRoles` - 验证 token 角色转换为 GrantedAuthority
  - `should_defaultToUserRole_when_tokenHasNoRoles` - 验证无角色 token 默认为 ROLE_USER
  - `should_clearSecurityContext_when_tokenInvalid` - 验证无效 token 清空 SecurityContext
  - `should_clearSecurityContext_when_noTokenProvided` - 验证无 token 清空 SecurityContext
  - `should_clearSecurityContext_when_tokenServiceThrowsException` - 验证异常时清空 SecurityContext
  - `should_notProcessToken_when_headerNotBearerFormat` - 验证非 Bearer 格式 header 不处理
- **验证**: `mvn test -pl claude-j-adapter -Dtest=JwtAuthenticationFilterTest` → 6 tests passed

### 问题 3：UserControllerSecurityTest 缺失
- **根因**: QA 验收发现 AC5 要求的安全端点测试未实现
- **修复**: 补充 `UserControllerSecurityTest`（adapter 层）
- **技术挑战**:
  - WebMvcTest 需要正确配置 ObjectMapper 处理 LocalDateTime 序列化（JavaTimeModule）
  - 方法级安全（@PreAuthorize）抛出的 AccessDeniedException 在 WebMvcTest 环境中未被 JwtAccessDeniedHandler 正确处理，需添加专门的 @ExceptionHandler
  - UserId 值对象格式验证（UR + 16 位字母数字，不含 I/O）
- **新增测试**:
  - `should_return403_when_userAccessAdminEndpoint` - 验证 USER 角色 403 Forbidden
  - `should_return200_when_adminAccessAdminEndpoint` - 验证 ADMIN 角色 200 OK
  - `should_return401_when_noTokenProvided` - 验证无 token 401 Unauthorized
  - `should_return401_when_invalidTokenProvided` - 验证无效 token 401 Unauthorized
  - `should_return200_when_userAccessUserEndpoint` - 验证 USER 角色 200 OK
- **验证**: `mvn test -pl claude-j-adapter -Dtest=UserControllerSecurityTest` → 5 tests passed

---

## 三项预飞检查（修复后）

| 检查项 | 状态 | 证据 |
|--------|------|------|
| mvn test | PASS | Tests run: 59, Failures: 0, Errors: 0, Skipped: 0 |
| mvn checkstyle:check | PASS | 0 Checkstyle violations |
| entropy-check | PASS | 0 FAIL, 12 WARN, status: PASS |

---

## 假设与待确认

- 角色 Role 存放在 User 聚合根（而非 AuthUser），因角色属于用户业务属性而非认证属性 ✓
- 新用户注册默认角色为 USER（单角色） ✓
- 测试环境禁用 Security 以验证业务逻辑 ✓
- Actuator/JwtSecret/TraceId 测试保持 dev profile 以测试 dev 配置 ✓