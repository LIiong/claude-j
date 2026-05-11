# 测试用例设计 — 029-testcontainers

## 测试范围
验证 `claude-j-infrastructure` 仓储集成测试切换到共享 MySQL 8 Testcontainers 引导，以及 `claude-j-start` 的 `FlywayVerificationTest` 切换到真实 MySQL 迁移验证；同时审查改动是否保持在该窄任务边界内。

## 测试策略
按测试金字塔分层测试 + 代码审查 + 风格检查。

---

## 0. AC 自动化覆盖矩阵（强制，动笔前先填）

| # | 验收条件（AC） | 自动化层 | 对应测试方法 | 手动备注（若有） |
|---|---|---|---|---|
| AC1 | infrastructure 仓储测试通过共享 MySQL 8 Testcontainers + Flyway 建表运行，不再依赖 H2 datasource 覆盖或手工 DDL | Infrastructure | `OrderRepositoryImplTest.should_saveNewOrder_when_orderHasNoId`、`PaymentRepositoryImplTest.should_saveAndFindPayment_when_newPaymentCreated`、`NotificationRepositoryImplTest.should_saveNotification_when_notificationIsNew` | - |
| AC2 | `PaymentRepositoryImplTest` 与 `NotificationRepositoryImplTest` 在真实 MySQL 上通过 Flyway 迁移完成表结构初始化 | Infrastructure | `PaymentRepositoryImplTest.should_updatePayment_when_paymentStatusChanged`、`NotificationRepositoryImplTest.should_findNotificationByOrderIdAndChannel_when_notificationExists` | - |
| AC3 | `FlywayVerificationTest` 在 MySQL 8 Testcontainers 下验证 Flyway 迁移记录与目标表清单 | Start（集成） | `FlywayVerificationTest.should_record_12_migrations_when_flyway_migrates`、`FlywayVerificationTest.should_create_15_tables_when_migrations_complete` | - |
| AC4 | 改动范围保持在仓储测试引导、测试迁移脚本兼容性、`FlywayVerificationTest`，不扩散修复其他 start 业务集成测试 | 代码审查 | `git diff --name-only` + 目标文件审查 | 自动化无法直接判定“过度修复”意图，采用 diff 审查作为证据 |

---

## 一、Domain 层测试场景

> 本任务不涉及 Domain 层新增或修改，已按模板说明省略

---

## 二、Application 层测试场景

> 本任务不涉及 Application 层新增或修改，已按模板说明省略

---

## 三、Infrastructure 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| I1 | 订单仓储测试使用共享 MySQL 容器 | Docker 可用，Testcontainers 可连通 Docker Desktop | 运行 `OrderRepositoryImplTest` | 9 个用例全部通过，Repository save/find/exists/update 行为保持正确 |
| I2 | 支付仓储测试不再依赖手工 DDL | Docker 可用，Flyway 迁移可在 MySQL 执行 | 运行 `PaymentRepositoryImplTest` | 8 个用例全部通过，状态更新与索引相关查询正常 |
| I3 | 通知仓储测试不再依赖手工 DDL | Docker 可用，通知迁移脚本可在测试资源下执行 | 运行 `NotificationRepositoryImplTest` | 3 个用例全部通过，payload JSON 往返与唯一键约束对应查询正常 |
| I4 | 共享测试配置由容器动态注入 datasource | `application.yml` 中 datasource 留空 | 运行基础设施定向测试 | Spring 上下文从 `@DynamicPropertySource` 获得连接信息，无 H2 残留 |

---

## 四、Adapter 层测试场景

> 本任务不涉及 Adapter 层新增或修改，已按模板说明省略

---

## 五、集成测试场景（全链路）

| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E1 | Flyway 迁移历史在 MySQL 中完整落库 | 运行 `FlywayVerificationTest.should_record_12_migrations_when_flyway_migrates` | `flyway_schema_history` 中 12 条版本记录全部成功 |
| E2 | Flyway 迁移后目标业务表齐备 | 运行 `FlywayVerificationTest.should_create_15_tables_when_migrations_complete` | 当前 schema 下存在 15 张 `t_` 业务表 |
| E3 | 全量回归确认非任务内失败边界 | 运行全量 `mvn test` | 若失败，应定位是否来自既有 start 非本任务集成测试 |

---

## 六、代码审查检查项

- [x] 依赖方向正确（adapter → application → domain ← infrastructure）
- [x] domain 模块无 Spring/框架 import
- [x] 聚合根封装业务不变量（本任务未改 domain，未引入贫血模型风险）
- [x] 值对象不可变，equals/hashCode 正确（本任务未改值对象）
- [x] Repository 接口在 domain，实现在 infrastructure
- [x] 对象转换链正确：DO ↔ Domain ↔ DTO ↔ Request/Response（本任务未改转换链）
- [x] Controller 无业务逻辑（本任务未改 Controller）
- [x] 异常通过 GlobalExceptionHandler 统一处理（本任务未改 Web 异常流）

## 七、代码风格检查项

- [x] Java 8 兼容（无 var、records、text blocks、List.of）
- [x] 聚合根用 @Getter，值对象用 @Getter + @EqualsAndHashCode + @ToString（本任务未改领域模型）
- [x] DO 用 @Data + @TableName，DTO 用 @Data（本任务未改 DO/DTO）
- [x] 命名规范：XxxDO, XxxDTO, XxxMapper, XxxRepository, XxxRepositoryImpl
- [x] 包结构符合 com.claudej.{layer}.{aggregate}.{sublayer}
- [x] 测试命名 should_xxx_when_xxx（新旧测试类中目标范围用例命名符合规范）
