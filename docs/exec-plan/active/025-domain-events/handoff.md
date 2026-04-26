---
task-id: "025-domain-events"
from: dev
to: architect
status: approved
timestamp: "2026-04-26T03:34:09Z"
review-timestamp: "2026-04-26T03:50:00Z"
pre-flight:
  entropy-check: pass  # Exit 0, 12 WARN, 0 FAIL
artifacts:
  - requirement-design.md
  - task-plan.md
  - dev-log.md
  - docs/architecture/decisions/006-domain-event-infrastructure.md
summary: "实现领域事件机制，将 Order/Payment 对 Inventory 的同步调用改为事件驱动协作。设计 DomainEvent 基类、4 个具体事件（OrderCreatedEvent/OrderCancelledEvent/PaymentSuccessEvent/PaymentRefundedEvent）、DomainEventPublisher 端口接口。Infrastructure 层实现 SpringDomainEventPublisher 和 InventoryEventListener。Application 层改造 OrderApplicationService/PaymentApplicationService 发布事件而非直接调用 InventoryRepository。使用 @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT) 保证事务隔离和最终一致性。"
review-notes: |
  架构评审已通过。主要修正：
  1. 移除 OrderPaidEvent（与 PaymentSuccessEvent 语义重叠）
  2. 澄清库存扣减仅在支付回调时发生（payOrder() 不发布库存事件）
  3. 维持「Application 层显式发布事件」假设，不新增 Order.registerEvents() 方法
  4. task-plan.md 需 @dev 自行修正：移除原子任务 1.5（OrderPaidEvent），更新 3.1 改造内容
  5. 已创建 ADR-006 记录领域事件基础设施选型决策
---

## 下一步

@dev 请修正 task-plan.md 后开始 Build 阶段：
1. 移除原子任务 1.5（OrderPaidEvent）
2. 更新原子任务 3.1：移除 payOrder() 的库存扣减相关描述
3. 更新事件监听器骨架：移除 onOrderPaid() 方法