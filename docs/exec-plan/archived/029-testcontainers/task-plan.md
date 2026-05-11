# 任务执行计划 — 029-testcontainers

## 任务状态跟踪

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Spec: 需求设计与执行计划 | dev | 待验收 | 聚焦持久化测试保真度 |
| 2 | Infrastructure: 提取共享 MySQL Testcontainers 引导 | dev | 待验收 | 已提取共享 MySQL 引导并清理 H2 datasource 覆盖 |
| 3 | Infrastructure: 迁移其余仓储集成测试到共享引导 | dev | 待验收 | payment / notification 手工 DDL 已删除，其余仓储测试已统一继承共享基类 |
| 4 | Start: 迁移 FlywayVerificationTest 到真实 MySQL | dev | 待验收 | 不扩展其他 start 测试 |
| 5 | Targeted verification | dev | 待验收 | 3 组定向命令已通过，使用 `DOCKER_HOST=unix:///Users/macro.li/.docker/run/docker.sock` + `-Dapi.version=1.41` |
| 6 | Full verification | dev | 待修复 | `mvn test` 仍因 start 模块既有非本任务用例失败；`checkstyle` / `entropy-check` 通过 |
| 7 | QA: 测试用例设计 | qa | 完成 | 已创建 `test-case-design.md`，覆盖 4 条 AC 与定向验证矩阵 |
| 8 | QA: 验收测试 + 代码审查 | qa | 完成 | 定向验证通过并产出 `test-report.md`；全量 31 个错误判定为任务外 |

## 执行顺序
infrastructure test bootstrap → repository tests → start flyway test → targeted verification → full verification → QA 验收

## 原子任务分解

### 2.1 Infrastructure 共享容器引导
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-infrastructure/src/test/java/com/claudej/infrastructure/test/MySqlRepositoryIntegrationTestSupport.java`、`/Users/macro.li/aiProject/claude-j/claude-j-infrastructure/pom.xml`、`/Users/macro.li/aiProject/claude-j/claude-j-infrastructure/src/test/resources/application.yml`
- **骨架**：测试基类声明 `MySQLContainer`、`@DynamicPropertySource`，由容器注入 datasource，保留共享 Flyway 配置
- **验证命令**：`mvn test -pl claude-j-infrastructure -Dtest=OrderRepositoryImplTest`
- **预期输出**：先暴露 H2/MySQL 差异，再转绿；最终 `Tests run: 9, Failures: 0, Errors: 0`
- **commit**：`test(infrastructure): move repository tests to mysql testcontainers`

### 3.1 迁移 payment / notification 特例
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-infrastructure/src/test/java/com/claudej/infrastructure/payment/persistence/repository/PaymentRepositoryImplTest.java`、`/Users/macro.li/aiProject/claude-j/claude-j-infrastructure/src/test/java/com/claudej/infrastructure/notification/persistence/repository/NotificationRepositoryImplTest.java`
- **骨架**：删除手写 H2 DDL，引导 Flyway 在 MySQL 容器中建表
- **验证命令**：`mvn test -pl claude-j-infrastructure -Dtest=PaymentRepositoryImplTest,NotificationRepositoryImplTest`
- **预期输出**：`Failures: 0, Errors: 0`
- **commit**：`test(infrastructure): migrate payment and notification repository tests`

### 3.2 扩展到其余仓储测试
- **文件**：各 `*RepositoryImplTest`
- **骨架**：移除类级 H2 datasource 覆盖，统一继承共享基类
- **验证命令**：`mvn test -pl claude-j-infrastructure`
- **预期输出**：仓储测试均可在 MySQL 8 容器下运行
- **commit**：`test(infrastructure): align repository tests on shared mysql bootstrap`

### 4.1 Start Flyway MySQL 验证
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-start/src/test/java/com/claudej/start/flyway/MySqlFlywayIntegrationTestSupport.java`、`/Users/macro.li/aiProject/claude-j/claude-j-start/src/test/java/com/claudej/start/flyway/FlywayVerificationTest.java`
- **骨架**：Flyway 验证测试独享 MySQL 容器，并基于 `DATABASE()` / `information_schema.tables` 断言迁移结果
- **验证命令**：`mvn test -pl claude-j-start -Dtest=FlywayVerificationTest`
- **预期输出**：`Tests run: 2, Failures: 0, Errors: 0`
- **commit**：`test(start): verify flyway against mysql container`

### 5.1 定向回归
- **文件**：`/Users/macro.li/aiProject/claude-j/docs/exec-plan/active/029-testcontainers/dev-log.md`
- **骨架**：记录 3 组定向验证命令与摘要
- **验证命令**：题目指定的 3 条命令
- **预期输出**：如 Docker 可用则全部通过；不可用则如实记录
- **commit**：`docs(exec-plan): capture targeted verification evidence`

### 6.1 全量预飞
- **文件**：`/Users/macro.li/aiProject/claude-j/docs/exec-plan/active/029-testcontainers/handoff.md`、`/Users/macro.li/aiProject/claude-j/docs/exec-plan/active/029-testcontainers/dev-log.md`
- **骨架**：附三项检查真实输出摘要，不声称未执行项通过
- **验证命令**：`mvn test && mvn checkstyle:check && ./scripts/entropy-check.sh`
- **预期输出**：真实记录 pass / fail / blocked
- **commit**：`docs(exec-plan): update handoff with verification evidence`

## 开发完成记录
- 2026-05-07：已引入模块级共享 MySQL 8 Testcontainers 基类，仓储测试与 FlywayVerificationTest 已改为通过 `@DynamicPropertySource` 注入容器 datasource。
- 2026-05-07：已删除 payment / notification 测试中的手工 H2 DDL 初始化，改为复用现有 Flyway 迁移。
- 2026-05-09：通过 `DOCKER_HOST=unix:///Users/macro.li/.docker/run/docker.sock` 与 `-Dapi.version=1.41` 打通 Testcontainers 到 Docker Desktop 的连接。
- 2026-05-09：已将 Flyway 升级到 `9.22.3` 并新增 test-scope `flyway-mysql`，同时修复 inventory / payment / notification 迁移脚本的 MySQL 兼容性；3 组定向验证全部通过。
- 2026-05-09：`mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml checkstyle:check` 通过；`./scripts/entropy-check.sh` 通过；全量 `mvn test` 仍有 31 个 start 模块既有集成测试失败，超出本任务范围。

## QA 验收记录
- 2026-05-09：QA 已完成 `test-case-design.md` 与 `test-report.md`，独立复跑 3 组定向 Testcontainers 验证全部通过；`mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml checkstyle:check` 通过；`./scripts/entropy-check.sh` 通过。
- 2026-05-09：QA 复跑全量 `DOCKER_HOST=unix:///Users/macro.li/.docker/run/docker.sock mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml -Dapi.version=1.41 test` 结果为 `Tests run: 379, Failures: 0, Errors: 31`，首个失败位于 `claude-j-adapter` 的 `OrderControllerTest`，不阻塞 029 窄范围验收。
