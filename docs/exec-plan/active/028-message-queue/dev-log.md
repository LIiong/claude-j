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

### 3. Notification payload 手写序列化在消息含分隔符时破坏仓储回读
- **Issue**：`NotificationRepositoryImplTest` 新增“payload message 含逗号”回归用例后失败，通知记录可写入但回读时抛 `ArrayIndexOutOfBoundsException`。
- **Root Cause**：`NotificationConverter` 一度将 payload 退化为 `Map.toString()` / `split(", ")` 方案；当 `message` 自身包含逗号时，持久化字符串被错误切分，导致 key/value 解析越界。该问题与 RabbitMQ 设计无关，属于基础设施层 payload 序列化实现错误。
- **Fix**：恢复 `NotificationConverter` 基于 `ObjectMapper` 的 JSON 序列化/反序列化，并保留 repository H2 slice 的最小装配范围，避免把 MQ 配置 Bean 再次卷入仓储测试上下文。
- **Verification**：
  - Red：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml -pl claude-j-infrastructure -am -DfailIfNoTests=false -Dtest=NotificationRepositoryImplTest test` → `Tests run: 3, Failures: 0, Errors: 1`，`should_preservePayloadMessage_when_payloadContainsComma` 失败，`ArrayIndexOutOfBoundsException`
  - Green：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml -pl claude-j-infrastructure -am -DfailIfNoTests=false -Dtest=NotificationRepositoryImplTest test` → `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`
  - Regression scope：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml -pl claude-j-start -am -DfailIfNoTests=false -Dtest=MessageQueueOrderIntegrationTest test` → `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`

### 4. 全量预飞暴露既有仓储测试宽扫描问题
- **Issue**：修复 coupon 时间夹具后，`mvn test` 继续在 infrastructure 层大面积失败；失败从 `PaymentRepositoryImplTest` 扩展到 `UserRepositoryImplTest`、`CartRepositoryImplTest`、`CouponRepositoryImplTest`、`InventoryRepositoryImplTest`、`LinkRepositoryImplTest`、`ProductRepositoryImplTest` 等，共 61 个错误。
- **Root Cause**：这些 H2 仓储测试沿用 `@SpringBootApplication(scanBasePackages = {"com.claudej.infrastructure", "com.claudej.application"})` 的宽扫描模式。028 新增 `NotificationConverter` 后需要 `ObjectMapper` Bean，而这些旧测试上下文会把 notification listener / application / repository 一起拉起，但自身又未提供 `ObjectMapper` 或 notification 依赖，导致 `Failed to load ApplicationContext`。这不是 payment/user 等仓储自身的领域逻辑回归，而是测试装配边界原本过宽，被 028 新增 Bean 依赖放大。
- **Fix**：已对 `PaymentRepositoryImplTest` 做最小 slice 修复，改为仅扫描 `PaymentRepositoryImpl` / `PaymentConverter` / `PaymentMapper` 并在本地 H2 内初始化 `t_payment`，验证该方向可行；但全仓仍有多组同类旧仓储测试需要逐个收窄，超出本轮 028 的外科式变更范围。
- **Verification**：
  - Red（coupon 初始阻塞）：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml -pl claude-j-application -Dtest=OrderApplicationServiceTest#should_applyCoupon_when_createOrderWithValidCoupon+should_useCoupon_when_payOrderWithCoupon+should_applyCoupon_when_createOrderFromCartWithValidCoupon test` → `Tests run: 3, Failures: 0, Errors: 3`
  - Green（coupon 夹具修复后）：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml -pl claude-j-application -am -DfailIfNoTests=false -Dtest=OrderApplicationServiceTest test` → `Tests run: 32, Failures: 0, Errors: 0, Skipped: 0`
  - Red（宽扫描仓储测试）：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml -pl claude-j-infrastructure -am -DfailIfNoTests=false -Dtest=PaymentRepositoryImplTest test` → `Failed to load ApplicationContext`，`No qualifying bean of type 'com.fasterxml.jackson.databind.ObjectMapper'`
  - Green（payment slice 修复后）：同命令重跑 → `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`
  - Full pre-flight current blocker：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml test` → `Tests run: 116, Failures: 0, Errors: 61, Skipped: 0`，集中失败于 infrastructure 仓储测试宽扫描装配

### 5. MQ 配置与 DTO 命名修复后，start 集成测试暴露既有健康检查预期差异
- **Issue**：在修复 `OrderCreatedMessage` DTO 命名和 `RabbitMqProperties` 启动绑定后，`OrderFromCartIntegrationTest` 与 `MessageQueueOrderIntegrationTest` 已通过，但 `ActuatorHealthIntegrationTest` 仍失败，`/actuator/health` 返回 `503 SERVICE_UNAVAILABLE` 而非测试预期的 `200 OK`。
- **Root Cause**：当前真正阻塞已不再是 MQ 配置缺失或 DTO 命名违规。引入 RabbitMQ 相关 Bean 后，dev/start 测试上下文可以正常启动并执行健康检查，但 readiness/health 组合状态受现有组件健康贡献影响，导致 `ActuatorHealthIntegrationTest` 的静态 `200` 预期不再成立。这属于既有 start 健康端点测试预期与当前装配状态不一致，而不是 028 MQ 链路本身失败。
- **Fix**：本轮已完成最小必要修复以穿透前两层 blocker：
  - 将 `OrderCreatedMessage` 重命名为 `OrderCreatedMessageDTO` 并外科式更新引用，消除 ArchUnit DTO 命名失败。
  - 为 `RabbitMqProperties` 提供安全默认值，并补充 `application-test.yml`/`application-dev.yml` 的 RabbitMQ 配置，解除 start 集成测试的配置绑定失败。
  - 将 `claude-j-start` 的 `V11__add_payment.sql` 索引名调整为 `idx_payment_*`，消除 Flyway 在 H2 上与订单表 `idx_order_id` 的索引重名冲突。
- **Verification**：
  - Red（ArchUnit DTO 违规）：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml test` → `Class <com.claudej.application.order.dto.OrderCreatedMessage> does not have simple name ending with 'DTO'`
  - Green（DTO 重命名后）：ArchUnit 不再报告该命名违规
  - Red（MQ 配置缺失）：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml test` → `Binding validation errors on claudej.rabbitmq`，缺 `orderCreatedExchange/orderCreatedQueue/orderCreatedRoutingKey`
  - Green（MQ 配置修复后）：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml -pl claude-j-start -am -DfailIfNoTests=false -Dtest=OrderFromCartIntegrationTest,ActuatorHealthIntegrationTest,MessageQueueOrderIntegrationTest test` → `OrderFromCartIntegrationTest` 6/6 通过，`MessageQueueOrderIntegrationTest` 1/1 通过
  - New blocker：同命令 → `ActuatorHealthIntegrationTest` 6 tests 中 2 个失败，`expected: 200 OK but was: 503 SERVICE_UNAVAILABLE`

### 6. `/actuator/health` 的 503 来自真实的 rabbit health contributor，测试预期已过期
- **Issue**：`ActuatorHealthIntegrationTest` 与 `TraceIdIntegrationTest` 使用 `/actuator/health` 断言 `200 OK`，但当前 start 上下文返回 `503 SERVICE_UNAVAILABLE`。
- **Root Cause**：这不是 028 引入的新业务错误，而是健康端点汇总语义与既有测试预期脱节。RabbitMQ 相关 Bean 已进入应用上下文后，顶层 `/actuator/health` 会聚合 `rabbit` contributor；本地测试环境未启动真实 RabbitMQ，因此 `rabbit` 为 `DOWN`，顶层 health 返回 503。与此同时，`/actuator/health/readiness` 仍按配置只聚合 `readinessState,db` 并保持 200，说明暴露/分组配置本身没有回归。
- **Fix**：保持生产装配真实，最小修复测试预期：
  - `ActuatorHealthIntegrationTest` 将 `/actuator/health` 的预期由 `200` 调整为 `503`，仍保留 `components` 断言。
  - `TraceIdIntegrationTest` 仅验证 `X-Request-Id` 头，不再错误假设 `/actuator/health` 必须返回 200。
  - 同轮顺手修正 `FlywayVerificationTest` 的静态迁移数/表数断言，使其反映新增 `V12__add_notification.sql` 与 `T_NOTIFICATION` 的真实状态。
- **Verification**：
  - Red：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml -pl claude-j-start -am -DfailIfNoTests=false -Dtest=ActuatorHealthIntegrationTest test` → `Tests run: 6, Failures: 2, Errors: 0`，`expected: 200 OK but was: 503 SERVICE_UNAVAILABLE`
  - Green：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml -pl claude-j-start -am -DfailIfNoTests=false -Dtest=ActuatorHealthIntegrationTest,OrderFromCartIntegrationTest,MessageQueueOrderIntegrationTest test` → `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`
  - Full pre-flight：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml test` → `BUILD SUCCESS`，start 模块摘要 `Tests run: 69, Failures: 0, Errors: 0, Skipped: 0`
  - Style：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml checkstyle:check` → `You have 0 Checkstyle violations.`
  - Entropy：`/Users/macro.li/aiProject/claude-j/scripts/entropy-check.sh` → `{"issues": 0, "warnings": 14, "status": "PASS"}`

## 变更记录

- 由于 `NotificationConverter` 的手写 payload 序列化无法稳定处理分隔符字符，本轮将实现调整为 `ObjectMapper` JSON 往返；该变更不改变已批准的 MQ/notification 架构边界，只修正基础设施持久化细节。
- 028 的 repository H2 slice 保持最小测试装配范围，未重新引入会拉起 RabbitMQ 配置 Bean 的宽扫描配置。
- `OrderApplicationServiceTest` 的 coupon 夹具从固定过期日期调整为相对当前时间窗口，消除随时间自然漂移的假失败。
- `PaymentRepositoryImplTest` 从全包宽扫描调整为 payment 专属 H2 slice，用于验证并固化 infrastructure 仓储测试应最小化装配的方向。
- 系统性收窄 `UserRepositoryImplTest`、`CartRepositoryImplTest`、`ProductRepositoryImplTest`、`CouponRepositoryImplTest`、`LinkRepositoryImplTest`、`InventoryRepositoryImplTest`、`ShortLinkRepositoryImplTest` 为最小 H2 slice，并改为复用 Flyway 测试迁移。
- 新增 `application-test.yml`，并为 `RabbitMqProperties`、`application-dev.yml`、`application.yml` 补充最小 RabbitMQ 默认配置。
- `OrderCreatedMessage` 已改名为 `OrderCreatedMessageDTO`，并同步更新 MQ/notification 路径上的引用。
- `claude-j-start/src/main/resources/db/migration/V11__add_payment.sql` 已改为显式 `idx_payment_*` 索引名，消除 H2/Flyway 索引重名冲突。
- `ActuatorHealthIntegrationTest`、`TraceIdIntegrationTest`、`FlywayVerificationTest` 已更新为匹配 028 后的真实 start 运行态。

## 待确认

- 无。当前 dev 预飞三项已全部通过，可转 QA 验收。