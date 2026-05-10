# 任务执行计划 — 030-full-suite-recovery

## 任务状态跟踪

<!-- 状态流转：待办 → 进行中 → 单测通过 → 待验收 → 验收通过 / 待修复 -->

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Failure classification：收集 full suite 失败清单、首个异常与分层归因 | dev | 单测通过 | 首个阻断簇确认在 start 模块 H2/Flyway bootstrap drift：`application-test.yml` 指向主迁移，导致 H2 执行 MySQL 索引内联 DDL 失败 |
| 2 | 建立 029 MySQL path 回归守护：锁定仓储 + Flyway 定向绿线 | dev | 单测通过 | `OrderRepositoryImplTest`、`PaymentRepositoryImplTest`、`NotificationRepositoryImplTest`、`FlywayVerificationTest` 均复跑通过 |
| 3 | 修复第 1 批主阻断失败簇并补齐 Red-Green 证据 | dev | 单测通过 | 将 start `test` profile Flyway 路径切回 `classpath:db/migration-h2`，先复现 V10 H2 迁移失败，再验证 canary 转绿 |
| 4 | 复跑 failure classification，确认剩余失败是否同根因 | dev | 单测通过 | 复跑后剩余 `NotificationRepository` 缺失属于同一 start bootstrap 命令未联编上游模块造成的局部现象；按全 reactor `mvn test` 复核后清零 |
| 5 | 修复第 2 批仍在 030 边界内的失败簇 | dev | 单测通过 | 无需新增第 2 批代码修复；full suite 恢复由共享 bootstrap 配置修正闭环 |
| 6 | 全量 end-state verification：`mvn test`、`checkstyle`、`entropy-check` | dev | 单测通过 | 三项验证已真实执行并通过，可转 QA |
| 7 | QA: 测试用例设计 | qa | 完成 | 已创建 `test-case-design.md`，补齐 AC 自动化覆盖矩阵与配置恢复验收场景 |
| 8 | QA: 验收测试 + 代码审查 | qa | 完成 | 已独立重跑 `mvn test` / `checkstyle` / `entropy-check` 并确认未回退 029 MySQL Testcontainers/Flyway 路径 |

## 执行顺序
failure classification → 029 regression guards on MySQL path → clustered fixes with TDD → reclassification if needed → end-state verification → QA 验收

## 原子任务分解（每项 10–15 分钟，单会话可完成并 commit）

> **目的**：保持目标驱动与窄范围推进，优先恢复 full suite，不把 030 扩散成 unrelated cleanup。
>
> **要求**：每个原子任务必填 5 个字段 — `文件路径`、`骨架片段`、`验证命令`、`预期输出`、`commit 消息`。
>
> **说明**：本次 Spec 阶段不创建 commit；以下 commit 字段仅为后续 Ralph Loop / Build 阶段占位。

### 1.1 Failure classification pass
- **文件**：`/Users/macro.li/aiProject/claude-j/docs/exec-plan/active/030-full-suite-recovery/dev-log.md`
- **骨架**：记录 full suite 失败测试类、模块、层级、首个 stack trace、是否疑似 029 回归
- **验证命令**：`DOCKER_HOST=unix:///Users/macro.li/.docker/run/docker.sock mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml -Dapi.version=1.41 test`
- **预期输出**：获得完整失败分类清单；若失败，日志可稳定定位首个阻断簇
- **commit**：`test(start): classify full suite failures after 029`

### 2.1 029 regression guards on MySQL path — repository slice
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-infrastructure/src/test/java/com/claudej/infrastructure/order/persistence/repository/OrderRepositoryImplTest.java`
- **测试**：保留既有 MySQL Testcontainers 支撑与仓储测试契约
- **骨架**：
  ```java
  @Test
  void should_keep_mysql_repository_path_green_when_full_suite_recovery_changes() { ... }
  ```
- **验证命令**：`DOCKER_HOST=unix:///Users/macro.li/.docker/run/docker.sock mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml -Dapi.version=1.41 test -pl claude-j-infrastructure -Dtest=OrderRepositoryImplTest,PaymentRepositoryImplTest,NotificationRepositoryImplTest`
- **预期输出**：`Tests run: 20, Failures: 0, Errors: 0`，029 仓储路径持续绿
- **commit**：`test(infrastructure): guard mysql repository regression during full suite recovery`

### 2.2 029 regression guards on MySQL path — Flyway slice
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-start/src/test/java/com/claudej/start/flyway/FlywayVerificationTest.java`
- **测试**：保留真实 MySQL 8 Testcontainers 迁移校验
- **骨架**：
  ```java
  @Test
  void should_keep_flyway_mysql_verification_green_when_full_suite_recovery_changes() { ... }
  ```
- **验证命令**：`DOCKER_HOST=unix:///Users/macro.li/.docker/run/docker.sock mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml -Dapi.version=1.41 test -pl claude-j-start -Dtest=FlywayVerificationTest`
- **预期输出**：`Tests run: 2, Failures: 0, Errors: 0`
- **commit**：`test(start): guard mysql flyway verification during full suite recovery`

### 3.1 First failing cluster — reproduce in smallest scope
- **文件**：按 failure classification 结果锁定，例如 Adapter 或 Start 对应测试类与最小装配支撑
- **骨架**：先写/调整能稳定看到 Red 的最小复现测试，再写最小修复
- **验证命令**：`mvn -pl {affected-module} -Dtest={FirstFailingTestClass} test`
- **预期输出**：先红后绿，失败原因与 full suite 首个阻断一致
- **commit**：`fix({scope}): recover first full suite blocking cluster`

### 4.1 Reclassification after first fix
- **文件**：`/Users/macro.li/aiProject/claude-j/docs/exec-plan/active/030-full-suite-recovery/dev-log.md`
- **骨架**：追加剩余失败分类，标明已消除簇 / 新暴露簇 / 是否需拆任务
- **验证命令**：`DOCKER_HOST=unix:///Users/macro.li/.docker/run/docker.sock mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml -Dapi.version=1.41 test`
- **预期输出**：失败数下降或失败簇收敛；若出现独立 surviving unrelated failures，明确另拆
- **commit**：`docs(exec-plan): refresh failure classification after first recovery fix`

### 5.1 Remaining in-scope cluster fixes
- **文件**：按剩余失败所在模块最小化处理
- **骨架**：一次只处理一个根因簇，先 Red 再 Green
- **验证命令**：`mvn -pl {affected-module} test -Dtest={ClusterTests}`
- **预期输出**：对应失败簇清零，无 029 回归
- **commit**：`fix({scope}): recover remaining in-scope suite failures`

### 6.1 End-state verification
- **文件**：`/Users/macro.li/aiProject/claude-j/docs/exec-plan/active/030-full-suite-recovery/handoff.md`
- **骨架**：写入三项真实验证摘要与 QA 交接说明
- **验证命令**：`mvn test && mvn checkstyle:check && ./scripts/entropy-check.sh`
- **预期输出**：三项命令全部通过；handoff pre-flight 可填真实 pass 摘要
- **commit**：`docs(exec-plan): hand off full suite recovery to qa`

## 开发完成记录
<!-- dev 完成后填写 -->
- 全量 `mvn clean test`：已完成，`DOCKER_HOST=unix:///Users/macro.li/.docker/run/docker.sock mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml -Dapi.version=1.41 test` → `Tests run: 69, Failures: 0, Errors: 0, Skipped: 0`
- 架构合规检查：已完成，`mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml checkstyle:check` → `BUILD SUCCESS` / `0 Checkstyle violations`；`./scripts/entropy-check.sh` → `{"issues": 0, "warnings": 14, "status": "PASS"}`
- 通知 @qa 时间：待本轮 handoff 更新后

## QA 验收记录
<!-- qa 验收后填写 -->
- 全量测试（含集成测试）：已完成，`DOCKER_HOST=unix:///Users/macro.li/.docker/run/docker.sock mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml -Dapi.version=1.41 test` → `Tests run: 69, Failures: 0, Errors: 0, Skipped: 0`
- 代码审查结果：已完成，确认 `/Users/macro.li/aiProject/claude-j/claude-j-start/src/main/resources/application-test.yml` 仅恢复 H2 test profile Flyway 路径，未回退 029 MySQL Testcontainers/Flyway 路径
- 代码风格检查：已完成，`mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml checkstyle:check` → `BUILD SUCCESS` / `0 Checkstyle violations`；`./scripts/entropy-check.sh` → `{"issues": 0, "warnings": 14, "status": "PASS"}`
- 问题清单：详见 test-report.md（0 个阻塞问题，1 个既有 WARN 改进项）
- **最终状态**：验收通过
