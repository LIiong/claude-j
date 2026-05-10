# 任务执行计划 — 028-message-queue

## 任务状态跟踪

<!-- 状态流转：待办 → 进行中 → 单测通过 → 待验收 → 验收通过 / 待修复 -->

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Domain: Notification 聚合 + 值对象 + 测试 | dev | 单测通过 | 已完成最小通知消费落点 |
| 2 | Domain: NotificationRepository 端口 | dev | 单测通过 | 已支撑消费幂等与结果查询 |
| 3 | Application: OrderCreatedMessage DTO + Publisher 端口 + Assembler | dev | 单测通过 | application 仅保留消息契约 |
| 4 | Application: NotificationApplicationService + 测试 | dev | 单测通过 | Mockito 用例覆盖成功/幂等/失败 |
| 5 | Infrastructure: RabbitMQ 配置 + Publisher + 事件桥接测试 | dev | 单测通过 | 事件桥接与 publisher 装配已覆盖 |
| 6 | Infrastructure: Notification 持久化实现 + H2 测试 | dev | 单测通过 | 新增 payload 含逗号回归测试并修复 converter |
| 7 | Infrastructure: RabbitMQ Consumer + 消费测试 | dev | 单测通过 | 消费监听委派通知应用服务 |
| 8 | Adapter: Notification 查询接口与测试（如评审通过需要） | dev | 验收通过 | 按评审结论本期不做 |
| 9 | Start: RabbitMQ 依赖、配置、schema、docker-compose | dev | 单测通过 | broker/runtime 配置已接通 |
| 10 | Start: 订单创建 → MQ → 通知 集成测试 | dev | 单测通过 | 全链路集成测试已通过 |
| 11 | 文档更新与开发日志整理 | dev | 待验收 | 已补齐全部 fresh pre-flight 证据 |
| 12 | 全量 mvn test | dev | 单测通过 | `Tests run: 69, Failures: 0, Errors: 0, Skipped: 0`（start 模块摘要） |
| 13 | 全量 mvn checkstyle:check | dev | 单测通过 | `You have 0 Checkstyle violations.` |
| 14 | 全量 ./scripts/entropy-check.sh | dev | 单测通过 | `{"issues": 0, "warnings": 14, "status": "PASS"}` |
| 15 | QA: 测试用例设计 | qa | 待办 | |
| 16 | QA: 验收测试 + 代码审查 | qa | 待办 | |

## 执行顺序
domain → application → infrastructure → adapter（如需要）→ start → 文档收口 → 全量测试 → QA 验收

## 原子任务分解（每项 10–15 分钟，单会话可完成并 commit）

> **目的**：将上表「按层」的粗粒度任务拆到 10–15 分钟的原子级，便于 Ralph Loop 单轮执行完整交付、便于新会话恢复时定位进度。
>
> **要求**：每个原子任务必填 5 个字段 — `文件路径`、`骨架片段`、`验证命令`、`预期输出`、`commit 消息`。

### 1.1 Domain 值对象 `NotificationId` / `NotificationPayload`
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-domain/src/main/java/com/claudej/domain/notification/model/valueobject/NotificationId.java`、`/Users/macro.li/aiProject/claude-j/claude-j-domain/src/main/java/com/claudej/domain/notification/model/valueobject/NotificationPayload.java`
- **测试**：`/Users/macro.li/aiProject/claude-j/claude-j-domain/src/test/java/com/claudej/domain/notification/model/valueobject/NotificationIdTest.java`、`/Users/macro.li/aiProject/claude-j/claude-j-domain/src/test/java/com/claudej/domain/notification/model/valueobject/NotificationPayloadTest.java`
- **骨架**（Red 阶段先写测试）：
  ```java
  @Test
  void should_throw_when_notification_id_is_blank() { ... }

  @Test
  void should_create_payload_when_order_snapshot_is_complete() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=NotificationIdTest,NotificationPayloadTest`
- **预期输出**：先红后绿；`Tests run: X, Failures: 0, Errors: 0`
- **commit**：`feat(domain): add notification value objects`

### 1.2 Domain 聚合根 `Notification`
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-domain/src/main/java/com/claudej/domain/notification/model/aggregate/Notification.java`
- **测试**：`/Users/macro.li/aiProject/claude-j/claude-j-domain/src/test/java/com/claudej/domain/notification/model/aggregate/NotificationTest.java`
- **骨架**：工厂 `createPending(...)` + `markSent(...)` + `markFailed(...)`；聚合内保证同订单同渠道幂等约束的状态表达
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=NotificationTest`
- **预期输出**：覆盖状态流转 / 非法参数 / 重复发送场景，全部绿
- **commit**：`feat(domain): add notification aggregate`

### 2.1 Domain 端口 `NotificationRepository`
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-domain/src/main/java/com/claudej/domain/notification/repository/NotificationRepository.java`
- **验证命令**：`mvn compile -pl claude-j-domain`
- **预期输出**：编译通过，无框架依赖泄漏
- **commit**：`feat(domain): add notification repository port`

### 3.1 Application 消息载体与端口
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-application/src/main/java/com/claudej/application/order/dto/OrderCreatedMessage.java`、`/Users/macro.li/aiProject/claude-j/claude-j-application/src/main/java/com/claudej/application/order/port/OrderMessagePublisher.java`
- **验证命令**：`mvn compile -pl claude-j-application`
- **预期输出**：编译通过，application 未依赖 MQ SDK
- **commit**：`feat(application): add order message contract`

### 3.2 Application `NotificationApplicationService`
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-application/src/main/java/com/claudej/application/notification/service/NotificationApplicationService.java`
- **测试**：`/Users/macro.li/aiProject/claude-j/claude-j-application/src/test/java/com/claudej/application/notification/service/NotificationApplicationServiceTest.java`
- **骨架**：
  ```java
  @Test
  void should_save_sent_notification_when_message_consumed_successfully() { ... }

  @Test
  void should_skip_when_notification_already_exists_for_order() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-application -Dtest=NotificationApplicationServiceTest`
- **预期输出**：Mockito verify 通过，先红后绿
- **commit**：`feat(application): add notification service`

### 5.1 Infrastructure RabbitMQ publisher 与桥接监听器
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-infrastructure/src/main/java/com/claudej/infrastructure/order/mq/RabbitMqOrderMessagePublisher.java`、`/Users/macro.li/aiProject/claude-j/claude-j-infrastructure/src/main/java/com/claudej/infrastructure/order/event/OrderCreatedEventBridgeListener.java`
- **测试**：`/Users/macro.li/aiProject/claude-j/claude-j-infrastructure/src/test/java/com/claudej/infrastructure/order/event/OrderCreatedEventBridgeListenerTest.java`
- **验证命令**：`mvn test -pl claude-j-infrastructure -Dtest=OrderCreatedEventBridgeListenerTest`
- **预期输出**：验证 `OrderCreatedEvent -> OrderCreatedMessage -> publisher.publish` 映射正确
- **commit**：`feat(infrastructure): bridge order event to rabbitmq`

### 6.1 Infrastructure Notification 持久化
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-infrastructure/src/main/java/com/claudej/infrastructure/notification/persistence/dataobject/NotificationDO.java`、`/Users/macro.li/aiProject/claude-j/claude-j-infrastructure/src/main/java/com/claudej/infrastructure/notification/persistence/repository/NotificationRepositoryImpl.java`
- **测试**：`/Users/macro.li/aiProject/claude-j/claude-j-infrastructure/src/test/java/com/claudej/infrastructure/notification/persistence/repository/NotificationRepositoryImplTest.java`
- **验证命令**：`mvn test -pl claude-j-infrastructure -Dtest=NotificationRepositoryImplTest`
- **预期输出**：DO ↔ Domain 映射准确，H2 写入回读一致
- **commit**：`feat(infrastructure): persist notifications`

### 7.1 Infrastructure RabbitMQ consumer
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-infrastructure/src/main/java/com/claudej/infrastructure/notification/mq/OrderCreatedMessageListener.java`
- **测试**：`/Users/macro.li/aiProject/claude-j/claude-j-infrastructure/src/test/java/com/claudej/infrastructure/notification/mq/OrderCreatedMessageListenerTest.java`
- **骨架**：
  ```java
  @Test
  void should_delegate_to_notification_service_when_message_received() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-infrastructure -Dtest=OrderCreatedMessageListenerTest`
- **预期输出**：消费后调用 `NotificationApplicationService`
- **commit**：`feat(infrastructure): consume order created messages`

### 8.1 Adapter Notification 查询接口（可选）
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-adapter/src/main/java/com/claudej/adapter/notification/web/NotificationController.java`
- **测试**：`/Users/macro.li/aiProject/claude-j/claude-j-adapter/src/test/java/com/claudej/adapter/notification/web/NotificationControllerTest.java`
- **验证命令**：`mvn test -pl claude-j-adapter -Dtest=NotificationControllerTest`
- **预期输出**：HTTP 200/404 断言通过
- **commit**：`feat(adapter): add notification query endpoint`

### 9.1 Start RabbitMQ 配置与 docker-compose
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-start/pom.xml`、`/Users/macro.li/aiProject/claude-j/claude-j-start/src/main/resources/application.yml`、`/Users/macro.li/aiProject/claude-j/claude-j-start/src/main/resources/application-dev.yml`、`/Users/macro.li/aiProject/claude-j/claude-j-start/src/main/resources/db/schema.sql`、`/Users/macro.li/aiProject/claude-j/docs/devops/docker-compose.yml`
- **验证命令**：`mvn compile -pl claude-j-start`
- **预期输出**：RabbitMQ 配置绑定通过；compose 含 broker 服务定义
- **commit**：`feat(start): wire rabbitmq runtime`

### 10.1 全链路集成测试
- **文件**：`/Users/macro.li/aiProject/claude-j/claude-j-start/src/test/java/com/claudej/mq/OrderNotificationIntegrationTest.java`
- **验证命令**：`mvn test -pl claude-j-start -Dtest=OrderNotificationIntegrationTest`
- **预期输出**：创建订单后通知记录最终落库，验证最小异步闭环
- **commit**：`test(start): cover order notification mq flow`

### 11.1 文档与交接收口
- **文件**：`/Users/macro.li/aiProject/claude-j/docs/exec-plan/active/028-message-queue/dev-log.md`、`/Users/macro.li/aiProject/claude-j/docs/exec-plan/active/028-message-queue/task-plan.md`、`/Users/macro.li/aiProject/claude-j/docs/exec-plan/active/028-message-queue/handoff.md`
- **验证命令**：`git diff -- /Users/macro.li/aiProject/claude-j/docs/exec-plan/active/028-message-queue`
- **预期输出**：文档与实际实现一致
- **commit**：`docs(docs): record mq delivery evidence`

## 开发完成记录
<!-- dev 完成后填写 -->
- 全量 `mvn clean test`：待构建阶段填写
- 架构合规检查：待构建阶段填写
- 通知 @qa 时间：待构建阶段填写

## QA 验收记录
<!-- qa 验收后填写 -->
- 全量测试（含集成测试）：待 QA 填写
- 代码审查结果：待 QA 填写
- 代码风格检查：待 QA 填写
- 问题清单：详见 `test-report.md`
- **最终状态**：待 QA 填写
