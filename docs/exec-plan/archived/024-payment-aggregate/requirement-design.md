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

## 架构评审

**评审人**：@architect
**日期**：2026-04-25
**结论**：✅ 通过（含设计澄清）

### 评审检查项（15 维四类）

**架构合规（7 项）**
- [x] 聚合根边界合理（遵循事务一致性原则）
  - Payment 聚合根持有 orderId 引用而非 Order 实体，符合跨聚合引用规范
  - PaymentGateway 作为 Domain 层端口接口（类似 ShortCodeGenerator），实现由 Infrastructure 提供
- [x] 值对象识别充分（金额、标识符等应为 VO）
  - PaymentId、PaymentMethod、PaymentStatus、PaymentResult 值对象设计合理
  - 复用 Money、OrderId、CustomerId 值对象（符合 DDD 实践）
- [x] Repository 端口粒度合适（方法不多不少）
  - PaymentRepository 5 个方法（save/findByPaymentId/findByOrderId/existsByPaymentId/findByCustomerId），与 OrderRepository 模式一致
- [x] 与已有聚合无循环依赖
  - Payment → Order（单向引用，通过 orderId 值对象）
  - PaymentApplicationService → OrderRepository（跨聚合协作）
- [x] DDL 设计与领域模型一致（字段映射、索引合理）
  - t_payment 表字段映射 Payment 聚合属性
  - idx_order_id、idx_customer_id、idx_transaction_no 索引覆盖主要查询
- [x] API 设计符合 RESTful 规范
  - POST /payments（创建）、GET /payments/{paymentId}（查询）、POST /payments/callback（回调）、POST /payments/{paymentId}/refund（退款）
  - 退款端点需增加 @PreAuthorize("hasRole('ADMIN')") 注释（Build 阶段实现）
- [x] 对象转换链正确（DO ↔ Domain ↔ DTO ↔ Request/Response）
  - PaymentConverter（DO ↔ Domain）、PaymentAssembler（Domain ↔ DTO）、Request/Response 与 DTO 映射

**需求质量（3 项）**
- [x] 需求无歧义：核心名词、流程、异常分支均有明确定义
  - Payment 状态机（PENDING → SUCCESS/FAILED，SUCCESS → REFUNDED）清晰
  - 幂等性设计明确（根据 transactionNo 查询现有 Payment）
- [x] 验收条件可验证：每条 AC 可转化为 `should_xxx_when_yyy` 测试用例
  - AC 1-7 均可转化为具体测试方法
- [x] 业务规则完备：状态机/不变量/边界值在需求中已列明
  - 状态转换规则在 PaymentStatus 设计中已封装

**计划可执行性（2 项）**
- [x] task-plan 粒度合格：按层任务已分解到原子级（10–15 分钟/步），每步含文件路径 + 验证命令 + 预期输出
  - 17 个原子任务，从 Domain 值对象到 Start DDL
  - 每个任务含验证命令（如 `mvn test -pl claude-j-domain -Dtest=PaymentIdTest`）和预期输出
- [x] 依赖顺序正确：domain → application → infrastructure → adapter → start 自下而上，层间依赖无倒置

**可测性保障（3 项）**
- [x] **AC 自动化全覆盖**：每条验收条件均有对应自动化测试方法（Domain/Application 单测 + Infrastructure H2 集成 + Adapter MockMvc）
- [x] **可测的注入方式**：PaymentApplicationService 使用构造函数注入（task-plan 已明确），符合 java-dev.md 规则
- [x] **配置校验方式合规**：MockPaymentGateway 配置（如模拟成功/失败）无需敏感校验，不涉及 ADR-005 场景

**心智原则（Karpathy — 动手前自检）**
- [x] **简洁性**：PaymentGateway 端口接口最小化（3 方法），无过度抽象；统一回调接口 `/callback`（后续扩展 `/callback/{pspType}` 不破坏现有代码）
- [x] **外科性**：仅新增 Payment 聚合相关文件，不修改 Order 聚合代码（通过 Repository 协作）
- [x] **假设显性**：3 项待确认已在「假设与待确认」列出，评审意见已明确处理方式

### 评审意见

#### 1. 待确认项处理决策

| 待确认项 | 评审决策 | 理由 |
|---------|---------|------|
| PSP 类型区分 | **保持统一接口** | Karpathy 原则② 简洁优先；MVP 阶段单一 PSP；后续扩展 `/callback/{pspType}` 不破坏现有 |
| 退款审批流程 | **MVP 直接退款** | 当前需求为管理员直接退款；审批流程属于后续需求，不预先设计 |
| 支付超时取消 | **暂不实现** | 项目无定时任务框架；属于独立需求（需引入 Scheduler）；不投机 |

#### 2. Payment 与 Order 聚合协作设计澄清

**设计原则**：Payment 成功回调后，PaymentApplicationService 应：
1. 更新 Payment 状态为 SUCCESS
2. 查询 Order（通过 orderId）
3. 调用 Order.markAsPaid()
4. 保存 Order

**职责边界**：
- `PaymentApplicationService.handleCallback()`：处理支付回调，更新 Payment 状态，触发 Order 状态变更
- `OrderApplicationService.payOrder()`：保留现有职责（库存扣减、优惠券核销），供其他场景调用（如线下支付）

**实现建议**：
- PaymentApplicationService 调用 OrderRepository + Order.markAsPaid()，不调用 OrderApplicationService（避免循环依赖）
- 若需库存扣减，在 PaymentApplicationService.handleCallback() 中调用 InventoryRepository（参考 OrderApplicationService.payOrder() 模式）

#### 3. 退款时库存回滚设计澄清

**当前 OrderApplicationService.refundOrder() 未回滚库存**：
- 创建订单：reserve（预占）
- 支付成功：deduct（扣减，预占转为实际扣减）
- 取消订单：release（释放预占）
- 退款：目前仅调用 order.refund()

**Payment 聚合退款场景**：
- 若订单已发货（SHIPPED/DELIVERED），退款不回滚库存（商品已出库）
- 若订单仅支付未发货（PAID），退款应回滚库存

**建议**：PaymentApplicationService.refundPayment() 根据 Order 状态决定是否回滚库存：
```java
if (order.getStatus() == OrderStatus.PAID) {
    releaseStockForOrder(order); // 回滚已扣减库存
}
```

此逻辑属于 PaymentApplicationService，不修改 OrderApplicationService（外科式变更）。

#### 4. 架构基线检查结果

```bash
$ ./scripts/entropy-check.sh
Exit code: 0
Issues: 0 FAIL, 12 WARN
```
- FAIL: 0（合规）
- WARN: 12（测试缺失、ADR 状态节缺失、归档目录修改）— 与本任务无关

#### 5. 其他建议

- ErrorCode 新增 Payment 相关错误码命名规范：PAYMENT_NOT_FOUND、PAYMENT_STATUS_INVALID 等（与已有风格一致）
- task-plan 原子任务骨架设计合理，符合项目模式

### 需要新增的 ADR

无。设计遵循已有模式（PaymentStatus 状态机参考 OrderStatus，PaymentGateway 端口参考 ShortCodeGenerator）。