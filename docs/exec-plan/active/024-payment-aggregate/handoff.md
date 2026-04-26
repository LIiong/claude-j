# Handoff — 024-payment-aggregate

## 基本信息
- **task-id**: 024-payment-aggregate
- **from**: dev
- **to**: qa
- **status**: pending-review
- **phase**: Build -> Verify

## 产出物清单
1. `docs/exec-plan/active/024-payment-aggregate/requirement-design.md` — 需求设计文档
2. `docs/exec-plan/active/024-payment-aggregate/task-plan.md` — 任务执行计划（已完成）
3. `docs/exec-plan/active/024-payment-aggregate/dev-log.md` — 开发日志（已填写）

## 开发摘要

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
- 支付成功回调触发 Order.pay() + Inventory 扣减
- 退款触发 Order.refund() + Inventory 回滚

### 数据库设计
- 表名: `t_payment`
- 核心字段: payment_id, order_id, customer_id, amount, status, method, transaction_no

## 测试统计
- Domain: 97 tests (PaymentId:8, PaymentMethod:6, PaymentStatus:33, PaymentResult:13, Payment:37)
- Application: 25 tests
- Infrastructure: 15 tests (PaymentRepositoryImpl:8, MockPaymentGateway:7)
- Adapter: 8 tests
- Start: FlywayVerificationTest:2 tests (11 migrations, 14 tables)
- **Payment 聚合新增**: 145 tests

## pre-flight
```yaml
pre-flight:
  mvn-test: pass       # Tests run: 915, Failures: 0, Errors: 0, Skipped: 0
  checkstyle: pass     # BUILD SUCCESS
  entropy-check: pass  # issues: 0, warnings: 13
```

## 变更记录
- PaymentCallbackCommand 增加 orderId 字段（回调需要携带订单信息）
- 退款库存恢复使用 adjustStock 而不是 release（库存已扣减）
- FlywayVerificationTest 更新为 11 migrations 和 14 tables

## 下一步
- @qa 开始验收测试
- 验收条件见 requirement-design.md