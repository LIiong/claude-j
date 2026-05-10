# 测试用例设计 — 030-full-suite-recovery

## 测试范围
验证 030 仅通过恢复 start 模块 H2 测试 profile 的 Flyway 路径来修复 full suite，同时不回退 029 已验收的 MySQL Testcontainers/Flyway 保真路径。

## 测试策略
按测试金字塔分层测试 + 代码审查 + 风格检查。

---

## 0. AC 自动化覆盖矩阵（强制，动笔前先填）

| # | 验收条件（AC） | 自动化层 | 对应测试方法 | 手动备注（若有） |
|---|---|---|---|---|
| AC1 | failure classification 形成可执行失败清单与分层归因，并在任务过程中持续维护 | 文档证据 + Start canary | `dev-log.md` 条目 2/3/4；`CartIntegrationTest`、`OrderFromCartIntegrationTest`、`MessageQueueOrderIntegrationTest` 作为 root-cause canary | 无 |
| AC2 | 029 回归守护路径保持通过：真实 MySQL Testcontainers 仓储/Flyway 校验不回退 | Infrastructure / Start | `OrderRepositoryImplTest`、`PaymentRepositoryImplTest`、`NotificationRepositoryImplTest`、`FlywayVerificationTest` | 无 |
| AC3 | 对纳入 030 范围的失败完成最小修复，并具备 Red-Green 证据 | 文档证据 + Start canary | `dev-log.md` 条目 2（Red/Green 记录）；`CartIntegrationTest`、`OrderFromCartIntegrationTest`、`MessageQueueOrderIntegrationTest` | 无 |
| AC4 | 全量 `mvn test` 通过 | Full reactor | `DOCKER_HOST=unix:///Users/macro.li/.docker/run/docker.sock mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml -Dapi.version=1.41 test` | 无 |
| AC5 | `mvn checkstyle:check` 通过 | Build verification | `mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml checkstyle:check` | 无 |
| AC6 | `./scripts/entropy-check.sh` 通过 | Architecture guard | `./scripts/entropy-check.sh` | 无 |

---

## 一、Domain 层测试场景

> 本任务不涉及 Domain 层生产代码变更，已按模板说明省略新增 Domain 用例设计；验收依赖 full reactor `mvn test` 回归既有 Domain 单测。

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D1 | 既有 Domain 回归不被 030 配置修复打坏 | start 测试 profile 切回 H2 shadow migration | 执行 full reactor `mvn test` | Domain 模块测试继续全绿，无新增架构/语义回归 |

---

## 二、Application 层测试场景

> 本任务不涉及 Application 层生产代码变更，已按模板说明省略新增 Application 用例设计；验收依赖 full reactor `mvn test` 回归既有 Application 单测。

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| A1 | 既有 Application 编排不被 030 配置修复打坏 | start 测试 profile 切回 H2 shadow migration | 执行 full reactor `mvn test` | Application 模块测试继续全绿 |

---

## 三、Infrastructure 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| I1 | 029 仓储 MySQL path 守护保持通过 | Docker / Testcontainers 可用 | 执行 full reactor `mvn test` 并抽查 `OrderRepositoryImplTest` | 仓储测试通过，未回退到 H2 兜底 |
| I2 | payment / notification 仓储守护保持通过 | Docker / Testcontainers 可用 | 执行 full reactor `mvn test` 并抽查 `PaymentRepositoryImplTest`、`NotificationRepositoryImplTest` | 真实 MySQL 容器启动并测试通过 |
| I3 | 不因 start H2 profile 修复破坏基础设施装配 | full reactor 编排 | 执行 full reactor `mvn test` | infrastructure 模块整体成功 |

---

## 四、Adapter 层测试场景

> 本任务不涉及 Adapter 层生产代码变更，已按模板说明省略新增 Adapter 用例设计；验收依赖 full reactor `mvn test` 回归既有 Web/API 测试。

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| W1 | 既有 Adapter 契约不被 030 配置修复打坏 | full reactor 编排 | 执行 full reactor `mvn test` | adapter 模块测试继续全绿 |

---

## 五、集成测试场景（全链路）

| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E1 | start H2 profile 使用 shadow migration 启动成功 | 读取 `application-test.yml` 并执行 full reactor `mvn test` | start 集成测试通过，不再触发 `V10__add_inventory.sql` 重复索引失败 |
| E2 | 029 Flyway MySQL verification 保持真实 MySQL 路径 | 执行 full reactor `mvn test` 并抽查 `FlywayVerificationTest` 输出 | `FlywayVerificationTest` 通过，保留 MySQL Testcontainers 校验 |
| E3 | full suite recovery 结果闭环 | 执行 `mvn test`、`mvn checkstyle:check`、`./scripts/entropy-check.sh` | 三项验证全部通过，变更范围仍局限于 start test profile 修复 |

---

## 六、代码审查检查项

- [ ] 依赖方向正确（adapter → application → domain ← infrastructure）
- [ ] domain 模块无 Spring/框架 import
- [ ] 聚合根封装业务不变量（非贫血模型）
- [ ] 值对象不可变，equals/hashCode 正确
- [ ] Repository 接口在 domain，实现在 infrastructure
- [ ] 对象转换链正确：DO ↔ Domain ↔ DTO ↔ Request/Response
- [ ] Controller 无业务逻辑
- [ ] 异常通过 GlobalExceptionHandler 统一处理
- [ ] 030 变更未回退 029 MySQL Testcontainers/Flyway 路径
- [ ] `application-test.yml` 仅恢复 H2 test profile Flyway 路径，未扩散 unrelated 改动

## 七、代码风格检查项

- [ ] Java 8 兼容（无 var、records、text blocks、List.of）
- [ ] 聚合根用 @Getter，值对象用 @Getter + @EqualsAndHashCode + @ToString
- [ ] DO 用 @Data + @TableName，DTO 用 @Data
- [ ] 命名规范：XxxDO, XxxDTO, XxxMapper, XxxRepository, XxxRepositoryImpl
- [ ] 包结构符合 com.claudej.{layer}.{aggregate}.{sublayer}
- [ ] 测试命名 should_xxx_when_xxx
- [ ] YAML 配置变更保持窄范围，仅修改测试 profile 所需键
