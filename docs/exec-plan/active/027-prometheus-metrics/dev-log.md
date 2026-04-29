# 开发日志 — 027-prometheus-metrics

## 问题记录

<!--
每条问题必须四段齐全（Issue / Root Cause / Fix / Verification），不得只记"决策"。
理由：违反 VERIFICATION 铁律的举证精神——没有 Verification 行的条目等于未证实。
Build 阶段 handoff 前请 self-check 所有条目；若缺 Verification 行 → 不得提交 handoff。
-->

### 1. 指标采集层级归属待明确
- **Issue**：Spec 阶段识别到需求包含 Micrometer/Prometheus 技术能力，但业务指标又与订单下单用例强相关，若直接在 `OrderApplicationService` 注入 `MeterRegistry` 会触碰六边形边界。
- **Root Cause**：观测需求跨越 `start` 装配与 `application` 用例编排两个层次，若不先定义端口，Build 阶段容易把第三方技术对象直接耦合进 application。
- **Fix**：在 `requirement-design.md` 中明确采用 `OrderMetricsPort` 端口方案，由 application 发出观测意图、由 infrastructure/start 实现 Micrometer 适配。
- **Verification**：`git diff -- /Users/macro.li/aiProject/claude-j/docs/exec-plan/active/027-prometheus-metrics/requirement-design.md` → `requirement-design.md` 已包含方案 A/B 对比、端口设计与最终取舍

### 2. 失败率表达方式需避免冗余状态
- **Issue**：需求提到“下单失败率”，存在直接在应用侧维护失败率 Gauge 的诱惑。
- **Root Cause**：失败率属于时间窗口上的派生统计，不是订单聚合的原生状态；若在应用内维护比率，会引入额外状态同步与线程安全复杂度。
- **Fix**：在 `requirement-design.md` 中规定只落地 success/failure counter 与 duration timer，失败率统一由 PromQL 查询计算。
- **Verification**：`git diff -- /Users/macro.li/aiProject/claude-j/docs/exec-plan/active/027-prometheus-metrics/requirement-design.md` → 设计文档已写明 failure rate PromQL 与“不新增 Gauge”原则

## 变更记录

- 无与原设计不一致的变更。
