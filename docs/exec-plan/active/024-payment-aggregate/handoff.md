# Handoff — 024-payment-aggregate

## 基本信息
- **task-id**: 024-payment-aggregate
- **from**: architect
- **to**: dev
- **status**: approved
- **phase**: Review -> Build

## 产出物清单
1. `docs/exec-plan/active/024-payment-aggregate/requirement-design.md` — 需求设计文档
2. `docs/exec-plan/active/024-payment-aggregate/task-plan.md` — 任务执行计划
3. `docs/exec-plan/active/024-payment-aggregate/dev-log.md` — 开发日志（空）

## 设计摘要

### 领域模型
- **聚合根**: Payment（持有 orderId、amount、status、method、transactionNo）
- **值对象**: PaymentId、PaymentMethod（枚举）、PaymentStatus（状态机枚举）、PaymentResult
- **端口接口**: PaymentRepository、PaymentGateway

### 状态机
```
PENDING → SUCCESS / FAILED
SUCCESS → REFUNDED
```

### API 设计
| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/v1/payments | 创建支付 |
| GET | /api/v1/payments/{paymentId} | 查询支付 |
| GET | /api/v1/payments/orders/{orderId} | 查询订单支付 |
| POST | /api/v1/payments/callback | 支付回调 |
| POST | /api/v1/payments/{paymentId}/refund | 退款（ADMIN）|

### 与 Order 集成
- Payment 创建时关联 orderId
- 支付成功回调触发 Order.markAsPaid()
- 退款触发 Order.refund() + Inventory 回滚

### 数据库设计
- 表名: `t_payment`
- 核心字段: payment_id, order_id, customer_id, amount, status, method, transaction_no

## 验收条件
1. 支付创建成功，状态为 PENDING
2. 支付回调处理后状态正确流转（SUCCESS/FAILED）
3. 支付成功后订单状态自动变更为 PAID
4. 退款后订单状态变更为 REFUNDED
5. MockPaymentGateway 可模拟成功/失败场景
6. 三项预飞全过
7. JaCoCo 阈值不下滑（domain 90% / application 80%）

## 待确认项
1. 支付回调 webhook 是否需要区分 PSP 类型？
2. 退款是否需要审批流程？
3. 支付超时自动取消机制是否需要？

## architect 评审意见

<!-- @architect 填写 -->
- **评审结果**: ✅ approved
- **修改建议**: 无重大修改，含设计澄清（详见 requirement-design.md 架构评审章节）
- **评审时间**: 2026-04-25

### 评审摘要
- 架构合规检查：PASS（0 FAIL，12 WARN）
- 待确认项已决策：PSP 类型暂统一、退款直接处理、超时取消暂不实现
- Payment 与 Order 协作：PaymentApplicationService 调用 OrderRepository + Order.markAsPaid()
- 退款库存回滚：仅 PAID 状态订单退款时回滚库存

### 下一步
- 状态流转至 Build 阶段
- @dev 可开始 TDD 开发