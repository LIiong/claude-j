# Handoff — 016-order-state-machine

## 基本信息
- **task-id**: 016-order-state-machine
- **from**: dev
- **to**: qa
- **status**: pending-review
- **date**: 2026-04-22

## 产出物
- `claude-j-domain/src/main/java/com/claudej/domain/order/model/valobj/OrderStatus.java` — 新增 REFUNDED + canRefund/toRefunded
- `claude-j-domain/src/test/java/com/claudej/domain/order/model/valobj/OrderStatusTest.java` — 新增 8 个退款测试
- `claude-j-domain/src/main/java/com/claudej/domain/order/model/aggregate/Order.java` — 新增 refund/isRefunded
- `claude-j-domain/src/test/java/com/claudej/domain/order/model/aggregate/OrderTest.java` — 新增 5 个退款测试
- `claude-j-application/src/main/java/com/claudej/application/order/service/OrderApplicationService.java` — 新增 shipOrder/deliverOrder/refundOrder
- `claude-j-application/src/test/java/com/claudej/application/order/service/OrderApplicationServiceTest.java` — 新增 12 个测试
- `claude-j-adapter/src/main/java/com/claudej/adapter/order/web/OrderController.java` — 新增 ship/deliver/refund 端点
- `claude-j-adapter/src/test/java/com/claudej/adapter/order/web/OrderControllerTest.java` — 新增 9 个端点测试

## pre-flight
- mvn-test: pass — Tests run: 549, Failures: 0, Errors: 0, Skipped: 0
- checkstyle: pass — 0 Checkstyle violations
- entropy-check: pass — 0 FAIL, 12 WARN (status: PASS)

## summary
完成 Order 状态机发货/送达/退款完整链路开发。

### Domain 层
- OrderStatus 新增 REFUNDED 枚举值，实现 canRefund()（PAID/SHIPPED/DELIVERED 可退款）和 toRefunded() 转换方法
- Order 新增 refund() 和 isRefunded() 方法

### Application 层
- shipOrder(orderId): find → ship → save → toDTO
- deliverOrder(orderId): find → deliver → save → toDTO
- refundOrder(orderId): find → hasCoupon? unuse+removeCoupon → refund → save → toDTO

### Adapter 层
- POST /api/v1/orders/{orderId}/ship
- POST /api/v1/orders/{orderId}/deliver
- POST /api/v1/orders/{orderId}/refund

### 测试覆盖
- Domain: 13 个新增测试（OrderStatusTest 8 + OrderTest 5）
- Application: 12 个新增测试
- Adapter: 9 个新增测试

### TDD 执行
- 所有代码均按 Red-Green-Refactor 流程开发
- Domain/Application 测试预先编写，Red 阶段验证失败后实现
- Adapter 测试预先编写，端点缺失返回 404 后实现

## 待验收项
1. 状态机转换规则正确性验证
2. 退款优惠券回滚逻辑验证
3. REST API 契约验证（请求/响应结构）
4. 异常场景覆盖验证（非法状态转换、订单不存在、优惠券不存在）

## 备注
- refundOrder 采用简化写法（单一 if hasCoupon），符合架构评审建议
- 无 Infrastructure 层变更（OrderConverter 通过 OrderStatus.valueOf() 自动支持 REFUNDED）