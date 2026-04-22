# Handoff — 016-order-state-machine

## 基本信息
- **task-id**: 016-order-state-machine
- **from**: dev
- **to**: architect
- **status**: pending-review
- **date**: 2026-04-22

## 产出物
- `docs/exec-plan/active/016-order-state-machine/requirement-design.md`
- `docs/exec-plan/active/016-order-state-machine/task-plan.md`
- `docs/exec-plan/active/016-order-state-machine/dev-log.md`

## 设计要点摘要
1. **Domain 层**：OrderStatus 新增 REFUNDED 枚举值 + canRefund()/toRefunded() 方法；Order 新增 refund()/isRefunded()
2. **Application 层**：新增 shipOrder()/deliverOrder()/refundOrder() 三个用例方法，refundOrder 内含优惠券回滚逻辑（coupon.unuse() + order.removeCoupon()）
3. **Adapter 层**：新增 POST /{orderId}/ship、/{orderId}/deliver、/{orderId}/refund 三个端点
4. **Infrastructure 层**：无变更（OrderConverter 通过 OrderStatus.valueOf() 自动支持 REFUNDED）
5. **DDL**：无变更（status 列为 VARCHAR(32)）

## 关键决策
- REFUNDED 为终态，不允许再做任何状态转换
- 退款不额外入参（无退款原因/金额），与 pay/cancel 端点风格一致
- 退款优惠券回滚逻辑与 cancelOrder 模式一致：先 unuse coupon，再 removeCoupon on order，最后 refund
- 退款仅允许从 PAID/SHIPPED/DELIVERED 状态发起

## 待确认项
- 无

## pre-flight
- N/A（Spec 阶段不涉及代码变更，无需运行测试命令）
