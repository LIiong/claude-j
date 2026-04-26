# 需求拆分设计 — 024-payment-aggregate

## 需求描述

新建 Payment 聚合，实现支付状态管理与 PSP（Payment Service Provider）抽象接口。Payment 聚合根持有 orderId、支付金额、支付状态、支付方式、第三方交易号。支付状态机支持 PENDING → SUCCESS / FAILED / REFUNDED 转换。支付成功后触发关联订单的 markAsPaid()，退款后触发订单状态变更。

## 领域分析

### 聚合根: Payment

- id (Long) — 数据库自增主键
- paymentId (PaymentId) — 业务支付标识（UUID）
- orderId (OrderId) — 关联订单号（引用 Order 聚合）
- customerId (CustomerId) — 客户标识
- amount (Money) — 支付金额
- status (PaymentStatus) — 支付状态（值对象/枚举）
- method (PaymentMethod) — 支付方式（ALIPAY/WECHAT/BANK_CARD）
- transactionNo (String) — 第三方支付平台交易号
- createTime (LocalDateTime) — 创建时间
- updateTime (LocalDateTime) — 更新时间

### 值对象

- **PaymentId**: 支付标识，UUID 格式，不可变
- **PaymentMethod**: 支付方式枚举（ALIPAY/WECHAT/BANK_CARD）
- **PaymentStatus**: 支付状态枚举（PENDING/SUCCESS/FAILED/REFUNDED），封装状态转换规则
- **Money**: 金额值对象（复用 order 聚合已有实现）
- **OrderId**: 订单号值对象（复用 order 聚合已有实现）
- **CustomerId**: 客户标识值对象（复用 order 聚合已有实现）

### 领域服务

无需领域服务。支付网关调用属于基础设施层实现。

### 端口接口

- **PaymentRepository**:
  - `save(Payment payment): Payment`
  - `findByPaymentId(PaymentId paymentId): Optional<Payment>`
  - `findByOrderId(OrderId orderId): Optional<Payment>`
  - `existsByPaymentId(PaymentId paymentId): boolean`
  - `findByCustomerId(CustomerId customerId): List<Payment>`

- **PaymentGateway**（PSP 抽象接口）:
  - `createPayment(Payment payment): PaymentResult` — 调用第三方创建支付订单
  - `queryPayment(String transactionNo): PaymentResult` — 查询支付状态
  - `refundPayment(String transactionNo, Money amount): PaymentResult` — 退款

## 关键算法/技术方案

### 1. 支付状态机

```
PENDING ──┬──> SUCCESS  (支付成功回调)
          ├──> FAILED   (支付失败回调)
          └──> (创建初始状态)

SUCCESS ──────> REFUNDED (管理员退款)
```

状态转换规则：
- PENDING → SUCCESS: 仅允许支付成功回调触发
- PENDING → FAILED: 仅允许支付失败回调触发
- SUCCESS → REFUNDED: 仅允许管理员退款触发
- FAILED/REFUNDED 为终态，不可再转换

### 2. PSP 抽象接口设计

PaymentGateway 作为 Domain 层端口接口，由 Infrastructure 层实现：
- **MockPaymentGateway**: 测试环境模拟实现，可配置返回成功/失败
- 未来扩展：**AlipayGateway** / **WechatPayGateway** 等真实 PSP 适配器

PaymentGateway 返回 PaymentResult 值对象：
- `success: boolean`
- `transactionNo: String`
- `message: String`

### 3. 与 Order 聚合集成

支付成功后触发 Order 状态变更：
- Application 层 PaymentApplicationService 调用 OrderRepository 查询订单
- 调用 Order.markAsPaid() 方法
- 保存 Order

退款后触发 Order 状态变更：
- 先调用 InventoryRepository 回滚库存预占（若订单未发货）
- 再调用 Order.refund() 方法
- 保存 Order

### 4. 幂等性设计

支付回调处理需支持幂等：
- 根据 transactionNo 查询现有 Payment
- 若已处理（状态非 PENDING），直接返回当前状态，不重复处理

## API 设计

| 方法 | 路径 | 描述 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| POST | /api/v1/payments | 创建支付 | `{ "orderId": "xxx", "customerId": "xxx", "amount": 100.00, "method": "ALIPAY" }` | `{ "success": true, "data": { "paymentId": "xxx", "status": "PENDING" } }` |
| GET | /api/v1/payments/{paymentId} | 查询支付详情 | — | `{ "success": true, "data": { "paymentId": "xxx", "orderId": "xxx", "status": "SUCCESS", ... } }` |
| GET | /api/v1/payments/orders/{orderId} | 查询订单支付状态 | — | `{ "success": true, "data": { "paymentId": "xxx", "status": "SUCCESS" } }` |
| POST | /api/v1/payments/callback | 支付回调 webhook | `{ "transactionNo": "xxx", "success": true, "message": "xxx" }` | `{ "success": true, "data": { "paymentId": "xxx", "status": "SUCCESS" } }` |
| POST | /api/v1/payments/{paymentId}/refund | 退款（管理员） | `{ "reason": "xxx" }` | `{ "success": true, "data": { "paymentId": "xxx", "status": "REFUNDED" } }` |

## 数据库设计

```sql
CREATE TABLE IF NOT EXISTS t_payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id VARCHAR(64) NOT NULL UNIQUE COMMENT '业务支付ID',
    order_id VARCHAR(64) NOT NULL COMMENT '关联订单ID',
    customer_id VARCHAR(64) NOT NULL COMMENT '客户ID',
    amount DECIMAL(19,2) NOT NULL COMMENT '支付金额',
    currency VARCHAR(16) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    status VARCHAR(32) NOT NULL COMMENT '支付状态',
    method VARCHAR(32) NOT NULL COMMENT '支付方式',
    transaction_no VARCHAR(128) COMMENT '第三方交易号',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',

    INDEX idx_order_id (order_id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_transaction_no (transaction_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付表';
```

## 影响范围

- **domain**:
  - 新增 `Payment` 聚合根
  - 新增 `PaymentId`、`PaymentMethod`、`PaymentStatus` 值对象
  - 新增 `PaymentRepository` 端口接口
  - 新增 `PaymentGateway` PSP 端口接口
  - 新增 `PaymentResult` 值对象
  - 新增 Payment 相关 ErrorCode

- **application**:
  - 新增 `PaymentApplicationService`
  - 新增 `CreatePaymentCommand`、`RefundPaymentCommand`
  - 新增 `PaymentDTO`
  - 新增 `PaymentAssembler`
  - 修改 `OrderApplicationService`（可选：增加支付成功后的回调处理逻辑，或保持现有 payOrder 方法）

- **infrastructure**:
  - 新增 `PaymentDO`
  - 新增 `PaymentMapper`
  - 新增 `PaymentConverter`
  - 新增 `PaymentRepositoryImpl`
  - 新增 `MockPaymentGateway` 实现
  - 新增 V11__add_payment.sql DDL

- **adapter**:
  - 新增 `PaymentController`
  - 新增 `CreatePaymentRequest`、`PaymentCallbackRequest`、`RefundPaymentRequest`
  - 新增 `PaymentResponse`

- **start**:
  - schema.sql 新增 t_payment 表定义

## 假设与待确认

1. **假设**：支付回调 webhook 简化为统一接口 `/callback`，不区分 PSP 类型。若未来对接多个 PSP，可扩展为 `/callback/alipay`、`/callback/wechat` 等。
2. **假设**：MockPaymentGateway 在测试环境下自动注入，生产环境需替换为真实 PSP 实现。
3. **待确认**：退款是否需要审批流程？当前设计为管理员直接退款。
4. **待确认**：支付超时自动取消机制是否需要？当前设计未包含超时取消逻辑。

## 验收条件

1. 支付创建成功，状态为 PENDING，返回 paymentId
2. 支付回调处理后状态正确流转（SUCCESS/FAILED）
3. 支付成功后订单状态自动变更为 PAID
4. 退款后订单状态变更为 REFUNDED
5. MockPaymentGateway 可模拟成功/失败场景
6. 三项预飞（mvn test / checkstyle / entropy-check）全过
7. JaCoCo 阈值不下滑（domain 90% / application 80%）