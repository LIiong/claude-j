# ADR-006: 领域事件基础设施选型

**日期**：2026-04-26
**状态**：已接受
**决策者**：@architect

## 背景

025-domain-events 任务引入领域事件机制，实现跨聚合协作（Order/Payment → Inventory）。面临事件基础设施技术选型决策：

1. **跨聚合协作需求**：创建订单预占库存、支付成功扣减库存、取消订单释放库存、退款恢复库存
2. **事务边界要求**：主事务与库存操作需解耦，避免主事务失败触发库存副作用
3. **架构边界约束**：Domain 层必须保持纯净，事件发布接口定义在 Domain，实现在 Infrastructure

## 决策

**使用 Spring ApplicationEventPublisher + @TransactionalEventListener 作为领域事件基础设施。**

技术方案：
| 组件 | 层级 | 说明 |
|------|------|------|
| DomainEvent | domain | 事件基类（抽象类），所有事件继承 |
| DomainEventPublisher | domain | 事件发布端口接口 |
| SpringDomainEventPublisher | infrastructure | 事件发布实现（适配器） |
| XxxEvent | domain | 具体领域事件类 |
| XxxEventListener | infrastructure | 事件监听器（适配器） |

事务策略：
- `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` — 主事务提交后触发监听器
- 监听器标注 `@Transactional` — 独立事务执行库存操作

## 备选方案

| 方案 | 优点 | 缺点 | 排除原因 |
|------|------|------|----------|
| 同步调用（当前实现） | 简单、事务一致 | 聚合耦合、并发瓶颈、事务边界模糊 | 违背跨聚合最终一致性原则 |
| @EventListener（同一事务） | 简单、Spring 原生 | 事件处理失败回滚主事务、耦合度高 | 不符合跨聚合事务隔离原则 |
| **@TransactionalEventListener** | Spring 原生、事务隔离、零额外依赖 | 最终一致性、需显式异常处理 | **选此** |
| Kafka/RabbitMQ | 分布式、可靠投递、重试机制 | 重量级、运维复杂、超出演示项目规模 | 演示项目无需 MQ |

## 影响

### 正向影响
- **解耦聚合**：Order/Payment 移除对 InventoryRepository 的直接依赖
- **事务隔离**：主事务失败不影响库存副作用（符合 DDD 跨聚合最终一致性原则）
- **架构合规**：Domain 层保持纯净，事件发布接口定义在 Domain，实现在 Infrastructure
- **可扩展性**：未来可平滑迁移到 MQ（仅替换 SpringDomainEventPublisher 实现）

### 负向影响
- **最终一致性**：监听器在主事务提交后执行，库存操作有微小延迟
- **失败处理**：监听器失败需显式 try-catch + log.error，否则异常被吞
- **单机限制**：不支持跨服务事件传递（单体应用场景）

### 缓解措施
- 监听器显式异常处理 + 日志记录
- 关键业务场景增加幂等性设计（如 reserveStock 重复调用检查）
- 未来若引入分布式架构，可替换为 MQ 实现

## 相关代码

- `com.claudej.domain.common.event.DomainEvent`
- `com.claudej.domain.common.event.DomainEventPublisher`
- `com.claudej.infrastructure.common.event.SpringDomainEventPublisher`
- `com.claudej.infrastructure.inventory.event.InventoryEventListener`

## 规则同步

- `docs/standards/java-dev.md` 增加 MUST：领域事件接口定义在 Domain 层，实现在 Infrastructure 层
- `@architect` checklist 增加一项：若改动涉及跨聚合协作，优先考虑事件驱动而非直接依赖