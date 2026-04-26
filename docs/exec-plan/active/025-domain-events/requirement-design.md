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

**OrderPaidEvent**
- 继承 DomainEvent
- orderId (String) — 订单 ID
- customerId (String) — 客户 ID
- items (List<OrderItemInfo>) — 订单项信息
- 触发时机：订单支付成功后

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
- `com.claudej.domain.order.event.OrderPaidEvent` — 订单支付事件
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

**domain 层**：
- `com.claudej.domain.order.model.aggregate.Order` — 新增 registerEvents() 方法收集待发布事件

### 测试类（新增）

**domain 层**：
- `DomainEventTest` — 事件基类测试
- `OrderCreatedEventTest` — 订单创建事件测试
- `OrderPaidEventTest` — 订单支付事件测试
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