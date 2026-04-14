# 开发日志 — 006-coupon-service

## 问题记录

<!-- 开发过程中遇到的问题、做出的决策及原因。按时间顺序记录。 -->

### 1. 优惠券折扣值建模
- **问题**：折扣值在固定金额和百分比两种模式下有不同的约束条件
- **决策**：创建 DiscountValue 值对象，内部持有 BigDecimal 值和 DiscountType，在构造时根据类型执行不同的校验逻辑
- **原因**：将校验逻辑封装在值对象中，符合 DDD 值对象自校验原则，避免在应用层重复校验

### 2. 过期策略选择
- **问题**：优惠券过期状态是定时批量更新还是查询时懒更新
- **决策**：采用懒过期策略，查询时检测并更新状态
- **原因**：避免引入定时任务的复杂性，对于电商场景查询频率足够高，懒过期即时性可接受

### 3. Money 值对象复用
- **问题**：优惠券的最低订单金额和折扣金额是否复用 order 聚合的 Money 值对象
- **决策**：复用 domain/common 或 order 的 Money（当前 Money 位于 order 包下）。考虑到 Money 是通用值对象，建议后续重构到 common 包。本期先直接在 coupon 聚合内部引用 order 的 Money，或在 coupon 内独立定义 minOrderAmount 为 BigDecimal + currency
- **原因**：为避免跨聚合引用，本期 coupon 聚合内的金额字段直接使用 BigDecimal + String(currency) 组合，不引入跨聚合依赖

## 变更记录

<!-- 与原设计（requirement-design.md）不一致的变更 -->
- 无与原设计不一致的变更。
