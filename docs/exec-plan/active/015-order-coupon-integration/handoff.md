---
task-id: "015-order-coupon-integration"
from: qa
to: qa
status: approved
timestamp: "2026-04-19T14:00:00"
pre-flight:
  mvn-test: pass       # Tests: 448 run, 0 failures, 0 errors (独立重跑验证)
  checkstyle: pass     # Exit 0, 0 violations (独立重跑验证)
  entropy-check: pass  # 0 FAIL, 13 WARN (独立重跑验证，WARN与本次任务无关)
artifacts:
  - requirement-design.md
  - task-plan.md
  - dev-log.md
  - test-case-design.md (QA 产出)
  - test-report.md (QA 产出)
summary: "Order ↔ Coupon 集成开发完成，全量测试通过。核心功能：下单用券验证、支付时核销、取消时回滚。"
---

# 交接文档

> 本次为 Build 阶段开发完成，进入 QA 验收。
> 状态流转：pending-review → approved / changes-requested

## 交接说明

### 开发完成概述

本次任务实现 Order 与 Coupon 聚合的完整集成，所有功能已通过 TDD 开发并验证：

1. **下单用券**：创建订单时验证优惠券（归属/有效期/最低消费），计算折扣金额
2. **支付核销**：支付订单时将优惠券状态从 AVAILABLE 转为 USED
3. **取消回滚**：取消订单时将优惠券状态从 USED 回滚为 AVAILABLE

### 实现范围

**Domain 层：**
- Order 聚合新增：couponId, discountAmount, finalAmount 字段
- Order 聚合新增：applyCoupon(), removeCoupon(), hasCoupon(), getCouponIdValue() 方法
- Order.reconstruct() 新增带优惠券参数的重载方法
- Coupon 聚合新增：unuse() 方法，支持状态回滚
- CouponStatus 枚举新增：canUnuse(), toAvailable() 方法
- Money 值对象新增：subtract() 方法
- ErrorCode 新增：COUPON_NOT_BELONG_TO_USER, COUPON_NOT_AVAILABLE, COUPON_MIN_ORDER_AMOUNT_NOT_MET

**Application 层：**
- CreateOrderCommand：新增可选 couponId 字段
- OrderDTO：新增 couponId, discountAmount, finalAmount 字段
- OrderAssembler：更新映射逻辑，支持新字段
- OrderApplicationService：
  - 注入 CouponRepository
  - createOrder() / createOrderFromCart()：添加优惠券验证和计算逻辑
  - payOrder()：添加优惠券核销逻辑
  - cancelOrder()：添加优惠券回滚逻辑

**Infrastructure 层：**
- OrderDO：新增 couponId, discountAmount, finalAmount 字段
- OrderConverter：更新 toDomain() 和 toDO() 方法，支持优惠券字段映射
- 数据库 DDL（V2__order_init.sql）：添加 coupon_id, discount_amount, final_amount 字段

### 测试覆盖

| 层级 | 测试类 | 用例数 | 说明 |
|------|--------|--------|------|
| Domain | MoneyTest | 18 | 新增 subtract() 方法测试 |
| Domain | CouponStatusTest | 12 | 新增 toAvailable() 方法测试 |
| Domain | CouponTest | 28 | 新增 unuse() 方法测试 |
| Domain | OrderTest | 28 | 新增 applyCoupon(), removeCoupon() 测试 |
| Application | OrderApplicationServiceTest | 17 | 新增优惠券集成测试 |
| Infrastructure | OrderRepositoryImplTest | 9 | DO↔Domain 往返验证 |

### 预飞检查项

**mvn test**
```
[INFO] Tests run: 448, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**mvn checkstyle:check**
```
[INFO] You have 0 Checkstyle violations.
[INFO] BUILD SUCCESS
```

**./scripts/entropy-check.sh**
```
{"issues": 0, "warnings": 12, "status": "PASS"}
架构合规检查通过。
```

### API 变更

| 端点 | 变更 | 说明 |
|------|------|------|
| POST /api/v1/orders | 请求体新增可选 couponId 字段 | 创建订单时可传入优惠券ID |
| POST /api/v1/orders/from-cart | 启用原有 couponId 字段 | 从购物车创建订单时可用券 |
| POST /api/v1/orders/{orderId}/pay | 内部新增优惠券核销 | 如有优惠券自动核销 |
| POST /api/v1/orders/{orderId}/cancel | 内部新增优惠券回滚 | 如有已核销优惠券自动回滚 |

### 异常场景处理

| 场景 | 行为 |
|------|------|
| 优惠券不存在 | 抛出 BusinessException(COUPON_NOT_FOUND) |
| 优惠券不属于当前用户 | 抛出 BusinessException(COUPON_NOT_BELONG_TO_USER) |
| 优惠券已过期 | 抛出 BusinessException(COUPON_NOT_AVAILABLE) |
| 订单金额不足 | 抛出 BusinessException(COUPON_MIN_ORDER_AMOUNT_NOT_MET) |
| 折扣金额大于订单金额 | 抛出 BusinessException(COUPON_DISCOUNT_VALUE_INVALID) |

## 交接历史

### 2026-04-19 — @qa 验收完成
- 状态：approved
- 说明：QA 验收通过，三项预飞检查独立验证通过，代码审查无阻塞性问题，测试用例设计完成，测试报告已生成
- 产出物：test-case-design.md, test-report.md

### 2026-04-19 — @dev → @qa
- 状态：pending-review
- 说明：015-order-coupon-integration 开发完成，全量测试通过，请求 QA 验收

### 2026-04-19 — @architect 评审完成
- 状态：approved
- 说明：015-order-coupon-integration 架构评审通过

### 2026-04-19 — @dev → @architect
- 状态：pending-review
- 说明：015-order-coupon-integration 设计文档完成，请求架构评审

<!-- 后续阶段继续追加 -->
