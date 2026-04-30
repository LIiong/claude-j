# 开发日志 — 027-prometheus-metrics

## 问题记录

### 1. 指标采集层级归属待明确
- **Issue**：Spec 阶段识别到需求包含 Micrometer/Prometheus 技术能力，但业务指标又与订单下单用例强相关，若直接在 `OrderApplicationService` 注入 `MeterRegistry` 会触碰六边形边界。
- **Root Cause**：观测需求跨越 `start` 装配与 `application` 用例编排两个层次，若不先定义端口，Build 阶段容易把第三方技术对象直接耦合进 application。
- **Fix**：在 `requirement-design.md` 中明确采用 `OrderMetricsPort` 端口方案，由 application 发出观测意图、由 infrastructure/start 实现 Micrometer 适配。
- **Verification**：`git diff -- /Users/macro.li/aiProject/claude-j/docs/exec-plan/active/027-prometheus-metrics/requirement-design.md` -> `requirement-design.md` 已包含方案 A/B 对比、端口设计与最终取舍。

### 2. 失败率表达方式需避免冗余状态
- **Issue**：需求提到“下单失败率”，存在直接在应用侧维护失败率 Gauge 的诱惑。
- **Root Cause**：失败率属于时间窗口上的派生统计，不是订单聚合的原生状态；若在应用内维护比率，会引入额外状态同步与线程安全复杂度。
- **Fix**：在 `requirement-design.md` 中规定只落地 success/failure counter 与 duration timer，失败率统一由 PromQL 查询计算。
- **Verification**：`git diff -- /Users/macro.li/aiProject/claude-j/docs/exec-plan/active/027-prometheus-metrics/requirement-design.md` -> 设计文档已写明 failure rate PromQL 与“不新增 Gauge”原则。

### 3. Spring Boot 测试上下文默认禁用 metrics export
- **Issue**：`ActuatorPrometheusIntegrationTest` 初始运行时 `/actuator/prometheus` 返回 404。
- **Root Cause**：Spring Boot 2.7 的测试上下文自动注入 `MetricsExportContextCustomizerFactory$DisableMetricExportContextCustomizer`，默认禁用非 simple metrics export，导致 Prometheus scrape endpoint 未注册。
- **Fix**：在 `ActuatorPrometheusIntegrationTest` 显式添加 `@AutoConfigureMetrics`，使测试上下文启用 metrics export。
- **Verification**：`mvn -s /private/tmp/maven-settings-no-proxy.xml test -pl claude-j-start -Dtest=ActuatorPrometheusIntegrationTest` -> `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`，同时 `Exposing 5 endpoint(s) beneath base path '/actuator'`。

### 4. Build blocker：infrastructure 端 metrics 端口编译可见性失效
- **Issue**：最后自动 Build 修复轮次无法让 `OrderRepositoryImplTest` 真实执行到测试方法。
- **Root Cause**：`claude-j-infrastructure` 单模块命令未带 `-am` 时使用本地仓库旧版 `claude-j-application`，旧包中没有 `com.claudej.application.order.port.OrderMetricsPort`，导致 `OrderMetricsConfiguration.java`、`MicrometerOrderMetricsRecorder.java`、`NoOpOrderMetricsPort.java` 编译失败。
- **Fix**：使用 `-am` 让 Maven reactor 同步构建上游模块，并将 `OrderRepositoryImplTest` 恢复为可被 Surefire 发现的 `@SpringBootTest`；测试上下文只扫描订单仓储、转换器和订单 mapper，避免重新拉起整个 infrastructure 包。
- **Verification**：`mvn -s /private/tmp/maven-settings-no-proxy.xml clean test -pl claude-j-infrastructure -am -DfailIfNoTests=false -Dtest=OrderRepositoryImplTest` -> `Running com.claudej.infrastructure.order.persistence.repository.OrderRepositoryImplTest`，`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`。

### 5. Prometheus 测试上下文装配了 NoOp 指标端口
- **Issue**：`ActuatorPrometheusIntegrationTest` 在断言订单指标名时持续失败，`/actuator/prometheus` 只暴露通用 JVM/HTTP 指标，不含订单指标。
- **Root Cause**：`OrderMetricsConfiguration` 依赖 `@ConditionalOnBean(MeterRegistry.class)` 与 `@ConditionalOnMissingBean(OrderMetricsPort.class)` 做双 bean 条件装配；在 start 测试上下文里，条件判断发生时 `MeterRegistry` 尚未满足，导致 fallback 的 `NoOpOrderMetricsPort` 先成为唯一 `OrderMetricsPort`，后续即使 Prometheus registry 已存在也不会再切回 Micrometer 实现。
- **Fix**：将 `OrderMetricsConfiguration` 收敛为单一 `orderMetricsPort(ObjectProvider<MeterRegistry>)` bean，在 bean 创建时通过 `ObjectProvider` 延迟判断 registry 是否可用；有 registry 时返回 `MicrometerOrderMetricsRecorder`，否则返回 `NoOpOrderMetricsPort`，避免条件装配时序问题且不引入 Prometheus 具体类型依赖。
- **Verification**：Red：`mvn -s /private/tmp/maven-settings-no-proxy.xml test -pl claude-j-start -am -DfailIfNoTests=false -Dtest=ActuatorPrometheusIntegrationTest` -> `Tests run: 2, Failures: 1, Errors: 0, Skipped: 0`，`Expecting actual: "com.claudej.infrastructure.order.persistence.metrics.NoOpOrderMetricsPort" to contain: "MicrometerOrderMetricsRecorder"`。Green：`mvn -s /private/tmp/maven-settings-no-proxy.xml test -pl claude-j-start -am -DfailIfNoTests=false -Dtest=ActuatorPrometheusIntegrationTest` -> `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`，reactor `BUILD SUCCESS`。

## 变更记录

- 为 Prometheus 集成测试显式添加 `@AutoConfigureMetrics`，避免测试上下文默认禁用 metrics export。
- `src/test/resources/application-dev.yml` 保留 `prometheus` 暴露配置，确保 dev profile 下端点可见。
- 未引入额外需求外指标，仍仅使用 `source` / `reason_type` / `outcome` 低基数标签。
- 修复 `OrderRepositoryImplTest` 测试装配：保留 `@SpringBootTest` 可发现性，仅扫描订单仓储所需 Bean 与 mapper。
- `OrderMetricsConfiguration` 改为 `ObjectProvider<MeterRegistry>` 延迟选择实现，修复 start 测试上下文误装配 `NoOpOrderMetricsPort` 的问题。
- 全量预飞已通过：`mvn -s /private/tmp/maven-settings-no-proxy.xml test`、`mvn -s /private/tmp/maven-settings-no-proxy.xml checkstyle:check -B`、`./scripts/entropy-check.sh`。
