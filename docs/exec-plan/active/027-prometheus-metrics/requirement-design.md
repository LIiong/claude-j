# 需求拆分设计 — 027-prometheus-metrics

## 需求描述
接入 Micrometer 与 Prometheus 指标导出能力，在现有 Spring Boot Actuator 基础上暴露 `/actuator/prometheus` 端点，并补充订单下单相关的关键业务指标，用于观测下单成功量与下单失败率。

本次需求仅覆盖指标采集与暴露，不改变订单领域规则、不新增数据库表；同时需要控制标签维度，避免高基数指标，并为后续 Build 阶段提供可验证的 TDD 验收条件。

## 领域分析

### 聚合根: Order
- `orderId` (`OrderId`) — 订单唯一标识，仅用于领域持久化与业务流转，不作为 Prometheus 标签输出。
- `customerId` (`CustomerId`) — 客户标识，属于高基数数据，不进入指标标签。
- `status` (`OrderStatus`) — 订单状态机结果，可作为有限枚举标签的候选来源，但本次仅在失败场景中归类业务结果，不直接暴露订单状态明细。
- `items` (`List<OrderItem>`) — 下单内容，属于业务数据，不进入指标标签。
- `totalAmount` (`Money`) — 订单金额，可用于 histogram/timer 观测，但本期需求未明确分桶统计，先不引入金额分布指标。

### 值对象
- **OrderId**: 订单标识，唯一且高基数；禁止作为指标标签。
- **CustomerId**: 客户标识，高基数；禁止作为指标标签。
- **OrderStatus**: 有限状态集合；若后续需要可作为低基数标签扩展。
- **Money**: 金额值对象；本期仅用于业务计算，不直接参与指标采集。
- **MetricsOutcome**（拟新增）: 有限结果分类值对象/枚举候选，表示 `success` / `business_error` / `system_error`，用于统一业务指标标签语义。

### 领域服务（如有）
- 无新增领域服务。
- 指标采集属于技术观测能力，不应下沉到 `domain`；订单聚合继续只负责业务不变量。

### 端口接口
- **OrderRepository**: 现有 `save(Order)`、`findByOrderId(OrderId)`、`findByCustomerId(CustomerId)` 等接口维持不变；指标需求不改 Repository 端口。
- **OrderMetricsPort**（application 侧观测端口，拟新增，建议放在 `application.order.port` 包下）:
  - `recordCreateOrderSuccess(String source)`
  - `recordCreateOrderFailure(String source, String reasonType)`
  - `recordCreateOrderDuration(String source, String outcome, long nanos)`
- **PrometheusMeterRegistry** 不直接进入 application/domain；由 infrastructure 提供 Micrometer 适配实现，`start` 仅负责 Bean 装配与端点暴露。

## 关键算法/技术方案

### 1. 接入方式
- 在 `claude-j-start` 模块新增 `micrometer-registry-prometheus` 依赖。
- 复用现有 `spring-boot-starter-actuator`，通过配置暴露 `/actuator/prometheus`。
- 保持 `/actuator/**` 已在安全配置中放行，无需为 Prometheus 单独新增安全绕行逻辑。

### 2. 指标归属
- 技术指标端点归属 `start` 模块装配。
- 业务指标采集点归属 `application.order.service.OrderApplicationService`：
  - `createOrder(...)`
  - `createOrderFromCart(...)`
- 原因：下单成功/失败在应用服务层已经汇聚输入校验、聚合创建、仓储保存、事件发布与购物车清理流程，最适合作为单一观测切点；避免污染 domain 层。

### 3. 指标设计
- 指标 1：`claudej_order_create_total`（Counter）
  - 含义：成功创建订单次数
  - 标签：`source=direct|cart`
- 指标 2：`claudej_order_create_failure_total`（Counter）
  - 含义：创建订单失败次数
  - 标签：
    - `source=direct|cart`
    - `reason_type=validation|business|system`
- 指标 3：`claudej_order_create_duration`（Timer）
  - 含义：创建订单执行耗时
  - 标签：
    - `source=direct|cart`
    - `outcome=success|business_error|system_error`

### 4. 标签控制策略
- 禁止使用 `orderId`、`customerId`、`couponId`、异常 message 作为标签，避免高基数膨胀。
- `reason_type` 只保留有限分类：
  - `validation`: 命令为空、客户 ID 为空、订单项为空等输入前置校验失败
  - `business`: `BusinessException` 导致的业务拒绝，如购物车为空、优惠券不可用
  - `system`: 非 `BusinessException` 的未知异常
- `source` 固定为 `direct`（直接下单）与 `cart`（购物车下单）。

### 5. 失败率计算方案
- Prometheus 端不单独维护“失败率”Gauge，避免派生指标重复存储。
- 失败率通过查询表达式计算：
  - `sum(rate(claudej_order_create_failure_total[5m])) / (sum(rate(claudej_order_create_total[5m])) + sum(rate(claudej_order_create_failure_total[5m])))`
- 原因：Prometheus 推荐用查询层表达比率，避免应用侧保存冗余聚合状态。

### 6. 备选方案与取舍
- 方案 A：在 `OrderApplicationService` 内直接注入 `MeterRegistry`
  - 优点：实现最少
  - 缺点：application 直接依赖 Micrometer，技术细节泄漏到用例层
- 方案 B：定义 `OrderMetricsPort`，由 infrastructure/start 提供 Micrometer 实现
  - 优点：保持六边形边界，application 只依赖端口
  - 缺点：多一层适配代码
- 结论：采用方案 B。该任务虽属技术观测，但仍需遵守 `application -> domain <- infrastructure` 依赖方向。

### 7. 假设与待确认
- 假设本期“下单量、下单失败率”仅要求覆盖订单创建，不要求支付、取消、退款等其他订单生命周期指标。
- 假设 `/actuator/prometheus` 端点在 `dev` 环境与测试环境需要可访问，以便集成测试验证。
- 假设现有 Actuator 开放策略允许新增 `prometheus` 暴露项，不需要额外鉴权控制。
- 评审结论：`OrderMetricsPort` 放在 `application.order.port` 包下，延续本项目“应用层定义端口、基础设施实现端口”的既有模式。

## API 设计

| 方法 | 路径 | 描述 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| GET | /actuator/prometheus | Prometheus 文本格式指标导出 | — | `text/plain; version=0.0.4` 指标文本 |
| GET | /actuator/metrics | 查看已注册指标名（现有） | — | Actuator metrics JSON |

补充说明：
- 不新增业务 REST API。
- `/actuator/prometheus` 由 Spring Boot Actuator + Prometheus registry 自动提供。
- Build 阶段需要补充集成测试，至少验证端点可访问且包含 `claudej_order_create_total` 等指标名。

## 数据库设计（如有）
本需求无新增表、无 DDL 变更。

```sql
-- No schema changes required for 027-prometheus-metrics.
```

## 影响范围
- **domain**:
  - 无领域模型变更；仅复用 `Order` 聚合及既有值对象语义
- **application**:
  - 新增订单指标采集端口（`application.order.port.OrderMetricsPort`）
  - 修改 `OrderApplicationService`，在 `createOrder` 与 `createOrderFromCart` 增加成功/失败/耗时记录编排
  - 新增 Mockito 单测覆盖 success、business failure、validation failure 标签分类，并显式断言 system failure 分类
- **infrastructure**:
  - 新增 Micrometer 适配实现（如 `MicrometerOrderMetricsRecorder`）
  - 负责 Counter/Timer 注册与标签封装
- **adapter**:
  - 无新增业务控制器
  - 可能仅需补充测试，确认业务 API 仍正常且观测逻辑不影响 HTTP 契约
- **start**:
  - `claude-j-start/pom.xml` 新增 Prometheus registry 依赖
  - `application*.yml` 增加 `prometheus` 端点暴露配置
  - 新增/扩展 Actuator 集成测试验证 `/actuator/prometheus`

## 验收条件
- `dev` 环境启动后可访问 `/actuator/prometheus`，返回 200。
- 返回内容包含 Micrometer 默认 Prometheus 文本格式，并可见：
  - `claudej_order_create_total`
  - `claudej_order_create_failure_total`
  - `claudej_order_create_duration_seconds`
- 直接下单成功后，`source="direct"` 的成功计数递增。
- 从购物车下单失败时，`source="cart"` 与有限 `reason_type` 标签的失败计数递增。
- 发生非 `BusinessException` 异常时，失败计数使用 `reason_type="system"`，耗时指标使用 `outcome="system_error"`。
- 失败率通过 PromQL 计算，不在代码中维护冗余 rate/gauge。
- Build 阶段需提供自动化闭环：
  - `OrderApplicationServiceTest` 覆盖 `should_record_success_metric_when_create_order_succeeds`
  - `OrderApplicationServiceTest` 覆盖 `should_record_failure_metric_when_create_order_from_cart_business_error`
  - `OrderApplicationServiceTest` 覆盖 system failure 分类
  - `ActuatorPrometheusIntegrationTest` 覆盖 `/actuator/prometheus` 200 与指标名可见
- Build 阶段完成后，三项预飞 `mvn test`、`mvn checkstyle:check`、`./scripts/entropy-check.sh` 必须真实通过。

## 架构评审

**评审人**：@architect
**日期**：2026-04-29
**结论**：✅ 通过

### 评审检查项（15 维四类）

**架构合规（7 项）**
- [x] 聚合根边界合理（遵循事务一致性原则）
- [x] 值对象识别充分（金额、标识符等应为 VO）
- [x] Repository 端口粒度合适（方法不多不少）
- [x] 与已有聚合无循环依赖
- [x] DDL 设计与领域模型一致（字段映射、索引合理）
- [x] API 设计符合 RESTful 规范
- [x] 对象转换链正确（DO ↔ Domain ↔ DTO ↔ Request/Response）

**需求质量（3 项）**
- [x] 需求无歧义：核心名词、流程、异常分支均有明确定义
- [x] 验收条件可验证：每条 AC 可转化为 `should_xxx_when_yyy` 测试用例
- [x] 业务规则完备：状态机/不变量/边界值在需求中已列明

**计划可执行性（2 项）**
- [x] task-plan 粒度合格：按层任务已分解到原子级（10–15 分钟/步），每步含文件路径 + 验证命令 + 预期输出（详见 `docs/exec-plan/templates/task-plan.template.md` 原子任务章节）
- [x] 依赖顺序正确：domain → application → infrastructure → adapter → start 自下而上，层间依赖无倒置

**可测性保障（3 项 — 010 复盘后新增）**
- [x] **AC 自动化全覆盖**：`test-case-design.md` 的「AC 自动化覆盖矩阵」每条 AC 都有对应自动化测试方法；任一标「手动」但无替代自动化测试 → **打回**
- [x] **可测的注入方式**：若引入新 Spring Bean，使用构造函数注入而非字段注入（避免测试反射）；详见 `java-dev.md` 依赖注入规则
- [x] **配置校验方式合规**：若涉及敏感/跨环境配置校验，使用 `@ConfigurationProperties + @Validated`，不得用 `ApplicationRunner`/`@PostConstruct`；详见 ADR-005

**心智原则（Karpathy — 动手前自检）**
- [x] **简洁性**：需求未要求的抽象/配置/工厂已移除；任何单一实现的 `XxxStrategy`/`XxxFactory` 需说明存在理由
- [x] **外科性**：设计仅改动任务直接相关的文件；若涉及跨聚合大改，在评审意见说明理由
- [x] **假设显性**：需求里含糊的字段/边界/异常，requirement-design 已在「假设与待确认」列出

> 完整原则与反模式：`.claude/rules/karpathy-guidelines.md`

### 评审意见
- `OrderMetricsPort` 作为 application 端口、由 infrastructure 提供 Micrometer 适配实现，符合 `application -> domain <- infrastructure` 的既有边界；`start` 仅负责依赖装配与 Actuator 暴露，避免把观测实现塞进启动层。
- 指标命名 `claudej_order_create_*` 与标签 `source/reason_type/outcome` 保持低基数，未引入 `orderId`、`customerId`、异常消息等高基数标签，符合 Prometheus 建模约束。
- 失败率留在 PromQL 查询层计算，不新增 Gauge，符合“派生指标不在应用内冗余存储”的简洁原则。
- 设计已覆盖 validation/business/system 三类失败语义，但 Build 阶段必须把 `system` 分类写成自动化测试，避免只验证业务异常分支。
- 参考现有 `OrderApplicationService` 与 `ActuatorHealthIntegrationTest` 模式，建议 Build 阶段沿用构造函数注入、`@SpringBootTest` 端点集成测试和 `SimpleMeterRegistry`/Mockito 分层验证，不引入额外抽象层。
- 架构基线已真实执行：`./scripts/entropy-check.sh` 退出码 0，结果 `FAIL=0 / WARN=13 / status=PASS`；现有 WARN 为仓库基线噪音，不阻断本任务评审。

### 自行修正记录（若有）
- 已在设计文档中明确 `OrderMetricsPort` 放置于 `application.order.port` 包下。
- 已将 Micrometer 实现归属表述收敛为“infrastructure 实现、start 装配”。
- 已补充 system failure 自动化覆盖要求，闭合验收条件与测试计划。

### 需要新增的 ADR
- 无。本次采用的“application 定义观测端口、infrastructure 实现、start 装配”属于现有六边形边界的直接应用，不形成新的项目级架构决策。
