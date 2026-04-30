# 任务执行计划 — 027-prometheus-metrics

## 任务状态跟踪

<!-- 状态流转：待办 → 进行中 → 单测通过 → 待验收 → 验收通过 / 待修复 -->

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Domain: 复用订单聚合与指标语义边界确认 | dev | 单测通过 | 不新增领域对象，确认指标不污染 domain |
| 2 | Application: 定义订单指标采集端口 | dev | 单测通过 | 新增 `OrderMetricsPort` 与有限标签分类 |
| 3 | Application: 下单应用服务接入指标 + 单测 | dev | 单测通过 | 覆盖 direct/cart 成功、校验失败、业务失败 |
| 4 | Infrastructure: Micrometer 指标实现 + 测试 | dev | 单测通过 | Counter/Timer 注册与低基数标签封装 |
| 5 | Adapter: 回归订单接口切片测试 | dev | 单测通过 | 确认观测接入不影响 HTTP 契约 |
| 6 | Start: Prometheus registry 依赖与 Actuator 配置 | dev | 单测通过 | 暴露 `/actuator/prometheus` |
| 7 | Start: Prometheus 集成测试 | dev | 单测通过 | 验证端点 200 与指标名可见 |
| 8 | 全量 `mvn test` | dev | 单测通过 | `mvn -s /private/tmp/maven-settings-no-proxy.xml clean test` 通过，reactor BUILD SUCCESS |
| 9 | 全量 `mvn checkstyle:check` | dev | 单测通过 | 0 Checkstyle violations |
| 10 | 全量 `./scripts/entropy-check.sh` | dev | 单测通过 | FAIL=0，WARN=13，status=PASS |
| 11 | QA: 测试用例设计 | qa | 完成 | 已完成测试设计并据此执行 re-verify |
| 12 | QA: 验收测试 + 代码审查 | qa | 完成 | 2026-04-30 独立复跑专项回归、全量测试、Checkstyle、entropy-check 均通过；结论：验收通过 |

## 执行顺序
domain 边界确认 → application 端口 → application 服务 + 测试 → infrastructure 指标实现 → adapter 回归测试 → start 配置与集成测试 → 全量测试 → QA 验收

## 原子任务分解（每项 10–15 分钟，单会话可完成并 commit）

> **目的**：将上表「按层」的粗粒度任务拆到 10–15 分钟的原子级，便于 Ralph Loop 单轮执行完整交付、便于新会话恢复时定位进度。
>
> **要求**：每个原子任务必填 5 个字段 — `文件路径`、`骨架片段`、`验证命令`、`预期输出`、`commit 消息`。

### 1.1 Domain 边界确认
- **文件**：`/Users/macro.li/aiProject/claude-j/docs/exec-plan/active/027-prometheus-metrics/requirement-design.md`
- **测试**：无新增 domain 测试；本任务明确不修改聚合不变量
- **骨架**：在设计文档中确认 `OrderId` / `CustomerId` 禁止作为指标标签，指标逻辑停留在 application/infrastructure
- **验证命令**：`git diff -- /Users/macro.li/aiProject/claude-j/docs/exec-plan/active/027-prometheus-metrics/requirement-design.md`
- **预期输出**：设计文档包含标签高基数控制与领域边界说明
- **commit**：`docs(spec): define metrics domain boundary`

### 2.1 Application 指标端口 Red
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-application/src/test/java/com/claudej/application/order/service/OrderApplicationServiceTest.java`
- **测试**：新增下单成功/失败指标断言
- **骨架**（Red 阶段先写测试）：
  ```java
  @Test
  void should_record_success_metric_when_create_order_succeeds() { ... }

  @Test
  void should_record_failure_metric_when_create_order_from_cart_business_error() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-application -Dtest=OrderApplicationServiceTest`
- **预期输出**：先红，失败原因是缺少指标端口调用或依赖
- **commit**：`test(application): add order metrics red tests`

### 2.2 Application 指标端口 Green
- **文件**：
  - `/Users/macro.li/aiProject/claude-j/claude-j-application/src/main/java/com/claudej/application/order/port/OrderMetricsPort.java`
  - `/Users/macro.li/aiProject/claude-j/claude-j-application/src/main/java/com/claudej/application/order/service/OrderApplicationService.java`
- **测试**：复用 `OrderApplicationServiceTest`
- **骨架**：构造函数注入 `OrderMetricsPort`，围绕 `createOrder` / `createOrderFromCart` 记录 success/failure/duration
- **验证命令**：`mvn test -pl claude-j-application -Dtest=OrderApplicationServiceTest`
- **预期输出**：`Tests run: X, Failures: 0, Errors: 0`
- **commit**：`feat(application): record order creation metrics`

### 3.1 Infrastructure Micrometer 适配 Red
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-infrastructure/src/test/java/com/claudej/infrastructure/order/metrics/MicrometerOrderMetricsRecorderTest.java`
- **测试**：使用 `SimpleMeterRegistry` 验证 counter/timer/tag 注册
- **骨架**：
  ```java
  @Test
  void should_increment_failure_counter_with_reason_type_tag() { ... }

  @Test
  void should_record_timer_with_outcome_tag() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-infrastructure -Dtest=MicrometerOrderMetricsRecorderTest`
- **预期输出**：先红，失败原因是实现类不存在
- **commit**：`test(infrastructure): add micrometer metrics red tests`

### 3.2 Infrastructure Micrometer 适配 Green
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-infrastructure/src/main/java/com/claudej/infrastructure/order/metrics/MicrometerOrderMetricsRecorder.java`
- **测试**：复用 `MicrometerOrderMetricsRecorderTest`
- **骨架**：基于 `MeterRegistry` 注册 `claudej_order_create_total`、`claudej_order_create_failure_total`、`claudej_order_create_duration`
- **验证命令**：`mvn test -pl claude-j-infrastructure -Dtest=MicrometerOrderMetricsRecorderTest`
- **预期输出**：Counter 和 Timer 注册成功，标签仅出现 `source` / `reason_type` / `outcome`
- **commit**：`feat(infrastructure): implement order micrometer metrics`

### 4.1 Adapter 回归验证
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-adapter/src/test/java/com/claudej/adapter/order/web/OrderControllerTest.java`
- **测试**：补充或复用切片测试，确保新增应用服务依赖后控制器仍可装配
- **骨架**：
  ```java
  @Test
  void should_return_200_when_create_order_request_valid() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-adapter -Dtest=OrderControllerTest`
- **预期输出**：HTTP 契约测试全绿
- **commit**：`test(adapter): keep order controller contract green`

### 5.1 Start Prometheus 端点 Red
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-start/src/test/java/com/claudej/actuator/ActuatorPrometheusIntegrationTest.java`
- **测试**：新增集成测试验证 `/actuator/prometheus`
- **骨架**：
  ```java
  @Test
  void should_return_200_when_actuator_prometheus_endpoint() { ... }

  @Test
  void should_expose_order_metrics_when_prometheus_scraped() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-start -Dtest=ActuatorPrometheusIntegrationTest`
- **预期输出**：先红，失败原因是依赖或 exposure 配置缺失
- **commit**：`test(start): add prometheus actuator red tests`

### 5.2 Start Prometheus 端点 Green
- **文件**：
  - `/Users/macro.li/aiProject/claude-j/claude-j-start/pom.xml`
  - `/Users/macro.li/aiProject/claude-j/claude-j-start/src/main/resources/application.yml`
  - `/Users/macro.li/aiProject/claude-j/claude-j-start/src/main/resources/application-dev.yml`
  - `/Users/macro.li/aiProject/claude-j/claude-j-start/src/test/resources/application-dev.yml`
- **测试**：复用 `ActuatorPrometheusIntegrationTest`
- **骨架**：新增 `micrometer-registry-prometheus` 依赖与 `prometheus` exposure
- **验证命令**：`mvn test -pl claude-j-start -Dtest=ActuatorPrometheusIntegrationTest`
- **预期输出**：端点返回 200，响应正文包含 `claudej_order_create_total`
- **commit**：`feat(start): expose prometheus metrics endpoint`

### 6.1 全量预飞
- **文件**：`/Users/macro.li/aiProject/claude-j/docs/exec-plan/active/027-prometheus-metrics/handoff.md`
- **测试**：
  - `mvn test`
  - `mvn checkstyle:check`
  - `./scripts/entropy-check.sh`
- **骨架**：将真实输出摘要写入 handoff `pre-flight` 与 `summary`
- **验证命令**：`mvn test && mvn checkstyle:check && ./scripts/entropy-check.sh`
- **预期输出**：三项全部 pass，输出摘要可粘贴到 handoff
- **commit**：`docs(handoff): record prometheus pre-flight evidence`

## 开发完成记录
- QA 修复回归：`mvn -s /private/tmp/maven-settings-no-proxy.xml test -pl claude-j-application -am -DfailIfNoTests=false -Dtest=OrderApplicationServiceTest` -> `Tests run: 32, Failures: 0, Errors: 0, Skipped: 0`
- QA 修复回归：`mvn -s /private/tmp/maven-settings-no-proxy.xml test -pl claude-j-start -am -DfailIfNoTests=false -Dtest=ActuatorPrometheusIntegrationTest` -> `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`，reactor `BUILD SUCCESS`
- 全量 `mvn test`：`mvn -s /private/tmp/maven-settings-no-proxy.xml test` -> start 模块 `Tests run: 68, Failures: 0, Errors: 0, Skipped: 0`，reactor `BUILD SUCCESS`
- 架构合规检查：`./scripts/entropy-check.sh` -> `issues: 0, warnings: 14, status: PASS`
- 代码风格检查：`mvn -s /private/tmp/maven-settings-no-proxy.xml checkstyle:check -B` -> `You have 0 Checkstyle violations.`
- 通知 @qa 时间：2026-04-30

## QA 验收记录
- 2026-04-30 @dev 修复 QA 阻塞后重新交付：`ActuatorPrometheusIntegrationTest` 已断言 `OrderMetricsPort` 为 Micrometer 实现、`MeterRegistry` 为 Prometheus registry，且 `/actuator/prometheus` 响应正文包含 `claudej_order_create_total`、`claudej_order_create_failure_total`、`claudej_order_create_duration_seconds`。
- 2026-04-30 @dev 修复 QA 阻塞后重新交付：`OrderApplicationServiceTest` 已覆盖失败路径 duration outcome，验证 business/system/validation 路径不再记录为 `success`。
