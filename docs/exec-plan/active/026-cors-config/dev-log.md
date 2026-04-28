# 开发日志 — 026-cors-config

## 问题记录

<!--
每条问题必须四段齐全（Issue / Root Cause / Fix / Verification），不得只记"决策"。
理由：违反 VERIFICATION 铁律的举证精神——没有 Verification 行的条目等于未证实。
Build 阶段 handoff 前请 self-check 所有条目；若缺 Verification 行 → 不得提交 handoff。
-->

### 1. Spec 阶段初始化
- **Issue**：当前阶段仅执行设计，不允许编写 `src/main/java` 代码；需要先明确 CORS 应落在安全链而不是控制器或领域层。
- **Root Cause**：CORS 属于 HTTP 横切配置，若设计阶段未先固定边界，Build 阶段容易出现把跨域规则散落到 `@CrossOrigin` 或 `WebMvcConfigurer` 的偏移实现。
- **Fix**：在 `requirement-design.md` 中明确采用 `SecurityFilterChain + CorsConfigurationSource + @ConfigurationProperties` 方案，并标注 domain/application/infrastructure 零影响。
- **Verification**：`git diff -- /Users/macro.li/aiProject/claude-j/docs/exec-plan/active/026-cors-config` → `仅包含 Spec 文档产出，无业务代码改动`

### 2. Security 链未接入 CORS
- **Issue**：新增的跨域测试在 Red 阶段失败，白名单预检 `OPTIONS` 请求被拦为 `401`，未认证跨域请求也缺少 `Access-Control-Allow-Origin` 响应头。
- **Root Cause**：`/Users/macro.li/aiProject/claude-j/claude-j-adapter/src/main/java/com/claudej/adapter/auth/security/SecurityConfig.java` 原本未调用 `.cors()`，导致 Spring Security 过滤链没有启用统一 CORS 协商。
- **Fix**：新增 `/Users/macro.li/aiProject/claude-j/claude-j-start/src/main/java/com/claudej/config/CorsProperties.java` 与 `/Users/macro.li/aiProject/claude-j/claude-j-start/src/main/java/com/claudej/config/CorsConfig.java`，并在 `/Users/macro.li/aiProject/claude-j/claude-j-adapter/src/main/java/com/claudej/adapter/auth/security/SecurityConfig.java` 注入 `CorsConfigurationSource` 后启用 `.cors().configurationSource(corsConfigurationSource)`。
- **Verification**：Red：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml test -pl claude-j-adapter -Dtest=SecurityCorsConfigTest` → `Tests run: 3, Failures: 3, Errors: 0`；Green：同命令 → `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`

### 3. CORS 白名单配置需要受控绑定与校验
- **Issue**：需要在不放宽生产默认值的前提下，为 dev 环境提供本地前端白名单，并保证开启 CORS 时白名单不能为空。
- **Root Cause**：若直接在安全配置中硬编码白名单，环境策略会泄漏到 adapter；若不校验 `allowed-origins`，`enabled=true` 时可能以空白名单启动，行为不透明。
- **Fix**：在 `/Users/macro.li/aiProject/claude-j/claude-j-start/src/main/java/com/claudej/config/CorsProperties.java` 使用 `@ConfigurationProperties + @Validated + @AssertTrue` 校验，在 `/Users/macro.li/aiProject/claude-j/claude-j-start/src/main/resources/application.yml` 保持默认收敛，在 `/Users/macro.li/aiProject/claude-j/claude-j-start/src/main/resources/application-dev.yml` 增加本地开发来源白名单。
- **Verification**：Red：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml test -pl claude-j-start -Dtest=CorsPropertiesTest` → `cannot find symbol CorsProperties`；Green：同命令 → `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`

### 4. 全量预飞暴露既有 infrastructure 装配失败
- **Issue**：本任务定向测试通过后，`mvn clean test` 仍失败，无法将 handoff 标记为预飞全绿。
- **Root Cause**：失败集中在 `claude-j-infrastructure`，根因是既有 `/Users/macro.li/aiProject/claude-j/claude-j-infrastructure/src/main/java/com/claudej/infrastructure/inventory/event/InventoryEventListener.java` 依赖 `InventoryApplicationService`，但 infrastructure 的 `@SpringBootTest` 上下文中缺少该 Bean，导致 `CartRepositoryImplTest` 等 78 个错误；这不是本次 CORS 改动引入的直接依赖变化。
- **Fix**：本轮保持最小改动原则，不扩展到无关 infrastructure 修复；在 handoff 中如实记录 `mvn clean test` 失败事实与根因，供后续单独修复该基线问题。
- **Verification**：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml clean test` → `claude-j-infrastructure` `Tests run: 110, Failures: 0, Errors: 78, Skipped: 0`；`python3` 读取 `/Users/macro.li/aiProject/claude-j/claude-j-infrastructure/target/surefire-reports/com.claudej.infrastructure.cart.persistence.repository.CartRepositoryImplTest.txt` → `No qualifying bean of type 'com.claudej.application.inventory.service.InventoryApplicationService'`

## 待确认
- 生产环境是否需要支持通配子域白名单（如 `https://*.example.com`）。
- 是否需要暴露自定义响应头；当前按最小暴露设计，不额外配置 exposed headers。
- 运维文档最终落点由 Build 阶段结合评审结果确认。

## 变更记录

<!-- 与原设计（requirement-design.md）不一致的变更 -->
<!-- 格式：变更内容 + 变更原因 -->
- 无与原设计不一致的变更。
