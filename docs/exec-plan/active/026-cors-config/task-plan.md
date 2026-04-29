# 任务执行计划 — 026-cors-config

## 任务状态跟踪

<!-- 状态流转：待办 → 进行中 → 单测通过 → 待验收 → 验收通过 / 待修复 -->

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Domain: 本任务无领域对象/端口变更，确认零影响 | dev | 单测通过 | `git diff -- claude-j-domain` 为空 |
| 2 | Application: 本任务无应用服务/DTO 变更，确认零影响 | dev | 单测通过 | `git diff -- claude-j-application` 为空 |
| 3 | Infrastructure: 本任务无 DO/Mapper/Repository 变更，确认零影响 | dev | 单测通过 | `git diff -- claude-j-infrastructure/src/main` 为空 |
| 4 | Start: CORS 配置属性绑定测试（Red→Green） | dev | 单测通过 | `CorsPropertiesTest` 2/2 通过 |
| 5 | Start: CORS 配置类与环境配置实现 | dev | 单测通过 | `CorsConfig` + yml 已落地 |
| 6 | Adapter: Security CORS 行为测试（Red→Green） | dev | 单测通过 | `SecurityCorsConfigTest` 3/3 通过 |
| 7 | Adapter: SecurityConfig 接入 CORS 配置 | dev | 单测通过 | 预检/401 语义验证通过 |
| 8 | Start/Docs: 运维说明与路线图更新 | dev | 单测通过 | 本轮仅更新 application.yml / application-dev.yml；未改 roadmap |
| 9 | 全量 mvn test | dev | 单测通过 | `mvn clean test` 全量通过；domain 673 + application 130 + adapter 111 + infrastructure 110 + start 66 |
| 10 | 全量 mvn checkstyle:check | dev | 单测通过 | `You have 0 Checkstyle violations.` |
| 11 | 全量 ./scripts/entropy-check.sh | dev | 单测通过 | `issues: 0, warnings: 13, status: PASS` |
| 12 | QA: 测试用例设计 | qa | 单测通过 | 已新增 `CorsSecurityIntegrationTest` 作为真实链路验证 |
| 13 | QA: 验收测试 + 代码审查 | qa | 待验收 | Build 返工完成，待 QA 复验 |

## 执行顺序
domain（零影响确认） → application（零影响确认） → infrastructure（零影响确认） → start（配置绑定测试与实现） → adapter（安全测试与接入） → docs → 全量测试 → QA 验收

## 原子任务分解（每项 10–15 分钟，单会话可完成并 commit）

> **目的**：将上表「按层」的粗粒度任务拆到 10–15 分钟的原子级，便于 Ralph Loop 单轮执行完整交付、便于新会话恢复时定位进度。
>
> **要求**：每个原子任务必填 5 个字段 — `文件路径`、`骨架片段`、`验证命令`、`预期输出`、`commit 消息`。

### 1.1 Domain 零影响确认
- **文件**：`/Users/macro.li/aiProject/claude-j/docs/exec-plan/active/026-cors-config/requirement-design.md`
- **测试**：无
- **骨架**：记录“CORS 为横切配置，不新增聚合/值对象/端口接口”。
- **验证命令**：`git diff -- /Users/macro.li/aiProject/claude-j/claude-j-domain`
- **预期输出**：无 domain 生产代码改动
- **commit**：`docs(spec): 记录 cors-config domain 零影响`

### 2.1 Application 零影响确认
- **文件**：`/Users/macro.li/aiProject/claude-j/docs/exec-plan/active/026-cors-config/requirement-design.md`
- **测试**：无
- **骨架**：记录“CORS 不新增 command / DTO / application service”。
- **验证命令**：`git diff -- /Users/macro.li/aiProject/claude-j/claude-j-application`
- **预期输出**：无 application 生产代码改动
- **commit**：`docs(spec): 记录 cors-config application 零影响`

### 3.1 Infrastructure 零影响确认
- **文件**：`/Users/macro.li/aiProject/claude-j/docs/exec-plan/active/026-cors-config/requirement-design.md`
- **测试**：无
- **骨架**：记录“CORS 不涉及 DO / Mapper / Repository 实现”。
- **验证命令**：`git diff -- /Users/macro.li/aiProject/claude-j/claude-j-infrastructure`
- **预期输出**：无 infrastructure 生产代码改动
- **commit**：`docs(spec): 记录 cors-config infrastructure 零影响`

### 4.1 Start 配置绑定测试
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-start/src/test/java/com/claudej/config/CorsPropertiesTest.java`
- **骨架**（Red 阶段先写测试）：
  ```java
  @Test
  void should_bind_dev_origins_when_profile_config_present() { ... }

  @Test
  void should_fail_when_allowed_origins_missing_in_enabled_mode() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-start -Dtest=CorsPropertiesTest`
- **预期输出**：先看到配置绑定/校验失败，再转为 `Tests run: X, Failures: 0, Errors: 0`
- **commit**：`test(start): 添加 cors 配置绑定测试`

### 5.1 Start CORS 配置实现
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-start/src/main/java/com/claudej/config/CorsProperties.java`
- **测试**：复用 `CorsPropertiesTest`
- **骨架**：`@ConfigurationProperties(prefix = "app.security.cors")` + `@Validated` + `CorsConfigurationSource` 配置类
- **验证命令**：`mvn test -pl claude-j-start -Dtest=CorsPropertiesTest`
- **预期输出**：配置绑定与校验通过
- **commit**：`feat(start): 添加 cors 白名单配置`

### 5.2 Start 环境配置落地
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-start/src/main/resources/application.yml`、`/Users/macro.li/aiProject/claude-j/claude-j-start/src/main/resources/application-dev.yml`
- **测试**：复用 `CorsPropertiesTest` 或轻量上下文测试
- **骨架**：默认最小暴露 + dev 本地前端白名单
- **验证命令**：`mvn test -pl claude-j-start -Dtest=CorsPropertiesTest`
- **预期输出**：dev 配置可绑定，默认配置不过度开放
- **commit**：`feat(start): 配置开发与默认 cors 策略`

### 6.1 Adapter Security CORS 行为测试
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-adapter/src/test/java/com/claudej/adapter/auth/security/SecurityCorsConfigTest.java`
- **测试**：`@WebMvcTest` 或安全切片测试，加载现有 `SecurityConfig` 与 MockMvc
- **骨架**：
  ```java
  @Test
  void should_return_cors_headers_when_preflight_request_from_allowed_origin() { ... }

  @Test
  void should_return_401_when_cross_origin_request_without_jwt() { ... }

  @Test
  void should_not_allow_when_origin_not_whitelisted() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-adapter -Dtest=SecurityCorsConfigTest`
- **预期输出**：先红后绿，覆盖 OPTIONS/401/非白名单来源
- **commit**：`test(adapter): 添加 security cors 行为测试`

### 7.1 Adapter 接入 CORS 配置
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-adapter/src/main/java/com/claudej/adapter/auth/security/SecurityConfig.java`
- **测试**：复用 `SecurityCorsConfigTest`
- **骨架**：在 `SecurityFilterChain` 中启用 `.cors().configurationSource(...)`
- **验证命令**：`mvn test -pl claude-j-adapter -Dtest=SecurityCorsConfigTest`
- **预期输出**：白名单预检成功，未认证跨域请求仍为 401
- **commit**：`feat(adapter): 接入 cors 安全配置`

### 8.1 运维说明与路线图更新
- **文件**：`/Users/macro.li/aiProject/claude-j/docs/roadmap/industry-gap-analysis.md` 或 Build 阶段确认的运维文档
- **测试**：无
- **骨架**：说明配置键、环境覆盖方式、C3 完成条件
- **验证命令**：`git diff -- /Users/macro.li/aiProject/claude-j/docs/roadmap/industry-gap-analysis.md`
- **预期输出**：仅包含 CORS 相关说明；C3 状态在验收通过后更新
- **commit**：`docs(roadmap): 标注 cors 配置完成`

### 9.1 全量验证
- **文件**：`/Users/macro.li/aiProject/claude-j/docs/exec-plan/active/026-cors-config/handoff.md`
- **测试**：`mvn test`、`mvn checkstyle:check`、`./scripts/entropy-check.sh`
- **骨架**：将真实输出摘要写入 `pre-flight` 与 `summary`
- **验证命令**：`mvn test && mvn checkstyle:check && ./scripts/entropy-check.sh`
- **预期输出**：三项全部通过，带真实统计摘要
- **commit**：`docs(handoff): 更新 cors-config 预飞结果`

## 开发完成记录
<!-- dev 完成后填写 -->
- 全量 `mvn clean test`：通过；domain `Tests run: 673, Failures: 0, Errors: 0, Skipped: 0`，application `Tests run: 130, Failures: 0, Errors: 0, Skipped: 0`，adapter `Tests run: 111, Failures: 0, Errors: 0, Skipped: 0`，infrastructure `Tests run: 110, Failures: 0, Errors: 0, Skipped: 0`，start `Tests run: 66, Failures: 0, Errors: 0, Skipped: 0`
- 架构合规检查：`mvn checkstyle:check -B` 通过（0 violations）；`./scripts/entropy-check.sh` 通过（issues: 0, warnings: 13, status: PASS）
- 通知 @qa 时间：待填写

## QA 验收记录
<!-- qa 验收后填写 -->
- 全量测试（含集成测试）：待填写
- 代码审查结果：待填写
- 代码风格检查：待填写
- 问题清单：详见 test-report.md
- **最终状态**：待填写
