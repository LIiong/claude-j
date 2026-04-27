# 测试用例设计 — 025-domain-events

## 测试范围
实现领域事件机制，将 Order/Payment 对 Inventory 的同步调用改为事件驱动协作。新增 DomainEvent 基类、4 个具体领域事件、DomainEventPublisher 端口，以及 InventoryEventListener 监听器。


## 测试策略
按测试金字塔分层测试 + 代码审查 + 风格检查。

---

## 0. AC 自动化覆盖矩阵（强制，动笔前先填）

> **背景**：010-secret-externalize 的 AC1「未设 JWT_SECRET 启动失败」被标为"手动验证"，关键安全保证失去回归保障。从 011 起每条 AC 必须在此表留下可回溯的自动化证据。
>
> **规则**：
> 1. 列出 `requirement-design.md#验收标准` 的每一条 AC
> 2. 每条必须映射到至少 1 个自动化测试（哪一层、哪个测试方法名）
> 3. 若标「手动」，必须说明**为什么不能自动化** + **替代自动化测试**（即便是弱化版）
> 4. @architect 评审时会校验本表是否填写完整，不全则 changes-requested

| # | 验收条件（AC） | 自动化层 | 对应测试方法 | 手动备注（若有） |
|---|---|---|---|---|
| AC1 | Order 创建时发布 OrderCreatedEvent，Inventory 监听器正确预占库存 | Start（集成） | `DomainEventIntegrationTest.should_reserveStock_when_orderCreated` | - |
| AC2 | Payment 成功时发布 PaymentSuccessEvent，Inventory 监听器正确扣减库存 | Start（集成） | `DomainEventIntegrationTest.should_deductStock_when_paymentSuccess` | - |
| AC3 | Order 取消时发布 OrderCancelledEvent，库存正确释放 | Start（集成） | `DomainEventIntegrationTest.should_releaseStock_when_orderCancelled` | - |
| AC4 | Payment 退款时发布 PaymentRefundedEvent，库存正确恢复 | Start（集成） | `DomainEventIntegrationTest.should_restoreStock_when_paymentRefunded` | - |
| AC5 | 三项预飞（mvn test / checkstyle / entropy-check）全过 | 全量检查 | `mvn test` / `checkstyle:check` / `entropy-check.sh` | - |
| AC6 | JaCoCo 阈值不下滑（domain 90% / application 80%） | 构建检查 | `mvn test` 包含覆盖率报告 | - |

**反模式（禁止）**：
- ❌ "启动失败无法自动化" → 错。用 `ApplicationContextRunner` / `@SpringBootTest(properties=...)` 均可模拟启动失败
- ❌ "性能场景只能手测" → 错。用 JMH / `@Timeout` 可自动化门限
- ❌ "CI 环境才触发" → 错。把 CI 的环境变量注入等价物放到 `@TestPropertySource` 即可

---

## 一、Domain 层测试场景

<!-- 纯单元测试，JUnit 5 + AssertJ，禁止 Spring 上下文 -->

### DomainEvent 基类
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D1 | 合法值创建 | - | new TestDomainEvent(validParams) | 创建成功，字段正确 |
| D2 | eventId 为空 | - | new TestDomainEvent(null, ...) | 抛 IllegalArgumentException |
| D3 | eventId 为空字符串 | - | new TestDomainEvent("", ...) | 抛 IllegalArgumentException |
| D4 | occurredOn 为空 | - | new TestDomainEvent(id, null, ...) | 抛 IllegalArgumentException |
| D5 | aggregateType 为空 | - | new TestDomainEvent(..., null, ...) | 抛 IllegalArgumentException |
| D6 | aggregateId 为空 | - | new TestDomainEvent(..., null) | 抛 IllegalArgumentException |
| D7 | 字段不可变 | 已创建事件 | 尝试修改字段（通过反射） | 字段为 final，无法修改 |

### OrderItemInfo 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D8 | 合法值创建 | - | new OrderItemInfo("P001", "Product", 10) | 创建成功 |
| D9 | productId 为空 | - | new OrderItemInfo(null, "Product", 10) | 抛 IllegalArgumentException |
| D10 | productName 为空 | - | new OrderItemInfo("P001", null, 10) | 抛 IllegalArgumentException |
| D11 | quantity 为 0 | - | new OrderItemInfo("P001", "Product", 0) | 抛 IllegalArgumentException |
| D12 | quantity 为负数 | - | new OrderItemInfo("P001", "Product", -5) | 抛 IllegalArgumentException |
| D13 | 相等性相同 | - | 两个相同值对象 | equals 返回 true，hashCode 相同 |
| D14 | 相等性不同 | - | 两个不同值对象 | equals 返回 false |
| D15 | 不可变性 | 已创建对象 | items 列表外部修改 | 内部列表不受影响（defensive copy） |

### OrderCreatedEvent
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D16 | 正常创建 | - | OrderCreatedEvent.create(orderId, customerId, items) | 创建成功，eventId 自动生成 |
| D17 | 构造函数参数校验 | - | new OrderCreatedEvent(null, now, ...) | 抛 IllegalArgumentException |
| D18 | orderId 为空 | - | OrderCreatedEvent.create(null, "C001", items) | 抛 IllegalArgumentException |
| D19 | customerId 为空 | - | OrderCreatedEvent.create("O001", null, items) | 抛 IllegalArgumentException |
| D20 | items 为空列表 | - | OrderCreatedEvent.create("O001", "C001", emptyList) | 抛 IllegalArgumentException |
| D21 | items 列表不可变 | 已创建事件 | 尝试修改返回的 items | 抛 UnsupportedOperationException |
| D22 | 继承基类属性 | - | 创建后获取 aggregateType | 返回 "Order" |

### OrderCancelledEvent
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D23 | 正常创建 | - | OrderCancelledEvent.create(orderId, customerId, items) | 创建成功 |
| D24 | 参数校验 | - | 各种非法参数组合 | 均抛 IllegalArgumentException |
| D25 | items 防御拷贝 | 传入可变列表 | 外部修改原列表 | 事件内部列表不变 |

### PaymentSuccessEvent
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D26 | 正常创建 | - | PaymentSuccessEvent.create(...) | 创建成功，含 transactionNo |
| D27 | paymentId 为空 | - | PaymentSuccessEvent.create(null, ...) | 抛 IllegalArgumentException |
| D28 | transactionNo 为空 | - | PaymentSuccessEvent.create(..., null, items) | 抛 IllegalArgumentException |
| D29 | items 不可为空 | - | PaymentSuccessEvent.create(..., emptyList) | 抛 IllegalArgumentException |

### PaymentRefundedEvent
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D30 | 正常创建 | - | PaymentRefundedEvent.create(...) | 创建成功 |
| D31 | 参数校验 | - | 各种非法参数 | 均抛 IllegalArgumentException |
| D32 | 继承基类属性 | - | 创建后获取 aggregateType | 返回 "Payment" |

---

## 二、Application 层测试场景

<!-- Mock 单元测试，JUnit 5 + Mockito，禁止 Spring 上下文 -->

### OrderApplicationService
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| A1 | createOrder 发布事件 | Mock 依赖 | createOrder(cmd) | verify domainEventPublisher.publish() 被调用 1 次，参数为 OrderCreatedEvent |
| A2 | createOrderFromCart 发布事件 | Mock 依赖 | createOrderFromCart(cmd) | verify domainEventPublisher.publish() 被调用 1 次，参数为 OrderCreatedEvent |
| A3 | cancelOrder 发布事件 | Mock 依赖，订单存在 | cancelOrder(orderId) | verify domainEventPublisher.publish() 被调用 1 次，参数为 OrderCancelledEvent |
| A4 | payOrder 不发布库存事件 | Mock 依赖，订单存在 | payOrder(orderId) | verify domainEventPublisher.publish() **不被调用** |
| A5 | 移除 InventoryRepository 依赖 | - | 检查构造函数 | 不再依赖 InventoryRepository |

### PaymentApplicationService
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| A6 | handleCallback 成功时发布事件 | Mock 依赖，支付 Pending | handleCallback(successCmd) | verify domainEventPublisher.publish() 被调用 1 次，参数为 PaymentSuccessEvent |
| A7 | handleCallback 失败时不发布事件 | Mock 依赖，支付 Pending | handleCallback(failCmd) | verify domainEventPublisher.publish() **不被调用** |
| A8 | refundPayment 发布事件 | Mock 依赖，支付 Success | refundPayment(paymentId, cmd) | verify domainEventPublisher.publish() 被调用 1 次，参数为 PaymentRefundedEvent |
| A9 | 移除 InventoryRepository 依赖 | - | 检查构造函数 | 不再依赖 InventoryRepository |

---

## 三、Infrastructure 层测试场景

<!-- 集成测试，@SpringBootTest + H2 -->

### SpringDomainEventPublisher
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| I1 | 正常发布事件 | Spring 上下文 | publish(event) | Spring ApplicationEventPublisher.publishEvent() 被调用 |
| I2 | 发布 null 事件 | - | publish(null) | 抛 IllegalArgumentException |
| I3 | 构造函数注入 | - | 创建实例 | 通过构造函数注入 ApplicationEventPublisher |

### InventoryEventListener
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| I4 | onOrderCreated 预占库存 | Mock InventoryAppService | onOrderCreated(event) | verify reserveStock() 被调用，每个 item 一次 |
| I5 | onPaymentSuccess 扣减库存 | Mock InventoryAppService | onPaymentSuccess(event) | verify deductStock() 被调用 |
| I6 | onOrderCancelled 释放库存 | Mock InventoryAppService | onOrderCancelled(event) | verify releaseStock() 被调用 |
| I7 | onPaymentRefunded 恢复库存 | Mock InventoryAppService | onPaymentRefunded(event) | verify adjustStock() 被调用 |
| I8 | 监听器异常不抛出 | Mock 抛异常 | onOrderCreated(event) | 捕获异常，记录 error log，不抛出 |
| I9 | 事务监听器注解 | - | 检查方法注解 | 存在 @TransactionalEventListener(phase = AFTER_COMMIT) 和 @Transactional |

---

## 四、Adapter 层测试场景

<!-- API 测试，@WebMvcTest + MockMvc -->

本次任务无新增 REST API，仅改造内部协作机制。现有 API 测试已在对应聚合中覆盖。

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| W1 | 创建订单 API | - | POST /api/v1/orders | 功能不变，内部改用事件 |
| W2 | 取消订单 API | - | POST /api/v1/orders/{id}/cancel | 功能不变，内部改用事件 |
| W3 | 支付回调 API | - | POST /api/v1/payments/callback | 功能不变，内部改用事件 |
| W4 | 退款 API | - | POST /api/v1/payments/{id}/refund | 功能不变，内部改用事件 |

---

## 五、集成测试场景（全链路）

<!-- @SpringBootTest + AutoConfigureMockMvc + H2，在 start 模块 -->

### DomainEventIntegrationTest（start 模块）

| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E1 | 创建订单触发库存预占 | 1. 创建 Inventory（初始可用库存 100）<br>2. 调用 POST /api/v1/orders 创建订单（数量 10）<br>3. 查询 Inventory 状态 | 可用库存变为 90，预留库存变为 10 |
| E2 | 支付成功触发库存扣减 | 1. 完成 E1（已预占 10）<br>2. 调用支付回调 POST /api/v1/payments/callback<br>3. 查询 Inventory 状态 | 可用库存保持 90，预留库存变为 0，实际库存变为 90 |
| E3 | 取消订单触发库存释放 | 1. 完成 E1（已预占 10）<br>2. 调用 POST /api/v1/orders/{id}/cancel<br>3. 查询 Inventory 状态 | 可用库存恢复为 100，预留库存变为 0 |
| E4 | 事务失败时事件不触发 | 1. 创建 Inventory（初始可用 100）<br>2. 模拟订单保存失败（如唯一约束冲突）<br>3. 查询 Inventory 状态 | 库存保持不变（事件不触发） |
| E5 | 事件发布与监听解耦 | 执行任意触发事件的操作 | 主事务成功提交后，监听器才执行（最终一致性） |

---

## 六、代码审查检查项

- [x] 依赖方向正确（adapter → application → domain ← infrastructure）
- [x] domain 模块无 Spring/框架 import
- [x] 聚合根封装业务不变量（非贫血模型）— 本次无新增聚合根
- [x] 值对象不可变（OrderItemInfo 为 final 类，字段 final），equals/hashCode 正确
- [x] Repository 接口在 domain，实现在 infrastructure — 本次未新增 Repository
- [x] 对象转换链正确：DO ↔ Domain ↔ DTO ↔ Request/Response — 事件对象不涉及跨层转换
- [x] Controller 无业务逻辑 — 本次未修改 Controller
- [x] 异常通过 GlobalExceptionHandler 统一处理 — 本次未修改异常处理
- [x] DomainEventPublisher 端口在 Domain 层，SpringDomainEventPublisher 实现在 Infrastructure 层
- [x] InventoryEventListener 使用 @TransactionalEventListener(AFTER_COMMIT) 保证最终一致性
- [x] 监听器异常处理：try-catch + log.error，不抛异常影响主事务

## 七、代码风格检查项

- [x] Java 8 兼容（无 var、records、text blocks、List.of）
- [x] DomainEvent 基类使用 @Getter（非 @Data）
- [x] OrderItemInfo 值对象使用 @Getter + 手动 equals/hashCode/toString
- [x] 事件类字段为 final，保证不可变性
- [x] 命名规范：DomainEvent, OrderCreatedEvent, DomainEventPublisher 符合规范
- [x] 包结构符合 com.claudej.{layer}.{aggregate}.{sublayer}
- [x] 测试命名 should_xxx_when_xxx（如 DomainEventTest.should_throw_when_event_id_is_null）
