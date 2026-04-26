---
task-id: "025-domain-events"
from: dev
to: architect
status: pending-review
timestamp: "2026-04-26T03:34:09Z"
pre-flight:
  mvn-test: pending
  checkstyle: pending
  entropy-check: pending
artifacts:
  - requirement-design.md
  - task-plan.md
  - dev-log.md
summary: "实现领域事件机制，将 Order/Payment 对 Inventory 的同步调用改为事件驱动协作。设计 DomainEvent 基类、5 个具体事件（OrderCreatedEvent/OrderPaidEvent/OrderCancelledEvent/PaymentSuccessEvent/PaymentRefundedEvent）、DomainEventPublisher 端口接口。Infrastructure 层实现 SpringDomainEventPublisher 和 InventoryEventListener。Application 层改造 OrderApplicationService/PaymentApplicationService 发布事件而非直接调用 InventoryRepository。使用 @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT) 保证事务隔离和最终一致性。"
---

## 评审要点

1. **架构边界**：Domain 层事件基类/端口接口是否保持纯净（无 Spring 依赖）
2. **依赖方向**：DomainEventPublisher 接口定义在 Domain，实现在 Infrastructure（符合六边形架构）
3. **事件命名**：事件类名是否遵循 DomainEvent + Aggregate + Action 模式
4. **事务边界**：@TransactionalEventListener AFTER_COMMIT 是否符合跨聚合最终一致性原则
5. **改造范围**：OrderApplicationService/PaymentApplicationService 移除 InventoryRepository 是否影响其他功能
6. **测试覆盖**：事件类/监听器/服务改造的测试策略是否合理