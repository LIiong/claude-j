# 开发日志 — 030-full-suite-recovery

## 问题记录

<!--
每条问题必须四段齐全（Issue / Root Cause / Fix / Verification），不得只记"决策"。
理由：违反 VERIFICATION 铁律的举证精神——没有 Verification 行的条目等于未证实。
Build 阶段 handoff 前请 self-check 所有条目；若缺 Verification 行 → 不得提交 handoff。
-->

### 1. Spec 阶段上下文恢复与任务边界确认
- **Issue**：029 已 QA 通过，但其交接中明确保留了全量 `mvn test` 仍有 31 个错误；030 需要在不回退 029 MySQL Testcontainers 改进的前提下恢复 full suite，同时不得把 unrelated surviving failures 直接折叠进当前任务。
- **Root Cause**：029 的验收边界刻意限定为仓储 Testcontainers 路径与 `FlywayVerificationTest`，未承担 full suite recovery；因此需要新增独立 follow-up task，先做失败归类和范围收敛。
- **Fix**：新建 `030-full-suite-recovery` 任务目录，并在 Spec 文档中明确目标、非目标、029 回归守护、failure classification 优先级，以及“独立失败再次拆任务”的边界。
- **Verification**：`文档交叉阅读` → `已读取 029 handoff/test-report、架构文档、开发流程文档与 exec-plan 模板，并生成 030 的 requirement-design/task-plan/handoff 初稿`

### 2. Start 模块 H2/Flyway bootstrap 漂移
- **Issue**：按用户要求先执行 failure classification 时，`mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml test -pl claude-j-start -Dtest=CartIntegrationTest,OrderFromCartIntegrationTest,MessageQueueOrderIntegrationTest` 在 Spring 上下文启动阶段失败，首个异常为 `Migration V10__add_inventory.sql failed`，H2 报 `Index "IDX_SKU_CODE" already exists`。
- **Root Cause**：`/Users/macro.li/aiProject/claude-j/claude-j-start/src/main/resources/application-test.yml` 在 029 后仍指向 `classpath:db/migration`，导致 `@ActiveProfiles("test")` 的 H2 start 集成测试执行了面向 MySQL 的主迁移脚本；V10 里的内联 `KEY idx_sku_code` 在 H2 MySQL mode 下被重复解析成重复索引，从而阻断 full suite。
- **Fix**：仅恢复 start 模块 `test` profile 的 Flyway 配置到 H2 shadow 迁移：`locations=classpath:db/migration-h2`、`validate-on-migrate=false`、保留 `baseline-on-migrate=true`、`clean-disabled=false`、`out-of-order=true`；不改 029 的 MySQL Testcontainers 支撑类与主迁移 SQL。
- **Verification**：
  - `Red`：`mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml test -pl claude-j-start -Dtest=CartIntegrationTest,OrderFromCartIntegrationTest,MessageQueueOrderIntegrationTest` → `BUILD FAILURE`，`Migration V10__add_inventory.sql failed`，`Index "IDX_SKU_CODE" already exists`
  - `Green canary`：`mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml test -pl claude-j-start -am -DfailIfNoTests=false -Dtest=CartIntegrationTest,OrderFromCartIntegrationTest,MessageQueueOrderIntegrationTest` → `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`
  - `029 guard`：`DOCKER_HOST=unix:///Users/macro.li/.docker/run/docker.sock mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml -Dapi.version=1.41 test -pl claude-j-start -Dtest=FlywayVerificationTest` → `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`

### 3. Start canary 单模块执行出现 NotificationRepository 缺失
- **Issue**：在修正 H2 Flyway 路径后，直接执行 `mvn ... test -pl claude-j-start -Dtest=...` 仍报 `No qualifying bean of type 'com.claudej.domain.notification.repository.NotificationRepository'`。
- **Root Cause**：这不是新的业务/装配缺陷，而是局部命令只测试 `claude-j-start`、未联编上游模块时，start 依赖的本地 SNAPSHOT jar 仍是旧版 installed artifact；`~/.m2` 中的 `claude-j-infrastructure-1.0.0-SNAPSHOT.jar` 不包含 029 新增的 notification 仓储实现，因此出现假阳性的 bean 缺失。
- **Fix**：按 full-suite/真实 reactor 边界复核，使用 `-am -DfailIfNoTests=false` 先联编上游模块进行 canary，再执行用户要求的全量 reactor `mvn test`；不为这个局部命令假象去修改生产装配代码。
- **Verification**：
  - `Reproduce`：`mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml test -pl claude-j-start -Dtest=CartIntegrationTest,OrderFromCartIntegrationTest,MessageQueueOrderIntegrationTest` → `BUILD FAILURE`，`No qualifying bean of type '...NotificationRepository'`
  - `Root-cause evidence`：检查本地仓库 jar → `/Users/macro.li/.m2/repository/com/claudej/claude-j-infrastructure/1.0.0-SNAPSHOT/claude-j-infrastructure-1.0.0-SNAPSHOT.jar` 中 notification 类数量 `count=0`
  - `Reactor proof`：`mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml test -pl claude-j-start -am -DfailIfNoTests=false -Dtest=CartIntegrationTest,OrderFromCartIntegrationTest,MessageQueueOrderIntegrationTest` → `BUILD SUCCESS`

### 4. Full suite recovery 与 029 回归守护复核
- **Issue**：修复 start H2 bootstrap 后，需要确认 full suite 已恢复，同时 029 的 MySQL Testcontainers/Flyway 真实 MySQL 路径未被打坏。
- **Root Cause**：030 的核心风险不是单点失败，而是“修 H2 路径时意外回退 029 的 MySQL 保真度改进”；因此必须同步复跑 targeted guards 与 full suite。
- **Fix**：按 030 边界复跑 029 targeted guards（Order/Payment/Notification repository + FlywayVerificationTest），然后执行全量 `mvn test`、`checkstyle`、`entropy-check`。
- **Verification**：
  - `DOCKER_HOST=unix:///Users/macro.li/.docker/run/docker.sock mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml -Dapi.version=1.41 test -pl claude-j-infrastructure -Dtest=OrderRepositoryImplTest` → `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`
  - `DOCKER_HOST=unix:///Users/macro.li/.docker/run/docker.sock mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml -Dapi.version=1.41 test -pl claude-j-infrastructure -Dtest=PaymentRepositoryImplTest,NotificationRepositoryImplTest` → `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`
  - `DOCKER_HOST=unix:///Users/macro.li/.docker/run/docker.sock mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml -Dapi.version=1.41 test -pl claude-j-start -Dtest=FlywayVerificationTest` → `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`
  - `DOCKER_HOST=unix:///Users/macro.li/.docker/run/docker.sock mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml -Dapi.version=1.41 test` → `Tests run: 69, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`
  - `mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml checkstyle:check` → `BUILD SUCCESS`，`You have 0 Checkstyle violations.`
  - `./scripts/entropy-check.sh` → `{"issues": 0, "warnings": 14, "status": "PASS"}`

## 变更记录

- 无与原设计不一致的变更。

## 待确认

- 若 failure classification 后发现多个彼此独立且修复路径差异很大的失败簇，默认先收敛主阻断簇，剩余 surviving unrelated failures 再拆新任务，待 architect 在评审中确认。
- 030 Build 阶段若必须修改生产代码，应限制在直接导致 full suite 失败的最小范围内，不因为“顺手”做跨聚合重构。
