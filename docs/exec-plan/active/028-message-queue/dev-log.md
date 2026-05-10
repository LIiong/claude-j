# 开发日志 — 028-message-queue

## 问题记录

### 1. 消费侧落点是否需要新聚合
- **Issue**：Spec 阶段需要决定“订单创建消息被消费后”的落点。如果只做日志打印，虽然能证明 RabbitMQ 链路打通，但无法体现 DDD 模式下消费侧状态建模；如果直接在 infrastructure 处理，又会让演示价值不足。
- **Root Cause**：需求要求“最小可运行生产/消费示例”与“保持 DDD + 六边形架构”，这意味着不仅要打通中间件，还要明确消费后的业务承载位置。现有聚合中没有适合承接“通知处理结果”的模型。
- **Fix**：在 `requirement-design.md` 中选择新增轻量 `notification` 聚合，最小化其职责，仅负责通知处理结果、幂等和状态表达；同时将此点标记为待 architect 确认，保留退化为基础设施日志消费者的备选方案。
- **Verification**：`Spec review` → `requirement-design.md` 已记录新增 `notification` 聚合方案、备选退化方案与待确认项

### 2. MQ 选型需要兼顾演示友好和后续可扩展性
- **Issue**：D1 明确要求在 RabbitMQ / Kafka / RocketMQ 中比较并优先选择最适合本项目本地演示与 docker-compose 落地的方案，不能仅凭常识直接下结论。
- **Root Cause**：三种 MQ 都可完成异步通知，但本项目当前仍是单体教学/半生产示范，核心约束不是峰值吞吐，而是本地启动复杂度、Spring Boot 2.7 / Java 8 集成成本、与现有事件模型的耦合度。
- **Fix**：在 `requirement-design.md` 中加入三方案对比表，从 docker-compose 复杂度、概念重量、场景匹配度、集成成熟度四个维度比较，并明确选 RabbitMQ 作为 D1 实施方案。
- **Verification**：`Spec review` → `requirement-design.md` 已包含 MQ 选型对比表与 RabbitMQ 结论

## 变更记录

- 无与原设计不一致的变更。
