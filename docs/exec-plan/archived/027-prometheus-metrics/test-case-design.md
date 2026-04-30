# 测试用例设计 — 027-prometheus-metrics

## 测试范围
验证 Prometheus 端点暴露、订单指标采集、订单仓储回归，以及指标接入后的分层合规性。

## 测试策略
按测试金字塔分层测试 + 代码审查 + 风格检查。

---

## 0. AC 自动化覆盖矩阵（强制，动笔前先填）

| # | 验收条件（AC） | 自动化层 | 对应测试方法 | 手动备注（若有） |
|---|---|---|---|---|
| AC1 | `dev` 环境启动后可访问 `/actuator/prometheus`，返回 200 | Start（集成） | `ActuatorPrometheusIntegrationTest.should_return_200_when_actuator_prometheus_endpoint` | - |
| AC2 | 返回内容包含 `claudej_order_create_total`、`claudej_order_create_failure_total`、`claudej_order_create_duration_seconds` | Start（集成） | 计划映射 `ActuatorPrometheusIntegrationTest.should_include_order_metrics_when_actuator_prometheus_endpoint` | 当前实现仅断言通用指标，需 QA 复核 |
| AC3 | 直接下单成功后，`source="direct"` 的成功计数递增 | Application | `OrderApplicationServiceTest.should_record_success_metric_when_create_order_succeeds` | - |
| AC4 | 从购物车下单失败时，`source="cart"` 与有限 `reason_type` 标签的失败计数递增 | Application | `OrderApplicationServiceTest.should_record_failure_metric_when_create_order_from_cart_business_error` | - |
| AC5 | 非 `BusinessException` 异常使用 `reason_type="system"` 与 `outcome="system_error"` | Application | `OrderApplicationServiceTest.should_record_system_failure_metric_when_create_order_from_cart_unexpected_error` | 当前未见对 `outcome="system_error"` 的断言，需 QA 复核 |
| AC6 | 失败率通过 PromQL 计算，不在代码中维护 rate/gauge | Infrastructure / 代码审查 | `MicrometerOrderMetricsRecorder` 仅注册 Counter/Timer；无 Gauge 测试 | 通过代码审查验证 |
| AC7 | Build 阶段提供自动化闭环，`OrderRepositoryImplTest` 真实执行 9 个测试 | Infrastructure | `OrderRepositoryImplTest`（9 个 `should_xxx_when_yyy`） | - |
| AC8 | 三项预飞 `mvn test`、`mvn checkstyle:check`、`./scripts/entropy-check.sh` 真实通过 | 全量验证 | QA 复跑全量命令 | - |

---

## 一、Domain 层测试场景

> 本任务不涉及 Domain 生产代码变更，已按模板说明省略新增 Domain 用例；回归依赖全量 `mvn test` 中的 673 个 domain 测试。

---

## 二、Application 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| A1 | 直接下单成功记录成功指标 | 订单保存与 DTO 转换均成功 | 调用 `createOrder(command)` | 调用 `recordCreateOrderSuccess("direct")` |
| A2 | 直接下单参数非法记录校验失败指标 | `command` 为空或订单项为空 | 调用 `createOrder(...)` | 抛 `BusinessException`，调用 `recordCreateOrderFailure("direct", "validation")` |
| A3 | 购物车下单业务失败记录业务失败指标 | 购物车不存在/为空 | 调用 `createOrderFromCart(command)` | 抛 `BusinessException`，调用 `recordCreateOrderFailure("cart", "business")` |
| A4 | 购物车下单系统异常记录系统失败指标 | 仓储抛 `RuntimeException` | 调用 `createOrderFromCart(command)` | 调用 `recordCreateOrderFailure("cart", "system")` |
| A5 | 耗时指标按 outcome 分类 | 成功/业务失败/系统失败三种路径 | 调用创建订单方法 | `recordCreateOrderDuration` 使用真实 outcome 标签 |

---

## 三、Infrastructure 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| I1 | 订单仓储测试真实执行 | 最小 Spring + H2 订单上下文已装配 | 运行 `OrderRepositoryImplTest` | `Tests run: 9`，非 `0` |
| I2 | 保存并按订单号查询 | H2 空库 | `save` → `findByOrderId` | 返回匹配订单与订单项 |
| I3 | 按客户查询与存在性检查 | 同客户下保存多单 | `findByCustomerId` / `existsByOrderId` | 结果正确 |
| I4 | 指标实现保持低基数 | `MeterRegistry` 已装配 | 审查 `MicrometerOrderMetricsRecorder` | 仅存在 `source` / `reason_type` / `outcome` 标签，无 Gauge |

---

## 四、Adapter 层测试场景

> 本任务无新增业务 API；Adapter 层仅做回归，依赖全量 `mvn test` 中的 111 个 adapter 测试确认 HTTP 契约未回归。

---

## 五、集成测试场景（全链路）

| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E1 | Prometheus 端点可访问 | 启动 start 模块测试上下文并请求 `GET /actuator/prometheus` | 返回 200 |
| E2 | Prometheus 输出包含订单指标 | 请求 `GET /actuator/prometheus` | 响应正文包含 `claudej_order_create_total`、`claudej_order_create_failure_total`、`claudej_order_create_duration_seconds` |

---

## 六、代码审查检查项

- [ ] 依赖方向正确（adapter → application → domain ← infrastructure）
- [ ] `OrderMetricsPort` 位于 application，未下沉到 domain 或上浮到 start
- [ ] Micrometer 实现位于 infrastructure，start 仅负责装配与 Actuator 暴露
- [ ] `OrderApplicationService` 仅发出观测意图，不直接依赖 `MeterRegistry`
- [ ] `OrderRepositoryImplTest` 测试上下文已收缩，不扫描无关 listener
- [ ] `/actuator/prometheus` 集成测试覆盖任务要求的订单指标名

## 七、代码风格检查项

- [ ] Java 8 兼容（无 var、records、text blocks、List.of）
- [ ] 包结构符合 `com.claudej.{layer}.{aggregate}.{sublayer}`
- [ ] 测试命名采用 `should_xxx_when_yyy`
- [ ] Application 端口与 infrastructure 实现命名清晰
- [ ] 未引入需求外 Gauge/高基数标签
- [ ] Checkstyle 全量通过
