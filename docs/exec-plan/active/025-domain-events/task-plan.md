# 任务执行计划 — 025-domain-events

## 任务状态跟踪

<!-- 状态流转：待办 → 进行中 → 单测通过 → 待验收 → 验收通过 / 待修复 -->

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Domain: DomainEvent 基类 + 测试 | dev | 待办 | |
| 2 | Domain: DomainEventPublisher 端口接口 | dev | 待办 | |
| 3 | Domain: OrderItemInfo 值对象 + 测试 | dev | 待办 | |
| 4 | Domain: OrderCreatedEvent + 测试 | dev | 待办 | |
| ~~5~~ | ~~Domain: OrderPaidEvent + 测试~~ | ~~dev~~ | ~~已移除~~ | 架构评审：与 PaymentSuccessEvent 语义重叠 |
| 5 | Domain: OrderCancelledEvent + 测试 | dev | 待办 | 原#6，OrderPaidEvent 移除后重新编号 |
| 6 | Domain: PaymentSuccessEvent + 测试 | dev | 待办 | 原#7 |
| 7 | Domain: PaymentRefundedEvent + 测试 | dev | 待办 | 原#8 |
| 8 | Infrastructure: SpringDomainEventPublisher + 测试 | dev | 待办 | |
| 9 | Infrastructure: InventoryEventListener + 测试 | dev | 待办 | |
| 10 | Application: OrderApplicationService 改造（发布事件）+ 更新测试 | dev | 待办 | payOrder() 不发布库存事件 |
| 11 | Application: PaymentApplicationService 改造（发布事件）+ 更新测试 | dev | 待办 | |
| 12 | 全量 mvn test | dev | 待办 | |
| 13 | 全量 mvn checkstyle:check | dev | 待办 | |
| 14 | ./scripts/entropy-check.sh | dev | 待办 | |
| 15 | QA: 测试用例设计 | qa | 待办 | |
| 16 | QA: 验收测试 + 代码审查 | qa | 待办 | |
| 17 | QA: 事件流程集成测试 | qa | 待办 | |

<!-- 根据实际需求增减任务行，保持编号连续 -->

## 执行顺序

domain（事件基类 + 端口接口 + 具体事件） → infrastructure（事件发布实现 + 监听器） → application（改造服务发布事件） → 全量测试 → QA 验收

## 原子任务分解（每项 10–15 分钟，单会话可完成并 commit）

> **目的**：将上表「按层」的粗粒度任务拆到 10–15 分钟的原子级，便于 Ralph Loop 单轮执行完整交付、便于新会话恢复时定位进度。
>
> **要求**：每个原子任务必填 5 个字段 — `文件路径`、`骨架片段`、`验证命令`、`预期输出`、`commit 消息`。

### 1.1 Domain 事件基类 DomainEvent
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/common/event/DomainEvent.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/common/event/DomainEventTest.java`
- **骨架**（Red 阶段先写测试）：
  ```java
  // DomainEventTest.java — 覆盖构造校验 / 相等性 / 不可变
  @Test
  void should_throw_when_event_id_is_null() { ... }
  @Test
  void should_throw_when_occurred_on_is_null() { ... }
  @Test
  void should_throw_when_aggregate_type_is_null() { ... }
  @Test
  void should_throw_when_aggregate_id_is_null() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=DomainEventTest`
- **预期输出**：`Tests run: X, Failures: 0, Errors: 0`（先看红再看绿）
- **commit**：`feat(domain): 领域事件基类 DomainEvent`

### 1.2 Domain 事件发布端口 DomainEventPublisher
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/common/event/DomainEventPublisher.java`
- **说明**：仅接口定义，无测试（纯接口）
- **骨架**：
  ```java
  public interface DomainEventPublisher {
      void publish(DomainEvent event);
  }
  ```
- **验证命令**：`mvn compile -pl claude-j-domain`
- **预期输出**：编译通过
- **commit**：`feat(domain): 事件发布端口 DomainEventPublisher`

### 1.3 Domain 值对象 OrderItemInfo
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/order/event/OrderItemInfo.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/order/event/OrderItemInfoTest.java`
- **骨架**（Red 阶段先写测试）：
  ```java
  // OrderItemInfoTest.java — 覆盖构造校验 / 相等性 / 不可变
  @Test
  void should_throw_when_product_id_is_null() { ... }
  @Test
  void should_throw_when_quantity_is_negative() { ... }
  @Test
  void should_equal_when_all_fields_match() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=OrderItemInfoTest`
- **预期输出**：`Tests run: X, Failures: 0, Errors: 0`
- **commit**：`feat(domain): 订单项信息值对象 OrderItemInfo`

### 1.4 Domain 事件 OrderCreatedEvent
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/order/event/OrderCreatedEvent.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/order/event/OrderCreatedEventTest.java`
- **骨架**（Red 阶段先写测试）：
  ```java
  // OrderCreatedEventTest.java
  @Test
  void should_create_event_with_valid_params() { ... }
  @Test
  void should_throw_when_order_id_is_null() { ... }
  @Test
  void should_contain_order_items() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=OrderCreatedEventTest`
- **预期输出**：`Tests run: X, Failures: 0, Errors: 0`
- **commit**：`feat(domain): 订单创建事件 OrderCreatedEvent`

### 1.5 Domain 事件 OrderCancelledEvent（原 1.6，OrderPaidEvent 已移除）
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/order/event/OrderCancelledEvent.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/order/event/OrderCancelledEventTest.java`
- **骨架**：类似 OrderCreatedEvent
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=OrderCancelledEventTest`
- **预期输出**：`Tests run: X, Failures: 0, Errors: 0`
- **commit**：`feat(domain): 订单取消事件 OrderCancelledEvent`

### 1.6 Domain 事件 PaymentSuccessEvent（原 1.7）
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/payment/event/PaymentSuccessEvent.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/payment/event/PaymentSuccessEventTest.java`
- **骨架**（Red 阶段先写测试）：
  ```java
  // PaymentSuccessEventTest.java
  @Test
  void should_create_event_with_valid_params() { ... }
  @Test
  void should_throw_when_payment_id_is_null() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=PaymentSuccessEventTest`
- **预期输出**：`Tests run: X, Failures: 0, Errors: 0`
- **commit**：`feat(domain): 支付成功事件 PaymentSuccessEvent`

### 1.7 Domain 事件 PaymentRefundedEvent（原 1.8）
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/payment/event/PaymentRefundedEvent.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/payment/event/PaymentRefundedEventTest.java`
- **骨架**：类似 PaymentSuccessEvent
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=PaymentRefundedEventTest`
- **预期输出**：`Tests run: X, Failures: 0, Errors: 0`
- **commit**：`feat(domain): 支付退款事件 PaymentRefundedEvent`

### 2.1 Infrastructure 事件发布实现 SpringDomainEventPublisher
- **文件**：`claude-j-infrastructure/src/main/java/com/claudej/infrastructure/common/event/SpringDomainEventPublisher.java`
- **测试**：`claude-j-infrastructure/src/test/java/com/claudej/infrastructure/common/event/SpringDomainEventPublisherTest.java`
- **骨架**：
  ```java
  // SpringDomainEventPublisher.java
  @Component
  public class SpringDomainEventPublisher implements DomainEventPublisher {
      private final ApplicationEventPublisher applicationEventPublisher;

      public SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
          this.applicationEventPublisher = applicationEventPublisher;
      }

      @Override
      public void publish(DomainEvent event) {
          applicationEventPublisher.publishEvent(event);
      }
  }
  ```
- **验证命令**：`mvn test -pl claude-j-infrastructure -Dtest=SpringDomainEventPublisherTest`
- **预期输出**：`Tests run: X, Failures: 0, Errors: 0`
- **commit**：`feat(infrastructure): 事件发布实现 SpringDomainEventPublisher`

### 2.2 Infrastructure 事件监听器 InventoryEventListener
- **文件**：`claude-j-infrastructure/src/main/java/com/claudej/infrastructure/inventory/event/InventoryEventListener.java`
- **测试**：`claude-j-infrastructure/src/test/java/com/claudej/infrastructure/inventory/event/InventoryEventListenerTest.java`
- **骨架**：
  ```java
  // InventoryEventListener.java
  @Component
  public class InventoryEventListener {
      private final InventoryApplicationService inventoryApplicationService;

      @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
      public void onOrderCreated(OrderCreatedEvent event) { ... }

      @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
      public void onPaymentSuccess(PaymentSuccessEvent event) { ... }

      @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
      public void onOrderCancelled(OrderCancelledEvent event) { ... }

      @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
      public void onPaymentRefunded(PaymentRefundedEvent event) { ... }
  }
  // 注意：已移除 onOrderPaid()，库存扣减仅在支付回调时由 onPaymentSuccess 处理
  ```
- **验证命令**：`mvn test -pl claude-j-infrastructure -Dtest=InventoryEventListenerTest`
- **预期输出**：`Tests run: X, Failures: 0, Errors: 0`
- **commit**：`feat(infrastructure): 库存事件监听器 InventoryEventListener`

### 3.1 Application 改造 OrderApplicationService
- **文件**：`claude-j-application/src/main/java/com/claudej/application/order/service/OrderApplicationService.java`
- **测试**：`claude-j-application/src/test/java/com/claudej/application/order/service/OrderApplicationServiceTest.java`
- **改造内容**：
  1. 移除 InventoryRepository 依赖
  2. 新增 DomainEventPublisher 依赖
  3. createOrder() 移除 reserveStockForOrder()，改为发布 OrderCreatedEvent
  4. payOrder() 移除 deductStockForOrder()，不发布库存事件（仅更新订单状态）
  5. cancelOrder() 移除 releaseStockForOrder()，改为发布 OrderCancelledEvent
  6. createOrderFromCart() 移除 reserveStockForOrder()，改为发布 OrderCreatedEvent
- **验证命令**：`mvn test -pl claude-j-application -Dtest=OrderApplicationServiceTest`
- **预期输出**：`Tests run: X, Failures: 0, Errors: 0`，Mock verify 事件发布
- **commit**：`refactor(application): OrderApplicationService 改用事件驱动库存协作`

### 3.2 Application 改造 PaymentApplicationService
- **文件**：`claude-j-application/src/main/java/com/claudej/application/payment/service/PaymentApplicationService.java`
- **测试**：`claude-j-application/src/test/java/com/claudej/application/payment/service/PaymentApplicationServiceTest.java`
- **改造内容**：
  1. 移除 InventoryRepository 依赖
  2. 新增 DomainEventPublisher 依赖
  3. handleCallback() 移除 deductStockForOrder()，改为发布 PaymentSuccessEvent
  4. refundPayment() 移除 releaseStockForOrder()，改为发布 PaymentRefundedEvent
- **验证命令**：`mvn test -pl claude-j-application -Dtest=PaymentApplicationServiceTest`
- **预期输出**：`Tests run: X, Failures: 0, Errors: 0`，Mock verify 事件发布
- **commit**：`refactor(application): PaymentApplicationService 改用事件驱动库存协作`

### 4.1 全量测试验证
- **验证命令**：`mvn clean test`
- **预期输出**：`Tests run: XXX, Failures: 0, Errors: 0`，含 ArchUnit 14 条架构规则通过
- **commit**：无需 commit，验证阶段

### 4.2 Checkstyle 验证
- **验证命令**：`mvn checkstyle:check`
- **预期输出**：Exit 0
- **commit**：无需 commit，验证阶段

### 4.3 Entropy Check 验证
- **验证命令**：`./scripts/entropy-check.sh`
- **预期输出**：12/12 checks passed
- **commit**：无需 commit，验证阶段

## 开发完成记录
<!-- dev 完成后填写 -->
- 全量 `mvn clean test`：待填写
- 架构合规检查：待填写
- 通知 @qa 时间：待填写

## QA 验收记录
<!-- qa 验收后填写 -->
- 全量测试（含集成测试）：待填写
- 代码审查结果：待填写
- 代码风格检查：待填写
- 问题清单：详见 test-report.md
- **最终状态**：待填写