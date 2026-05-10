# 测试用例设计 — 028-message-queue

## 测试范围
验证订单创建后 RabbitMQ 异步通知链路、通知持久化 JSON 载荷、既有 infrastructure repository H2 slice 收敛，以及 start 模块 health 验证预期与当前装配语义一致。

## 测试策略
按测试金字塔分层测试 + 代码审查 + 风格检查。

---

## 0. AC 自动化覆盖矩阵（强制，动笔前先填）

| # | 验收条件（AC） | 自动化层 | 对应测试方法 | 手动备注（若有） |
|---|---|---|---|---|
| AC1 | 订单创建后通过 MQ 桥接触发通知处理，并生成通知记录 | Start（集成） | `MessageQueueOrderIntegrationTest.should_createNotificationRecord_when_orderCreatedEventBridgesToMessageConsumer` | - |
| AC2 | 通知 payload 持久化后可完整回读，包含逗号等分隔符字符 | Infrastructure | `NotificationRepositoryImplTest.should_preservePayloadMessage_when_payloadContainsComma` | - |
| AC3 | MQ consumer 将订单创建消息委派给通知应用服务 | Infrastructure | `OrderCreatedMessageListenerTest.should_delegateToNotificationService_when_messageReceived` | - |
| AC4 | 进程内订单创建事件在事务提交后桥接为 MQ 消息 | Infrastructure | `OrderCreatedEventBridgeListenerTest.should_publishOrderCreatedMessage_when_orderCreatedEventHandled` | - |
| AC5 | start 模块健康检查测试与 Rabbit health contributor 语义一致，不再错误断言 200 | Start（集成） | `ActuatorHealthIntegrationTest.should_return_503_when_overall_health_has_down_contributor` | - |

---

## 一、Domain 层测试场景

### NotificationId / NotificationPayload 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D1 | 合法通知标识创建 | - | `new NotificationId("NOTI-001")` | 创建成功 |
| D2 | 空通知标识 | - | `new NotificationId("")` | 抛业务异常 |
| D3 | 合法 payload 创建 | - | `NotificationPayload.of(...)` | 快照字段完整保留 |
| D4 | payload 非法字段 | 缺失订单/客户/金额/消息任一字段 | `NotificationPayload.of(...)` | 抛业务异常 |

### Notification 聚合根
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D5 | 创建待发送通知 | - | `Notification.createPending(...)` | 初始状态为 `PENDING` |
| D6 | 标记发送成功 | 待发送通知 | `markSent(...)` | 状态变为 `SENT`，记录发送时间 |
| D7 | 标记发送失败 | 待发送通知 | `markFailed()` | 状态变为 `FAILED` |

---

## 二、Application 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| A1 | 成功处理订单创建消息 | 仓储无现有通知 | `handleOrderCreated(message)` | 调用 sender，最终保存 `SENT` 记录 |
| A2 | 幂等跳过重复消息 | 仓储已存在同订单同渠道通知 | `handleOrderCreated(message)` | 不重复发送，不重复保存 |
| A3 | 发送失败记录状态 | sender 抛异常 | `handleOrderCreated(message)` | 保存 `FAILED` 记录并重新抛出异常 |

---

## 三、Infrastructure 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| I1 | 保存并查询通知记录 | H2 初始化 `t_notification` | `save -> findByOrderIdAndChannel` | 返回匹配记录 |
| I2 | payload JSON 往返 | payload 含金额与消息文本 | 保存并回读 | DO ↔ Domain 字段完整还原 |
| I3 | payload 含逗号 | message 包含逗号 | 保存并回读 | 消息内容不被错误拆分 |
| I4 | 订单事件桥接发布 | mock publisher + assembler | `onOrderCreated(event)` | 发布 `OrderCreatedMessageDTO` |
| I5 | 消费者委派应用服务 | mock `NotificationApplicationService` | `onOrderCreated(message)` | 调用 `handleOrderCreated` |
| I6 | 既有 repository H2 slice 收敛不回归 | 最小扫描仓储测试上下文 | 运行既有 repository tests | 不再被 MQ Bean 装配污染 |

---

## 四、Adapter 层测试场景

> 本任务不涉及 Adapter 层新增接口，已按模板说明省略

---

## 五、集成测试场景（全链路）

| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E1 | 核心流程正向 | 创建订单 | 订单创建后生成 `SENT` 通知记录 |
| E2 | 健康端点聚合状态 | 请求 `/actuator/health` | 顶层 health 因 `rabbit` contributor 为 `DOWN` 返回 503 |
| E3 | 就绪探针不受 rabbit contributor 影响 | 请求 `/actuator/health/readiness` | readiness 仍返回 200 |

---

## 六、代码审查检查项

- [x] 依赖方向正确（adapter → application → domain ← infrastructure）
- [x] domain 模块无 Spring/框架 import
- [x] 聚合根封装业务不变量（非贫血模型）
- [x] 值对象不可变，equals/hashCode 正确
- [x] Repository 接口在 domain，实现在 infrastructure
- [x] 对象转换链正确：DO ↔ Domain ↔ DTO
- [x] Controller 无业务逻辑（本任务未新增 controller）
- [x] 异常通过现有统一处理链路暴露（本任务未改 adapter）

## 七、代码风格检查项

- [x] Java 8 兼容（无 var、records、text blocks、List.of）
- [x] 聚合根用 `@Getter`，值对象用不可变语义
- [x] DO 用 `@Data + @TableName`，DTO 用 `@Data`
- [x] 命名规范：`OrderCreatedMessageDTO`、`NotificationRepositoryImpl` 等符合约定
- [x] 包结构符合 `com.claudej.{layer}.{aggregate}.{sublayer}`
- [x] 测试命名遵循 `should_xxx_when_yyy`
