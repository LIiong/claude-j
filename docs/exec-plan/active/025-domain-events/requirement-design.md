# 需求拆分设计 — 025-domain-events

## 需求描述

实现领域事件机制，将现有 Application 层对 Inventory 的直接同步调用改为事件驱动协作模式。使用 Spring Events 作为事件基础设施，在 Domain 层定义事件基类和端口接口，在 Infrastructure 层实现事件发布与监听，解耦 Order、Payment、Inventory 跨聚合协作。

## 领域分析

### 领域事件基类（Domain 层）

**DomainEvent**（抽象基类，所有领域事件继承）
- eventId (String) — 事件唯一标识，UUID 格式
- occurredOn (LocalDateTime) — 事件发生时间
- aggregateType (String) — 发出事件的聚合类型（如 "Order"、"Payment"）
- aggregateId (String) — 发出事件的聚合根 ID

约束：
- 事件不可变（所有字段 final）
- eventId 不能为空，必须唯一
- occurredOn 不能为空
- aggregateType 不能为空
- aggregateId 不能为空

### 具体领域事件

**OrderCreatedEvent**
- 继承 DomainEvent
- orderId (String) — 订单 ID
- customerId (String) — 客户 ID
- items (List<OrderItemInfo>) — 订单项信息（productId、quantity）
- 触发时机：订单创建成功后

**OrderCancelledEvent**
- 继承 DomainEvent
- orderId (String) — 订单 ID
- customerId (String) — 客户 ID
- items (List<OrderItemInfo>) — 订单项信息
- 触发时机：订单取消后

**PaymentSuccessEvent**
- 继承 DomainEvent
- paymentId (String) — 支付 ID
- orderId (String) — 订单 ID
- customerId (String) — 客户 ID
- transactionNo (String) — 交易号
- 触发时机：支付成功回调处理后

**PaymentRefundedEvent**
- 继承 DomainEvent
- paymentId (String) — 支付 ID
- orderId (String) — 订单 ID
- customerId (String) — 客户 ID
- transactionNo (String) — 原交易号
- 触发时机：支付退款后

### 辅助值对象

**OrderItemInfo**（用于事件携带订单项信息）
- productId (String) — 商品 ID
- productName (String) — 商品名称
- quantity (int) — 数量
- 不可变，用于事件数据传递

### 端口接口

**DomainEventPublisher**（Domain 层接口，Infrastructure 实现）
- `void publish(DomainEvent event)` — 发布领域事件

## 关键算法/技术方案

### 事件驱动协作模式

将现有的同步调用改为事件驱动：

| 业务场景 | 现有实现 | 改造后 |
|---------|---------|--------|
| 创建订单 → 预占库存 | OrderApplicationService 直接调用 InventoryRepository | 发布 OrderCreatedEvent，InventoryEventListener 监听并预占 |
| 支付成功 → 扣减库存 | PaymentApplicationService 直接调用 InventoryRepository | 发布 PaymentSuccessEvent，InventoryEventListener 监听并扣减 |
| 取消订单 → 释放库存 | OrderApplicationService 直接调用 InventoryRepository | 发布 OrderCancelledEvent，InventoryEventListener 监听并释放 |
| 退款 → 恢复库存 | PaymentApplicationService 直接调用 InventoryRepository | 发布 PaymentRefundedEvent，InventoryEventListener 监听并恢复 |

### 技术选型

**事件基础设施**：Spring ApplicationEventPublisher + @EventListener / @TransactionalEventListener

**选型理由**：
- Spring 内置支持，无需引入额外消息中间件
- @TransactionalEventListener 保证事务内事件发布，事务成功后才触发监听器
- 与现有 Spring Boot 2.7.18 架构无缝集成
- Java 8 兼容

### 事务边界策略

使用 **@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)**：
- 事件监听器在发布方事务提交后执行
- 监听器操作独立事务（需标注 @Transactional）
- 保证主事务失败时事件不会触发副作用

**权衡说明**：
- 方案 A：同步事件（@EventListener，同一事务） — 优点：简单；缺点：事件处理失败会回滚主事务
- 方案 B：事务后事件（@TransactionalEventListener AFTER_COMMIT） — 优点：解耦、隔离；缺点：最终一致性
- **选择方案 B**：符合跨聚合协作的最终一致性原则（DDD 推荐）

### 事件监听器职责

**InventoryEventListener**
- `onOrderCreated(OrderCreatedEvent)` → 调用 InventoryApplicationService.reserveStock()
- `onPaymentSuccess(PaymentSuccessEvent)` → 调用 InventoryApplicationService.deductStock()
- `onOrderCancelled(OrderCancelledEvent)` → 调用 InventoryApplicationService.releaseStock()
- `onPaymentRefunded(PaymentRefundedEvent)` → 调用 InventoryApplicationService.adjustStock()

## API 设计

本次任务无新增 REST API，仅改造内部协作机制。

现有 API 保持不变：
- POST /api/v1/orders — 创建订单（内部改用事件预占库存）
- POST /api/v1/payments/callback — 支付回调（内部改用事件扣减库存）
- POST /api/v1/orders/{orderId}/cancel — 取消订单（内部改用事件释放库存）
- POST /api/v1/payments/{paymentId}/refund — 退款（内部改用事件恢复库存）

## 数据库设计（无新增）

本次任务不新增数据库表，仅改造应用层逻辑。

现有相关表：
- t_order — 订单表
- t_order_item — 订单项表
- t_inventory — 库存表
- t_payment — 支付表

## 影响范围

### 新增类（按层列出）

**domain 层**：
- `com.claudej.domain.common.event.DomainEvent` — 事件基类（抽象类）
- `com.claudej.domain.common.event.DomainEventPublisher` — 事件发布端口接口
- `com.claudej.domain.order.event.OrderCreatedEvent` — 订单创建事件
- `com.claudej.domain.order.event.OrderCancelledEvent` — 订单取消事件
- `com.claudej.domain.order.event.OrderItemInfo` — 订单项信息值对象
- `com.claudej.domain.payment.event.PaymentSuccessEvent` — 支付成功事件
- `com.claudej.domain.payment.event.PaymentRefundedEvent` — 支付退款事件

**infrastructure 层**：
- `com.claudej.infrastructure.common.event.SpringDomainEventPublisher` — 事件发布实现
- `com.claudej.infrastructure.inventory.event.InventoryEventListener` — 库存事件监听器

### 修改类

**application 层**：
- `com.claudej.application.order.service.OrderApplicationService` — 移除直接库存调用，改为发布事件
- `com.claudej.application.payment.service.PaymentApplicationService` — 移除直接库存调用，改为发布事件

### 测试类（新增）

**domain 层**：
- `DomainEventTest` — 事件基类测试
- `OrderCreatedEventTest` — 订单创建事件测试
- `OrderCancelledEventTest` — 订单取消事件测试
- `PaymentSuccessEventTest` — 支付成功事件测试
- `PaymentRefundedEventTest` — 支付退款事件测试
- `OrderItemInfoTest` — 订单项信息值对象测试

**infrastructure 层**：
- `SpringDomainEventPublisherTest` — 事件发布实现测试
- `InventoryEventListenerTest` — 库存事件监听器测试

**application 层**：
- `OrderApplicationServiceTest` — 更新测试，Mock DomainEventPublisher
- `PaymentApplicationServiceTest` — 更新测试，Mock DomainEventPublisher

## 假设与待确认

1. **假设**：事件监听器失败时记录日志但不阻塞主流程（符合最终一致性原则）
2. **假设**：不引入消息队列（MQ），仅使用 Spring 内置事件机制（演示项目规模）
3. **假设**：Order 聚合根内部不持有事件列表，由 Application 层决定何时发布（简化实现）

## 验收条件

1. Order 创建时发布 OrderCreatedEvent，Inventory 监听器正确预占库存
2. Payment 成功时发布 PaymentSuccessEvent，Inventory 监听器正确扣减库存
3. Order 取消时发布 OrderCancelledEvent，库存正确释放
4. Payment 退款时发布 PaymentRefundedEvent，库存正确恢复
5. 三项预飞（mvn test / checkstyle / entropy-check）全过
6. JaCoCo 阈值不下滑（domain 90% / application 80%）

---

## 设计澄清与修正（架构评审前）

> **评审人**：@architect
> **日期**：2026-04-26
> **修正原因**：设计文档存在业务流程歧义，需澄清后方可编码。

### 1. 库存扣减时机澄清

**问题**：当前代码中库存扣减发生在两个场景：
- `OrderApplicationService.payOrder()` — 用户主动支付时
- `PaymentApplicationService.handleCallback()` — 支付网关回调时

这导致潜在重复扣减风险。

**澄清**：根据业务语义，库存扣减应在「支付成功确认」时发生，即支付回调处理时。

**修正方案**：
- `OrderApplicationService.payOrder()` **不再**发布扣减库存事件（该方法用于内部状态流转，不涉及库存）
- 仅 `PaymentApplicationService.handleCallback()` 发布 `PaymentSuccessEvent`，触发库存扣减

**映射表更新**：

| 业务场景 | 现有实现 | 改造后 | 事件类型 |
|---------|---------|--------|----------|
| 创建订单 → 预占库存 | OrderApplicationService.createOrder() 直接调用 | 发布 OrderCreatedEvent | OrderCreatedEvent |
| 支付回调成功 → 扣减库存 | PaymentApplicationService.handleCallback() 直接调用 | 发布 PaymentSuccessEvent | PaymentSuccessEvent |
| 取消订单 → 释放库存 | OrderApplicationService.cancelOrder() 直接调用 | 发布 OrderCancelledEvent | OrderCancelledEvent |
| 退款 → 恢复库存 | PaymentApplicationService.refundPayment() 直接调用 | 发布 PaymentRefundedEvent | PaymentRefundedEvent |

### 2. OrderPaidEvent 移除

**原因**：当前设计中的 `OrderPaidEvent` 在「订单支付成功后」触发，但：
- 若指 `payOrder()` 调用，则不应扣减库存（库存扣减在支付回调时）
- 若指支付回调，则已由 `PaymentSuccessEvent` 覆盖

**修正**：移除 `OrderPaidEvent`，避免语义歧义。支付成功场景统一由 `PaymentSuccessEvent` 处理。

### 3. Domain 事件类清单更新

**保留的事件**（4 个）：
- `OrderCreatedEvent` — 订单创建时预占库存
- `OrderCancelledEvent` — 订单取消时释放库存
- `PaymentSuccessEvent` — 支付回调成功时扣减库存
- `PaymentRefundedEvent` — 退款时恢复库存

**移除的事件**：
- ~~`OrderPaidEvent`~~ — 与 `PaymentSuccessEvent` 语义重叠

### 4. Order 聚合根事件收集策略

**设计假设澄清**：
- 「假设：Order 聚合根内部不持有事件列表，由 Application 层决定何时发布」 — **维持此假设**
- **不新增** `registerEvents()` 方法，简化实现
- 事件发布由 Application 层在事务边界内显式调用 `DomainEventPublisher.publish()`

**理由**：
- 聚合根持有事件列表需引入额外复杂度（事件收集、清空、批量发布）
- 当前业务场景简单，直接发布即可满足需求
- 若未来引入事件溯源（Event Sourcing），再重构聚合根

### 5. Application 层改造范围更新

**OrderApplicationService**：
- 移除 `InventoryRepository` 依赖
- 新增 `DomainEventPublisher` 依赖
- `createOrder()` 和 `createOrderFromCart()` → 发布 `OrderCreatedEvent`（预占库存）
- `cancelOrder()` → 发布 `OrderCancelledEvent`（释放库存）
- `payOrder()` → **不发布库存事件**（仅更新订单状态）

**PaymentApplicationService**：
- 移除 `InventoryRepository` 依赖
- 新增 `DomainEventPublisher` 依赖
- `handleCallback()` → 发布 `PaymentSuccessEvent`（扣减库存）
- `refundPayment()` → 发布 `PaymentRefundedEvent`（恢复库存）

---

## 架构评审

**评审人**：@architect
**日期**：2026-04-26
**结论**：✅ 通过（已自行修正设计歧义）

**熵检查基线**：`entropy-check.sh` 退出码 0，12 WARN，0 FAIL

### 评审检查项（15 维四类）

**架构合规（7 项）**
- [x] 聚合根边界合理（遵循事务一致性原则） — Order/Payment/Inventory 各为独立聚合，事件驱动实现跨聚合最终一致性
- [x] 值对象识别充分（金额、标识符等应为 VO） — OrderItemInfo 为不可变值对象，eventId、aggregateId 等均为 String（简化实现）
- [x] Repository 端口粒度合适（方法不多不少） — 无新增 Repository，现有 InventoryRepository 方法够用
- [x] 与已有聚合无循环依赖 — Order → Inventory 无直接依赖，通过事件解耦；Payment → Inventory 同理
- [x] DDL 设计与领域模型一致（字段映射、索引合理） — 无新增 DDL，仅改造 Application 层
- [x] API 设计符合 RESTful 规范 — 无新增 API，现有 API 保持不变
- [x] 对象转换链正确（DO ↔ Domain ↔ DTO ↔ Request/Response） — 事件对象为纯 Domain 层概念，不涉及跨层转换

**需求质量（3 项）**
- [x] 需求无歧义：核心名词、流程、异常分支均有明明确义 — 已澄清库存扣减时机、移除 OrderPaidEvent、统一事件语义
- [x] 验收条件可验证：每条 AC 可转化为 `should_xxx_when_yyy` 测试用例 — 4 条 AC 对应 4 个事件监听器测试
- [x] 业务规则完备：状态机/不变量/边界值在需求中已列明 — 事件触发时机、监听器职责已明确

**计划可执行性（2 项）**
- [x] task-plan 粒度合格：按层任务已分解到原子级（10–15 分钟/步），每步含文件路径 + 验证命令 + 预期输出 — task-plan.md 已按模板拆分 8 个原子任务（Domain 事件类 + Infrastructure 监听器 + Application 改造）
- [x] 依赖顺序正确：domain → application → infrastructure → adapter → start 自下而上，层间依赖无倒置 — 执行顺序：Domain 事件基类 → Domain 具体事件 → Infrastructure 发布实现 → Infrastructure 监听器 → Application 改造

**可测性保障（3 项）**
- [x] **AC 自动化全覆盖**：验收条件 1-4 均可转化为自动化测试（事件发布验证、监听器调用验证）
- [x] **可测的注入方式**：DomainEventPublisher 使用构造函数注入，Application 层测试可 Mock
- [x] **配置校验方式合规**：无敏感配置校验需求，不涉及此检查项

**心智原则（Karpathy — 动手前自检）**
- [x] **简洁性**：移除 OrderPaidEvent 避免冗余；不引入聚合根事件列表机制；仅使用 Spring 内置事件而非 MQ
- [x] **外科性**：仅改造 OrderApplicationService/PaymentApplicationService 的库存协作部分，其他功能保持不变
- [x] **假设显性**：「假设与待确认」章节已列出 3 项假设，并在「设计澄清与修正」章节进一步澄清

### 评审意见

**正面评价**：
1. **六边形架构边界清晰**：DomainEventPublisher 接口定义在 Domain 层（端口），SpringDomainEventPublisher 实现放在 Infrastructure 层（适配器），符合六边形架构原则。
2. **事务边界合理**：采用 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` 实现跨聚合最终一致性，主事务失败不影响库存副作用。
3. **解耦效果显著**：Order/Payment 聚合移除对 InventoryRepository 的直接依赖，通过事件驱动实现松耦合。
4. **Java 8 兼容**：所有代码设计符合 Java 8 语法约束，无 var/records/text blocks/List.of 等。

**已修正问题**：
1. **库存扣减时机歧义**：已澄清库存扣减仅在支付回调时发生，移除 OrderPaidEvent，避免重复扣减。
2. **Order 聚合根事件收集**：维持「由 Application 层显式发布」假设，不新增 registerEvents() 方法，简化实现。
3. **影响范围更新**：移除 OrderPaidEvent 相关类和测试，减少工作量。

**潜在风险与建议**：
1. **事件监听器失败处理**：设计假设「记录日志但不阻塞主流程」，建议在监听器中显式 try-catch + log.error，避免异常传播。
2. **事件顺序依赖**：若后续业务要求「创建订单 → 预占库存 → 支付 → 扣减库存」严格顺序，需考虑事件幂等性设计（当前 InventoryApplicationService.reserveStock() 已支持重复调用检查）。
3. **集成测试覆盖**：建议在 start 模块增加事件流程集成测试，覆盖「创建订单 → 预占库存 → 取消订单 → 释放库存」全链路。

### 需要新增的 ADR

**ADR-006: 领域事件基础设施选型**

**背景**：跨聚合协作需要事件驱动机制，面临技术选型决策。

**决策**：
- 使用 Spring ApplicationEventPublisher + @TransactionalEventListener 作为事件基础设施
- 不引入外部消息队列（MQ）
- Domain 层定义 DomainEventPublisher 端口接口，Infrastructure 层实现

**理由**：
- Spring 内置支持，零额外依赖
- @TransactionalEventListener 保证事务隔离
- 演示项目规模，无需 Kafka/RabbitMQ 等重量级中间件
- 符合六边形架构（端口在 Domain，适配器在 Infrastructure）

**影响**：
- 事件监听器在发布方事务提交后执行（最终一致性）
- 监听器失败不影响主事务（需显式异常处理）
- 不支持跨服务事件传递（单体应用场景）