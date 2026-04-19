---
task-id: "015-order-coupon-integration"
from: dev
to: architect
status: pending-review
timestamp: "2026-04-19T12:00:00"
pre-flight:
  mvn-test: pending
  checkstyle: pending
  entropy-check: pending
  tdd-evidence: []
artifacts:
  - requirement-design.md
  - task-plan.md
summary: "Order ↔ Coupon 集成设计完成，待架构评审。核心方案：Application 层协调跨聚合操作，Order 聚合封装金额不变量，Coupon 聚合提供 use/unuse 状态转换。"
---

# 交接文档

> 本次为 Spec 阶段设计评审，尚未开始编码。
> 状态流转：pending-review → approved / changes-requested

## 交接说明

### 设计概述

本次任务实现 Order 与 Coupon 聚合的完整集成，覆盖订单生命周期中的优惠券使用场景：

1. **下单用券**：创建订单时验证优惠券（归属/有效期/最低消费），计算折扣金额
2. **支付核销**：支付订单时将优惠券状态从 AVAILABLE 转为 USED
3. **取消回滚**：取消订单时将优惠券状态从 USED 回滚为 AVAILABLE

### 关键设计决策

| 决策点 | 方案 | 理由 |
|--------|------|------|
| 优惠券验证位置 | Application 层 | 验证需访问 CouponRepository 查询跨聚合数据，不应放在 Domain 层 |
| 金额计算 | Coupon.calculateDiscount() | 由 Coupon 聚合封装折扣规则，Order 仅保存结果 |
| 跨聚合一致性 | 本地事务（@Transactional） | 一个事务内修改 Order 和 Coupon，保证强一致性 |
| 优惠券回滚 | Coupon.unuse() 方法 | 封装状态回滚逻辑，清除 usedTime/usedOrderId |
| finalAmount 存储 | DO 中冗余存储 | 便于查询和报表，避免运行时重复计算 |

### 主要变更点

**Domain 层：**
- Order 聚合新增：couponId, discountAmount, finalAmount 字段
- Order 聚合新增：applyCoupon(), removeCoupon() 方法
- Coupon 聚合新增：unuse() 方法

**Application 层：**
- CreateOrderCommand / CreateOrderFromCartCommand：启用 couponId 字段
- OrderDTO：新增 couponId, discountAmount, finalAmount
- OrderApplicationService：注入 CouponRepository，实现验证/计算/核销/回滚逻辑

**Infrastructure 层：**
- OrderDO：新增 coupon_id, discount_amount, final_amount
- OrderConverter：更新往返映射
- schema.sql：更新 DDL

### 风险与注意事项

1. **金额一致性**：必须保证 finalAmount = totalAmount - discountAmount，在 Order.applyCoupon() 中强制校验
2. **状态回滚安全性**：Coupon.unuse() 必须校验当前状态为 USED，防止误回滚
3. **懒过期策略**：优惠券核销前必须调用 checkAndExpire() 处理过期

## 评审回复

<!-- @architect 填写 -->

### 评审结论

- [ ] approved — 设计通过，可以进入 Build 阶段
- [ ] changes-requested — 需要修改，详见下方问题清单

### 问题清单

<!-- 如有修改意见，逐条列出 -->

### 评审人

- 评审者：@architect
- 评审时间：

---

## 交接历史

### 2026-04-19 — @dev → @architect
- 状态：pending-review
- 说明：015-order-coupon-integration 设计文档完成，请求架构评审

<!-- 后续阶段继续追加 -->
