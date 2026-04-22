# 需求拆分设计 — 016-order-state-machine

## 需求描述
补齐 Order 状态机的发货（ship）、送达（deliver）、退款（refund）完整链路。
当前 OrderStatus 枚举有 CREATED/PAID/SHIPPED/DELIVERED/CANCELLED，缺少 REFUNDED；Order 聚合根已有 ship()/deliver() 方法但缺 refund()；Application 和 Adapter 层缺少对应用例方法和 REST 端点。退款时需回滚已使用的优惠券。

## 领域分析

### 聚合根: Order（变更）
- status (OrderStatus) — 新增 REFUNDED 枚举值，新增 canRefund()/toRefunded() 转换规则
- refund() 方法 — 退款：调用 status.toRefunded()，更新 updateTime
- isRefunded() 便捷方法 — 判断是否已退款

### 值对象
- **OrderStatus**: 新增 REFUNDED("已退款") 枚举值，新增 canRefund() / toRefunded() 方法
  - canRefund() 规则：PAID / SHIPPED / DELIVERED 可退款
  - toRefunded() 规则：仅 canRefund() 为 true 时允许转换，否则抛 BusinessException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION)

### 领域服务
- 无需新增领域服务。退款时的优惠券回滚由 Application 层编排（与 cancelOrder 模式一致）。

### 端口接口
- **OrderRepository**: 无变更（现有 save/findByOrderId 已满足）
- **CouponRepository**: 无变更（现有 findByCouponId/save 已满足）

## 关键算法/技术方案

### 状态机合法转换（完整视图）
```
CREATED ──pay──→ PAID
CREATED ──cancel──→ CANCELLED
PAID ──ship──→ SHIPPED
PAID ──cancel──→ CANCELLED
PAID ──refund──→ REFUNDED       [新增]
SHIPPED ──deliver──→ DELIVERED
SHIPPED ──refund──→ REFUNDED    [新增]
DELIVERED ──refund──→ REFUNDED  [新增]
```

### 退款优惠券回滚
退款流程与 cancelOrder 的优惠券回滚逻辑模式一致：
1. 查找订单
2. 如果订单使用了优惠券且优惠券已核销（USED 状态），调用 coupon.unuse() 回滚
3. 订单调用 removeCoupon() 移除关联
4. 订单调用 refund() 变更状态
5. 保存订单和优惠券

### 假设与待确认
- **假设 1**：REFUNDED 状态的订单不允许再做任何状态转换（终态），与 CANCELLED 类似
- **假设 2**：退款不需要额外入参（如退款原因、退款金额），仅做状态变更 + 优惠券回滚。如需退款金额部分退，属于未来需求
- **假设 3**：refund 接口为 POST /{orderId}/refund，无请求体，与现有 pay/cancel 端点风格一致

## API 设计

| 方法 | 路径 | 描述 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| POST | /api/v1/orders/{orderId}/ship | 发货 | — | `{ "success": true, "data": { "orderId": "...", "status": "SHIPPED", ... } }` |
| POST | /api/v1/orders/{orderId}/deliver | 送达 | — | `{ "success": true, "data": { "orderId": "...", "status": "DELIVERED", ... } }` |
| POST | /api/v1/orders/{orderId}/refund | 退款 | — | `{ "success": true, "data": { "orderId": "...", "status": "REFUNDED", ... } }` |

错误场景：
- 订单不存在 → 404 ORDER_NOT_FOUND
- 状态不允许转换 → 400 INVALID_ORDER_STATUS_TRANSITION
- 优惠券回滚失败（优惠券不存在）→ 404 COUPON_NOT_FOUND

## 数据库设计
无 DDL 变更。t_order.status 列为 VARCHAR(32)，已支持存储 "REFUNDED" 字符串。

## 影响范围
- **domain**:
  - `OrderStatus.java` — 新增 REFUNDED 枚举值 + canRefund() + toRefunded()
  - `Order.java` — 新增 refund() + isRefunded() 方法
  - `OrderStatusTest.java` — 新增退款转换测试
  - `OrderTest.java` — 新增退款场景测试
- **application**:
  - `OrderApplicationService.java` — 新增 shipOrder() / deliverOrder() / refundOrder() 方法
  - `OrderApplicationServiceTest.java` — 新增对应测试
- **infrastructure**: 无变更（OrderConverter 已通过 OrderStatus.valueOf() 支持新枚举值）
- **adapter**:
  - `OrderController.java` — 新增 ship / deliver / refund 端点
  - `OrderControllerTest.java` — 新增对应测试
- **start**: 无 DDL 变更
