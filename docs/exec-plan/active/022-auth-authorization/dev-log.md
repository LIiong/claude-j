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

## 假设与待确认

- 角色 Role 存放在 User 聚合根（而非 AuthUser），因角色属于用户业务属性而非认证属性 ✓
- 新用户注册默认角色为 USER（单角色） ✓
- 测试环境禁用 Security 以验证业务逻辑 ✓
- Actuator/JwtSecret/TraceId 测试保持 dev profile 以测试 dev 配置 ✓

---

## 三项预飞检查

| 检查项 | 状态 | 证据 |
|--------|------|------|
| mvn test | ✅ pass | Tests run: 59, Failures: 0, Errors: 0 (claude-j-start) |
| mvn checkstyle:check | ✅ pass | Exit 0, 0 errors |
| entropy-check | ✅ pass | 0 FAIL, 12 WARN, status: PASS |