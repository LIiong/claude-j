# 需求拆分设计 — 028-message-queue

## 需求描述
为当前 claude-j 电商示范项目补齐 P1-必做能力 D1：消息队列接入。在保持现有 DDD + 六边形架构边界不变的前提下，为订单创建场景增加“订单创建后异步发送通知事件”的最小可运行示例，并补齐本地 docker-compose broker、自动化测试与交付文档。

本次 Spec 阶段需要先对 RabbitMQ、Kafka、RocketMQ 做本地演示友好性、Spring Boot 2.7 / Java 8 兼容性、docker-compose 落地成本、消息语义适配度的比较，并给出明确选型结论。

## 领域分析

### 聚合根: Order
- `orderId` (`OrderId`) — 订单唯一标识，也是通知消息的业务主键
- `customerId` (`CustomerId`) — 通知接收侧关联的业务标识
- `items` (`List<OrderItem>`) — 订单明细，消息载体中仅透出通知所需的最小快照
- `status` (`OrderStatus`) — 本任务主线只覆盖 CREATED 场景，不新增订单状态
- `totalAmount` (`Money`) — 通知文案/载荷展示所需金额快照

### 聚合根: Notification（新增轻量示例聚合）
- `notificationId` (`NotificationId`) — 通知记录唯一标识
- `orderId` (`OrderId`) — 来源订单标识，用于幂等去重与链路追踪
- `channel` (`NotificationChannel`) — 示例阶段仅 EMAIL / INTERNAL 两类枚举中的一种
- `status` (`NotificationStatus`) — PENDING / SENT / FAILED，表达消费处理结果
- `payload` (`NotificationPayload`) — 订单创建通知所需最小快照
- `sentAt` (`LocalDateTime`) — 成功发送时间

说明：Notification 聚合仅承担“消费订单创建消息并形成通知处理示例”的职责，不扩展真实邮件/短信下发能力，保持教学项目中的最小闭环。

### 值对象
- **NotificationId**：不可变，非空字符串，封装通知主键
- **NotificationChannel**：不可变枚举，限定通知渠道，Spec 阶段默认 INTERNAL 以避免引入外部邮件网关
- **NotificationStatus**：不可变枚举，限定通知生命周期
- **OrderCreatedMessage**：不可变消息载体，封装 `eventId`、`orderId`、`customerId`、`totalAmount`、`occurredOn` 等跨进程传输字段；属于 application 层 DTO，不进入 domain
- **NotificationPayload**：不可变值对象，封装通知消费侧真正需要的订单快照，避免直接依赖 MQ 原始报文

### 领域服务（如有）
- **NotificationDomainService**（可选，待实现阶段确认）
  - 职责：集中封装“同一订单是否允许重复生成通知记录”等幂等规则
  - 说明：若 Notification 聚合根自身即可完整承载不变量，则不新增该领域服务，遵循简洁优先原则

### 端口接口
- **OrderMessagePublisher**（application 定义端口，infrastructure 实现）
  - `publishOrderCreated(OrderCreatedMessage message)`
  - 责任：将应用层已组装的订单创建消息发送到 MQ，不暴露具体中间件 SDK 给 application
- **NotificationRepository**（domain 端口）
  - `save(Notification notification)`
  - `findByOrderId(OrderId orderId)`
  - 责任：保存/查询通知消费结果，支撑幂等与示例查询
- **NotificationSender**（domain 或 application 侧端口，待实现阶段确认）
  - `send(Notification notification)`
  - 责任：抽象“通知已发送”动作；本任务最小实现可为日志/内存记录，不引入真实第三方

## 关键算法/技术方案

### 一、消息队列选型比较

| 方案 | 优点 | 缺点 | 与本项目匹配度 |
|---|---|---|---|
| RabbitMQ | Spring AMQP 成熟；单 broker docker-compose 最轻；队列/交换机模型直观；适合订单创建后异步通知这种命令式/工作队列场景；本地资源占用低 | 吞吐和日志回放能力不如 Kafka；不是主打流式处理 | 最高 |
| Kafka | 生态成熟；擅长高吞吐事件流、消息回放、分区扩展 | 本地演示至少要 broker + KRaft/ZK 配置；概念较重；对当前单体教学项目偏重 | 中 |
| RocketMQ | 国内生态常见；事务消息能力强；与订单类场景贴近 | 本地 docker-compose 需要 nameserver + broker；Spring Boot 2.7 + Java 8 可用但示例和运维复杂度高于 RabbitMQ | 中 |

### 二、选型结论
选择 **RabbitMQ** 作为 D1 实施方案。

**取舍理由**：
1. **本地演示成本最低**：单容器即可在 `docs/devops/docker-compose.yml` 中落地，便于 clone 后直接启动。
2. **与主线场景匹配**：订单创建后异步通知属于典型工作队列/事件分发，不需要 Kafka 的回放能力，也不需要 RocketMQ 的更重事务能力。
3. **与现有架构兼容度高**：application 只定义 `OrderMessagePublisher` 端口，infrastructure 用 Spring AMQP 适配即可，不破坏现有 `adapter -> application -> domain <- infrastructure` 方向。
4. **便于演示最小闭环**：可用 direct exchange + single queue 实现“订单创建 → 发布消息 → 通知消费者落库/打日志”的可观测链路。

### 三、架构落点设计

#### domain
- 保留现有 `OrderCreatedEvent` 作为进程内领域事件，不直接绑定 MQ。
- 新增 `notification` 聚合，封装通知消费结果与幂等规则。
- `NotificationRepository` 位于 domain，返回/保存领域对象。

#### application
- 在 `order` 聚合下新增消息端口 `OrderMessagePublisher`。
- 由 `OrderApplicationService#createOrder` / `createOrderFromCart` 在保存订单成功后继续发布现有 `OrderCreatedEvent`；由 infrastructure 中的进程内监听器桥接为 MQ 消息。
- 新增 `OrderCreatedMessageAssembler`（或等价转换器）将 `OrderCreatedEvent` 转成 MQ 消息载体 `OrderCreatedMessage`。
- 在 `notification` 聚合下新增 `NotificationApplicationService`，由 MQ consumer 调用以完成通知记录创建/发送/更新状态。

#### infrastructure
- 新增 `order/mq/` 适配目录，包含：
  - RabbitMQ 配置属性类
  - `RabbitMqOrderMessagePublisher` 实现 `OrderMessagePublisher`
  - MQ message converter / queue / exchange / binding 配置
  - `OrderCreatedEventBridgeListener`：监听现有 Spring 领域事件并调用 publisher
- 新增 `notification/mq/OrderCreatedMessageListener` 作为 RabbitMQ consumer，消费后调用 `NotificationApplicationService`
- 新增 `notification/persistence/` 实现 `NotificationRepository`，保存通知消费结果到 `t_notification`
- 消息序列化采用 JSON；载荷字段保持最小必要集合，避免直接传整个领域对象

#### start
- 在 `claude-j-start` 增加 RabbitMQ starter 依赖装配入口所需配置
- 在 `application*.yml` / `application-dev.yml` 增加 MQ 配置前缀（host、port、username、password、exchange、queue、routing-key）
- 在 `docs/devops/docker-compose.yml` 新增 `rabbitmq` 服务（带 management 端口）

### 四、事件桥接策略
采用 **“进程内领域事件 + infrastructure 桥接到 MQ”** 两段式：
1. `OrderApplicationService` 继续发布 `OrderCreatedEvent` 到现有 `DomainEventPublisher`
2. infrastructure 新增桥接监听器，在事务提交后接收 `OrderCreatedEvent`
3. 监听器将事件映射为 `OrderCreatedMessage` 并调用 `OrderMessagePublisher`
4. RabbitMQ consumer 收到消息后调用 `NotificationApplicationService`

这样保持：
- domain/application 不感知 MQ SDK
- 现有 A11 的进程内事件消费（如库存预占）可继续存在，不被 MQ 改造波及
- D1 只新增异步通知链路，不破坏既有跨聚合同步/异步行为

### 五、消息可靠性边界
本任务作为 D1 最小闭环，可靠性策略限定为：
- 生产端：事务提交后再发消息，避免订单回滚后仍发通知
- 消费端：基于 `orderId + channel` 唯一约束与 `NotificationRepository` 查询做幂等保护，重复消息不重复创建 SENT 记录
- 失败处理：消费异常时记录 FAILED 状态，并由后续独立任务定义重试、死信或人工补偿策略
- 已知边界：事务提交成功但 RabbitMQ publish 失败时，D1 接受“订单已创建、消息未送达”的窗口风险；该可靠投递缺口由 D2 的 Transactional Outbox 关闭

**明确不在本任务实现的内容**：
- Transactional Outbox（留给 D2）
- 分布式事务 / 事务消息
- 死信队列重放控制台
- 真实短信/邮件第三方集成

### 六、测试策略
遵循 TDD 与分层测试：
1. **Domain**：`Notification` 聚合与值对象测试，覆盖幂等/状态流转/非法参数
2. **Application**：
   - `NotificationApplicationServiceTest`：Mockito mock `NotificationRepository` / `NotificationSender`
   - `OrderCreatedEventBridgeListenerTest` 或 message assembler 单测，验证事件到消息映射
3. **Infrastructure**：
   - `NotificationRepositoryImplTest`：H2 验证 DO ↔ Domain 往返
   - RabbitMQ publisher / listener 集成测试：优先使用 Spring AMQP 测试支持，必要时用 `@SpringBootTest` + mock listener container
4. **Adapter**：本任务主线不新增 REST API，可不新增 adapter 端点；若为演示需要补查询通知结果接口，再补 `@WebMvcTest`
5. **Start / Integration**：新增 1 个 `@SpringBootTest` 集成测试，验证“创建订单 -> 发布并消费消息 -> 生成通知记录”最小闭环，控制在单任务 ≤ 3 个全链路集成测试内

### 七、假设与待确认
- **假设 1**：通知示例允许以内置日志/数据库记录代替真实邮件发送，只要能证明消息被成功消费并完成通知处理即可。
- **假设 2**：本任务不要求把现有库存/优惠券事件链路全部迁移到 MQ，只对“订单创建后通知”做新增异步示例。
- **假设 3**：本地演示以 `docs/devops/docker-compose.yml` 为统一入口，而非仓库根目录新增第二份 compose 文件。
- **架构确认 1**：接受新增 `notification` 轻量聚合作为消费侧落点，但其边界限定为“通知处理结果与幂等记录”，不得在 D1 扩展真实多渠道编排、模板管理或用户触达偏好等新子域能力。
- **架构确认 2**：D1 仅做 happy path + 基础失败记录，不预留死信队列/重试配置骨架；若后续引入重试策略、死信队列、人工补偿或投递观测面板，应在 D2 通过独立设计与 ADR 明确。
- **架构确认 3**：Build 阶段默认不新增 `notification` 查询 API；若仅为验证异步闭环，可通过 repository 集成测试与 start 模块单个全链路测试举证，避免为演示目的扩张对外契约。

## API 设计

当前主线场景不要求新增对外 API，沿用既有订单创建接口：

| 方法 | 路径 | 描述 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| POST | /api/v1/orders | 创建订单后异步触发 MQ 通知 | 现有 `CreateOrderRequest` | 现有 `ApiResult<OrderResponse>` |
| POST | /api/v1/orders/from-cart | 从购物车创建订单后异步触发 MQ 通知 | 现有 `CreateOrderFromCartRequest` | 现有 `ApiResult<OrderResponse>` |

如 Build 阶段为了演示通知结果需要新增查询接口，可补充：

| 方法 | 路径 | 描述 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| GET | /api/v1/notifications/orders/{orderId} | 查询订单通知处理结果（仅在需求新增对外观测要求时启用） | — | `{ "success": true, "data": { ... } }` |

## 数据库设计（如有）
```sql
CREATE TABLE IF NOT EXISTS t_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    payload_json TEXT NOT NULL,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_notification_order_channel (order_id, channel)
);
```

设计说明：
- 以 `(order_id, channel)` 做唯一约束，支撑消费幂等
- `payload_json` 保存最小通知快照，便于本地演示和问题追踪
- 暂不新增 outbox 表；若后续做 D2，再单独引入 `t_outbox`

## 架构评审

**评审人**：@architect
**日期**：2026-04-30
**结论**：✅ 通过

### 评审检查项（15 维四类）

**架构合规（7 项）**
- [x] 聚合根边界合理（遵循事务一致性原则）
- [x] 值对象识别充分（金额、标识符等应为 VO）
- [x] Repository 端口粒度合适（方法不多不少）
- [x] 与已有聚合无循环依赖
- [x] DDL 设计与领域模型一致（字段映射、索引合理）
- [x] API 设计符合 RESTful 规范
- [x] 对象转换链正确（DO ↔ Domain ↔ DTO ↔ Request/Response）

**需求质量（3 项）**
- [x] 需求无歧义：核心名词、流程、异常分支均有明确定义
- [x] 验收条件可验证：每条 AC 可转化为 `should_xxx_when_yyy` 测试用例
- [x] 业务规则完备：状态机/不变量/边界值在需求中已列明

**计划可执行性（2 项）**
- [x] task-plan 粒度合格：按层任务已分解到原子级（10–15 分钟/步），每步含文件路径 + 验证命令 + 预期输出（详见 `docs/exec-plan/templates/task-plan.template.md` 原子任务章节）
- [x] 依赖顺序正确：domain → application → infrastructure → adapter → start 自下而上，层间依赖无倒置

**可测性保障（3 项 — 010 复盘后新增）**
- [x] **AC 自动化全覆盖**：`test-case-design.md` 的「AC 自动化覆盖矩阵」每条 AC 都有对应自动化测试方法；任一标「手动」但无替代自动化测试 → **打回**
- [x] **可测的注入方式**：若引入新 Spring Bean，使用构造函数注入而非字段注入（避免测试反射）；详见 `java-dev.md` 依赖注入规则
- [x] **配置校验方式合规**：若涉及敏感/跨环境配置校验，使用 `@ConfigurationProperties + @Validated`，不得用 `ApplicationRunner`/`@PostConstruct`；详见 ADR-005

**心智原则（Karpathy — 动手前自检）**
- [x] **简洁性**：需求未要求的抽象/配置/工厂已移除；任何单一实现的 `XxxStrategy`/`XxxFactory` 需说明存在理由
- [x] **外科性**：设计仅改动任务直接相关的文件；若涉及跨聚合大改，在评审意见说明理由
- [x] **假设显性**：需求里含糊的字段/边界/异常，requirement-design 已在「假设与待确认」列出

> 完整原则与反模式：`.claude/rules/karpathy-guidelines.md`

### 评审意见
1. **RabbitMQ 作为 D1 选型合理，且与 ADR-006 不冲突。** ADR-006 只是为 025 任务选择了进程内 `@TransactionalEventListener` 作为当时最轻的跨聚合协作方案，并明确写明未来可平滑迁移到 MQ。028 设计继续保留进程内领域事件作为 application/domain 边界，再由 infrastructure 做 MQ 桥接，符合“端口在内、适配在外”的六边形约束，也避免让现有库存监听链路一次性迁移到分布式消息。
2. **`notification` 聚合作为消费侧示例落点合适，但边界必须收窄。** 我已在正文确认：D1 中它只负责“通知处理结果 + 幂等记录”，不扩展多渠道策略、模板编排、用户偏好等真实通知子域。相比退化成 infrastructure 级日志消费者，这样更能保持 DDD 示例完整，同时不会与现有 `order`/`user`/`auth` 聚合形成循环依赖。
3. **D1 只做“事务后发布 + 消费幂等 + 失败记录”，把 Transactional Outbox 留到 D2 是合理切分，但必须显式承认可靠性缺口。** 我已补充 D1 的已知边界：事务提交成功但 RabbitMQ publish 失败时，仍存在“订单已创建、消息未送达”的窗口风险。这一缺口不应被“事务后发布”表述掩盖，后续 D2 应用 Outbox 关闭该窗口；在 D1 范围内接受它，符合简洁优先和阶段性交付。
4. **Build 阶段不建议预留 `notification` 查询 API。** 当前需求目标是演示异步链路接入，而不是新增用户可见通知查询能力。已有订单创建 API + repository/H2 测试 + start 模块单个全链路测试，足以证明闭环；如果为了“看得见”而新增 controller，会把适配器契约、DTO、测试、鉴权与文档一起拉进范围，违反外科式变更。仅当后续需求明确要求对外观测时，再单开任务补查询接口。
5. **task-plan 可执行性通过，但第 8 项应按“默认跳过”理解。** 我无法按角色边界修改 `task-plan.md`，因此仅在此注明：`/Users/macro.li/aiProject/claude-j/docs/exec-plan/active/028-message-queue/task-plan.md` 中“8.1 Adapter Notification 查询接口”在 Build 时默认不做，除非需求新增对外观测要求。
6. **架构基线已真实运行通过。** 执行命令：`/Users/macro.li/aiProject/claude-j/scripts/entropy-check.sh`；退出码 `0`。结果：`0 FAIL / 13 WARN / status PASS`。WARN 为仓库既有测试缺失、部分 ADR 状态节提示和归档目录历史提示，不构成 028 当前设计阻塞。

### 需要新增的 ADR
本轮**不新增 ADR**。原因：
- MQ 选型是 028 D1 的任务级技术选择，目前仅为“订单创建后通知”提供最小演示闭环，尚未上升为全项目统一消息基础设施决策；
- 现有 ADR-006 已允许未来从进程内事件平滑迁移到 MQ，本次设计仍处于其兼容路径内；
- 待 D2 明确 Transactional Outbox、重试/死信、统一消息模型或更多聚合接入时，再新增“消息基础设施策略” ADR 更合适。
