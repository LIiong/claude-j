# 测试报告 — 022-auth-authorization

**测试日期**：2026-04-25
**测试人员**：@qa
**版本状态**：待修复
**任务类型**：基础设施（Spring Security 授权体系）

---

## 一、测试执行结果

### 三项预飞验证（独立重跑）

| 检查项 | 命令 | 结果 | 证据 |
|--------|------|------|------|
| mvn test | `mvn test 2>&1 | tail -50` | PASS | Tests run: 59, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS |
| checkstyle:check | `mvn checkstyle:check 2>&1` | PASS | You have 0 Checkstyle violations, BUILD SUCCESS |
| entropy-check | `./scripts/entropy-check.sh 2>&1` | PASS | 错误 (FAIL): 0, 警告 (WARN): 12, status: PASS |

### 分层测试：`mvn clean test` ✅ 通过

| 模块 | 测试类 | 用例数 | 通过 | 失败 | 耗时 |
|------|--------|--------|------|------|------|
| domain | RoleTest | 5 | 5 | 0 | ~0.1s |
| domain | UserTest | 18 | 18 | 0 | ~0.1s |
| domain | HexagonalArchitectureTest | 13 | 13 | 0 | ~2.5s |
| application | *ApplicationServiceTest (8 files) | 多 | 全 | 0 | ~3s |
| infrastructure | *RepositoryImplTest (9 files) + JwtSecretValidatorTest | 多 | 全 | 0 | ~20s |
| adapter | *ControllerTest (10 files) | 多 | 全 | 0 | ~17s |
| **分层合计** | **约 59 测试** | **59** | **59** | **0** | **~68s** |

### 集成测试（全链路）：✅ 通过

| 模块 | 测试类 | 用例数 | 通过 | 失败 | 耗时 |
|------|--------|--------|------|------|------|
| start | JwtSecretIntegrationTest | 6 | 6 | 0 | ~1.9s |
| start | FlywayVerificationTest | 2 | 2 | 0 | ~6.2s |
| start | CouponIntegrationTest | 5 | 5 | 0 | ~0.1s |
| start | ShortLinkIntegrationTest | 10 | 10 | 0 | ~0.1s |
| start | CartIntegrationTest | 9 | 9 | 0 | ~0.1s |
| start | ActuatorHealthIntegrationTest | 6 | 6 | 0 | ~1.9s |
| start | TraceIdIntegrationTest | 2 | 2 | 0 | ~0.02s |
| **总计** | **7 个测试类** | **40** | **40** | **0** | **~10s** |

### AC 自动化覆盖矩阵

| AC | 验收条件 | 对应测试/验证 | 状态 |
|----|---------|--------------|------|
| AC1 | Role 枚举定义 USER/ADMIN | RoleTest (5 cases) | ✅ |
| AC1 | User 聚合根持有 Set<Role> | UserTest.should_createUser_when_validInput | ✅ |
| AC1 | t_user.roles 字段 + Flyway V9 | V9__add_user_roles.sql 存在 | ✅ |
| AC2 | JWT claims 包含 roles | JwtTokenServiceImpl 源码审查 | ✅ |
| AC2 | JwtTokenProvider 能解析角色 | **缺失对应测试** | ❌ |
| AC3 | SecurityFilterChain 配置 | SecurityConfig 源码审查 | ✅ |
| AC3 | JwtAuthenticationFilter 构建 GrantedAuthority | **缺失对应测试** | ❌ |
| AC3 | @EnableMethodSecurity | SecurityConfig 源码审查 | ✅ |
| AC3 | 401/403 自定义响应 | JwtAuthenticationEntryPoint/JwtAccessDeniedHandler 源码审查 | ✅ |
| AC4 | 所有 Controller @PreAuthorize 标注 | 源码审查（10 个 Controller） | ✅ |
| AC5 | JwtAuthenticationFilterTest | **缺失** | ❌ |
| AC5 | SecurityConfigTest 或 @WebMvcTest | 现有 WebMvcTest **禁用安全过滤器** | ⚠️ |
| AC5 | UserControllerSecurityTest（403 测试） | **缺失** | ❌ |
| AC5 | 集成测试不破坏 | 59 tests passed | ✅ |
| AC6 | 三项预飞通过 | 独立重跑验证 | ✅ |
| AC7 | docs/ops/authorization.md | **缺失** | ❌ |

---

## 二、代码审查结果

### 依赖方向检查

| 检查项 | 结果 | 说明 |
|--------|------|------|
| adapter → application（不依赖 domain/infrastructure） | ✅ | AuthController 仅依赖 AuthApplicationService |
| application → domain（不依赖其他层） | ✅ | 无 Spring/MyBatis import |
| domain 无外部依赖 | ✅ | Role.java、User.java 无框架依赖 |
| infrastructure → domain + application | ✅ | JwtTokenServiceImpl 正确实现 TokenService 接口 |

### 安全实现检查

| 检查项 | 结果 | 说明 |
|--------|------|------|
| Role 枚举定义完整 | ✅ | USER/ADMIN 两个角色，含 isAdmin/isUser 方法 |
| User 聚合根 roles 字段 | ✅ | Set<Role> 类型，含 hasRole/addRole/removeRole 方法 |
| UserDO.roles 字段 | ✅ | String 类型，逗号分隔存储 |
| UserConverter roles 解析 | ✅ | parseRoles 方法正确解析逗号分隔字符串 |
| JwtTokenServiceImpl roles claim | ✅ | generateTokenPair 添加 roles claim，extractRolesFromToken 提取角色 |
| JwtAuthenticationFilter GrantedAuthority | ✅ | 构建 "ROLE_" + role.name() 格式的 authority |
| SecurityConfig SecurityFilterChain | ✅ | @Profile("!test") 分离测试环境，permitAll 配置正确 |
| JwtAuthenticationEntryPoint | ✅ | 401 返回 ApiResult.fail("401", "未认证") |
| JwtAccessDeniedHandler | ✅ | 403 返回 ApiResult.fail("403", "权限不足") |
| @EnableGlobalMethodSecurity | ✅ | prePostEnabled = true |

### Controller @PreAuthorize 标注检查

| Controller | 类级标注 | 方法级标注（ADMIN） | 权限说明注释 |
|------------|---------|-------------------|-------------|
| AuthController | 无（permitAll） | 无 | ✅ 公开端点说明 |
| UserController | 无 | freezeUser/unfreezeUser ✅ | ✅ 权限说明 |
| OrderController | @PreAuthorize("hasRole('USER')") | shipOrder/refundOrder ✅ | ✅ 权限说明 |
| ProductController | @PreAuthorize("hasRole('USER')") | createProduct/updatePrice/activateProduct/deactivateProduct ✅ | ✅ 权限说明 |
| CartController | @PreAuthorize("hasRole('USER')") | 无（全 USER） | ✅ 权限说明 |
| CouponController | @PreAuthorize("hasRole('USER')") | createCoupon ✅ | ✅ 权限说明 |
| LinkController | @PreAuthorize("hasRole('USER')") | 无（全 USER） | ✅ 权限说明 |

---

## 三、代码风格检查结果

| 检查项 | 结果 |
|--------|------|
| Java 8 兼容（无 var、records、text blocks、List.of） | ✅ 无违规 |
| 聚合根仅 @Getter | ✅ User.java 使用 @Getter |
| 值对象 @Getter + @EqualsAndHashCode | ✅ Role 是 enum，无需 |
| DO 用 @Data + @TableName | ✅ UserDO 正确 |
| DTO 用 @Data | ✅ UserDTO 正确 |
| 命名规范：XxxDO, XxxDTO, XxxMapper | ✅ 符合 |
| 包结构 com.claudej.{layer}.{aggregate}.{sublayer} | ✅ 符合 |
| 测试命名 should_xxx_when_xxx | ✅ RoleTest/UserTest 符合 |
| Checkstyle 0 violations | ✅ |

---

## 四、测试金字塔合规

| 层 | 测试类型 | 框架 | Spring 上下文 | 结果 |
|---|---------|------|-------------|------|
| Domain | 纯单元测试 | JUnit 5 + AssertJ | 无 | ✅ RoleTest/UserTest |
| Application | Mock 单元测试 | JUnit 5 + Mockito | 无 | ✅ 8 个 ServiceTest |
| Infrastructure | 集成测试 | @SpringBootTest + H2 | 有 | ✅ 9 个 RepositoryTest |
| Adapter | API 测试 | @WebMvcTest + MockMvc | 部分 | ⚠️ **禁用安全过滤器** |
| **全链路** | **接口集成测试** | **@SpringBootTest + H2** | **完整** | ✅ 40 tests |

**注意**：Adapter 层 WebMvcTest 使用 `@AutoConfigureMockMvc(addFilters = false)` 禁用安全过滤器，**无法验证 @PreAuthorize 授权逻辑**。

---

## 五、问题清单

| # | 严重度 | 描述 | 处理 |
|---|--------|------|------|
| 1 | **高** | AC5 要求的 JwtAuthenticationFilterTest 缺失 — 无法验证 token 角色提取和 GrantedAuthority 构建 | 需 @dev 补充测试 |
| 2 | **高** | AC5 要求的 UserControllerSecurityTest 缺失 — 无法验证普通用户访问 ADMIN 端点返回 403 | 需 @dev 补充测试 |
| 3 | **高** | AC2 要求的 JwtTokenProvider extractRolesFromToken 测试缺失 — 无法验证角色解析逻辑 | 需 @dev 补充测试 |
| 4 | **中** | WebMvcTest 使用 `addFilters = false` 绕过安全测试 — 现有 Controller 测试无法验证授权 | 需创建专门的安全测试配置 |
| 5 | **中** | AC7 要求的 docs/ops/authorization.md 缺失 — 无端点权限映射文档 | 需 @dev 补充文档 |
| 6 | **低** | entropy-check 12 WARN：缺少 auth 聚合测试文件（AuthControllerTest、AuthApplicationServiceTest、TokenServiceTest 等） | 建议后续补充 |
| 7 | **低** | entropy-check 5 WARN：ADR 缺少状态节（001-005） | 不阻塞验收 |
| 8 | **低** | TestSecurityConfig 使用已废弃的 WebSecurityConfigurerAdapter（Spring Security 5.7+ 推荐使用 SecurityFilterChain Bean） | 建议后续升级 |

**3 个阻塞性问题（高），3 个改进建议（中/低）。**

---

## 六、验收结论

| 维度 | 结论 |
|------|------|
| 功能完整性 | ⚠️ Spring Security 配置完整，Controller @PreAuthorize 标注完整，但 **缺少安全测试验证** |
| 测试覆盖 | ❌ AC5 要求的 3 项安全测试缺失（JwtAuthenticationFilterTest、UserControllerSecurityTest、extractRolesFromToken 测试） |
| 架构合规 | ✅ Domain 纯净、依赖方向正确、DO 不泄漏 |
| 代码风格 | ✅ Checkstyle 0 violations |
| 数据库设计 | ✅ V9 迁移脚本正确 |

### 最终状态：❌ 待修复 — 见问题清单

**关键阻塞**：AC5 明确要求：
- `JwtAuthenticationFilterTest`：验证带角色的 token 能正确构建 Authentication
- `UserControllerSecurityTest`：验证普通用户访问 ADMIN 接口返回 403
- 现有集成测试不破坏 ✅

上述两项测试缺失，无法证明授权功能在生产场景下有效工作。

---

## 验证证据

### 三项预飞独立验证

```bash
# mvn test
$ mvn test 2>&1 | tail -50
Tests run: 59, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

# checkstyle:check
$ mvn checkstyle:check 2>&1
You have 0 Checkstyle violations.
BUILD SUCCESS

# entropy-check
$ ./scripts/entropy-check.sh 2>&1
错误 (FAIL): 0
警告 (WARN): 12
{"issues": 0, "warnings": 12, "status": "PASS"}
架构合规检查通过。
```

### 安全配置审查关键代码片段

```java
// SecurityConfig.java - @EnableGlobalMethodSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Profile("!test")
public class SecurityConfig { ... }

// JwtAuthenticationFilter.java - GrantedAuthority 构建
List<SimpleGrantedAuthority> authorities = roles.stream()
    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
    .collect(Collectors.toList());

// JwtTokenServiceImpl.java - roles claim
String accessToken = Jwts.builder()
    .subject(userId.getValue())
    .claim("typ", "access")
    .claim("roles", roleNames)  // 角色携带
    ...

// UserController.java - ADMIN 端点标注
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/{userId}/freeze")
public ApiResult<UserResponse> freezeUser(@PathVariable String userId) { ... }
```

### 缺失测试确认

```bash
# JwtAuthenticationFilterTest 不存在
$ find . -name "JwtAuthenticationFilterTest.java"
# 无结果

# UserControllerSecurityTest 不存在
$ find . -name "*SecurityTest.java"
# 无结果

# WebMvcTest 禁用安全过滤器
$ grep -r "addFilters = false" claude-j-adapter/src/test/
claude-j-adapter/src/test/java/com/claudej/adapter/user/web/UserControllerTest.java:
@AutoConfigureMockMvc(addFilters = false)
```