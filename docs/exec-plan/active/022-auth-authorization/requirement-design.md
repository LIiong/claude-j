# 需求设计 — 022-auth-authorization

## 任务定位

| 属性 | 内容 |
|------|------|
| 任务编号 | 022-auth-authorization |
| 任务名称 | Spring Security + 授权体系 |
| 所属批次 | P0 基础设施（industry-gap-analysis.md 批 1） |
| 优先级 | 🔴 P0 |
| 前置依赖 | 009-auth-system（JWT 认证已完成）、010-secret-externalize（密钥外置化已完成） |
| 后续影响 | A10 Admin API、所有需要角色区分的业务端点 |

---

## 现状与问题

当前 claude-j 的认证层（009-auth-system）仅完成"身份验证"（Authentication）：
- ✅ JWT token 签发与校验
- ✅ 密码 BCrypt 哈希
- ✅ 登录/注册 API

但**缺少"权限控制"（Authorization）**：
- ❌ 任何持有有效 token 的用户都能调用所有接口（包括管理接口）
- ❌ 无角色概念，无法区分普通用户 / 管理员
- ❌ 无方法级安全注解（`@PreAuthorize`）
- ❌ 无 URL 级访问控制
- ❌ 无接口权限清单文档

**安全风险示例**：普通用户拿到 token 后可调用 `DELETE /api/v1/users/{id}` 删除他人账户。

---

## 目标

建立完整的 RBAC（Role-Based Access Control）授权体系：

1. **角色模型**：定义 `USER`、`ADMIN` 等角色，User 聚合持有角色列表
2. **JWT 扩展**：token 中携带角色信息（claims 中加入 `roles`）
3. **Spring Security 集成**：`SecurityFilterChain` + `JwtAuthenticationFilter` 改造
4. **方法级授权**：`@PreAuthorize("hasRole('ADMIN')")` 在 Controller/Service 生效
5. **URL 级授权**：公开端点（`/auth/**`）vs 受保护端点分级
6. **权限映射文档**：每个 API 需要什么角色，一目了然

---

## 非目标（本期不做）

- 细粒度权限（如 `order:read`, `order:write` 级别的 ACL）— 保持简单 RBAC
- OAuth2 / SSO 集成
- 动态权限配置（数据库维护角色-权限映射表）
- 部门/组织级别的数据范围控制

---

## 验收条件（Definition of Done）

### AC1: 角色模型
- [ ] `Role` 枚举定义至少 `USER`、`ADMIN` 两个角色
- [ ] `User` 聚合根持有 `Set<Role>`（多角色支持）
- [ ] 数据库 `t_user` 表新增 `roles` 字段（JSON 数组或逗号分隔字符串）
- [ ] Flyway 迁移脚本 `V9__add_user_roles.sql`

### AC2: JWT Token 携带角色
- [ ] 登录/刷新 token 时，JWT claims 包含 `roles` 字段
- [ ] `JwtTokenProvider` 能从 token 中解析出角色列表
- [ ] 现有测试更新通过（不破坏已有认证流程）

### AC3: Spring Security 授权
- [ ] `SecurityConfig` 配置 `SecurityFilterChain`：
  - `/api/v1/auth/**` — permitAll（公开）
  - `/actuator/**` — permitAll（或 ADMIN，先保持公开兼容现有）
  - 其余 `/api/v1/**` — authenticated
- [ ] `JwtAuthenticationFilter` 改造：校验 token 后，构建包含 `GrantedAuthority` 的 `Authentication` 对象
- [ ] `@EnableMethodSecurity(prePostEnabled = true)` 启用方法级安全
- [ ] `AuthenticationEntryPoint` + `AccessDeniedHandler` 自定义响应（使用 `ApiResult<T>` 格式）

### AC4: 端点权限标注
- [ ] 所有现有 Controller 梳理，按以下原则标注：
  - 查询类（GET /list, /{id}）：`hasRole('USER')` 或 authenticated
  - 修改类（POST /create, PUT /update）：`hasRole('USER')`
  - 删除/管理类：`hasRole('ADMIN')`
- [ ] `UserController` 的 `DELETE /users/{id}` 明确标为 ADMIN
- [ ] 每个 Controller 文件顶部添加权限注释说明

### AC5: 测试覆盖
- [ ] `JwtAuthenticationFilterTest`：验证带角色的 token 能正确构建 Authentication
- [ ] `SecurityConfigTest`（或 `@WebMvcTest`）：验证公开端点无需 token、受保护端点拒绝无 token 请求
- [ ] `UserControllerSecurityTest`：验证普通用户访问 ADMIN 接口返回 403
- [ ] 集成测试：`JwtSecretIntegrationTest` 等现有测试不破坏

### AC6: 三项预飞
- [ ] `mvn clean test` 全模块通过
- [ ] `mvn checkstyle:check` 无违规
- [ ] `mvn exec:java -Dexec.mainClass="com.claudej.checks.EntropyCheck"` 通过

### AC7: 文档
- [ ] `docs/ops/authorization.md`：角色定义、端点权限映射表、JWT claims 说明
- [ ] 更新 `docs/architecture/decisions/003-auth-aggregate-separation.md`（如相关）

---

## 技术方案

### 角色存储方案

**选型**：`t_user.roles` 用逗号分隔字符串（如 `"USER,ADMIN"`）

理由：
- 简单，无需新增角色表
- 适合当前只有 2-3 个角色的场景
- MyBatis-Plus 可用 `typeHandler` 自动映射 `Set<Role>`
- 未来如需动态权限可再迁移

### JWT Claims 结构

```json
{
  "sub": "user@example.com",
  "roles": ["USER", "ADMIN"],
  "iat": 1714012800,
  "exp": 1714016400
}
```

### SecurityFilterChain 配置草案

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/**").permitAll()
            .requestMatchers("/actuator/**").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
            .accessDeniedHandler(new JwtAccessDeniedHandler())
        );
    return http.build();
}
```

### 依赖

```xml
<!-- 已有 spring-boot-starter-security（可能在 parent pom） -->
<!-- 需要确认是否已引入，如未引入需添加 -->
```

---

## 风险与回退

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 改造 JwtAuthenticationFilter 破坏现有认证 | 高 | 先写测试再改代码；保留原有 token 解析逻辑，仅新增 authority 构建 |
| Spring Security 6.x 与现有配置冲突 | 中 | 当前使用 Spring Boot 3.x（Security 6.x），`authorizeHttpRequests` 而非 `authorizeRequests` |
| H2 集成测试因 Security 配置失败 | 中 | 测试配置类 `@TestConfiguration` 放行测试端点，或 Mock SecurityContext |

---

## 参考文档

- industry-gap-analysis.md C1 项
- 009-auth-system 实现（archived）
- Spring Security 官方文档：https://docs.spring.io/spring-security/reference/

---

## 变更记录

| 日期 | 变更 | 作者 |
|------|------|------|
| 2026-04-25 | 初稿，基于 industry-gap-analysis C1 项细化 | Hermes PM |
