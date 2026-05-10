# 需求拆分设计 — 030-full-suite-recovery

## 需求描述
在 029-testcontainers 已被 QA 验收通过的前提下，新增后续任务 030-full-suite-recovery，目标是恢复全量 `mvn test`，同时保留 029 已完成的 MySQL 8 Testcontainers 仓储测试与 Flyway 迁移保真度改进，不回退到 H2 兜底，也不扩散为与当前失败根因无关的大范围重构。

本任务以“失败归类 → 定向修复 → 全量回归”为主线，先识别 029 之后全量测试仍失败的真实清单与分层归因，再仅对直接阻断 full suite 的代码、测试装配或配置做外科式修复，最终以全量 `mvn test`、`mvn checkstyle:check`、`./scripts/entropy-check.sh` 全部通过作为验收目标。

## 领域分析

### 聚合根: 无新增聚合
本任务属于测试套件恢复与装配修复，不新增业务聚合根，也不主动扩展既有领域行为。

现有受影响上下文主要体现在测试覆盖的既有聚合与集成流：
- cart / coupon / order / payment / notification / inventory / product / user / auth / message queue 相关既有测试路径
- start 模块中的 Spring Boot 集成装配、Flyway 迁移与测试环境配置

### 值对象
- 无新增值对象。
- 若修复过程中暴露真实业务不变量缺失，应在对应聚合任务中另案处理；030 不以业务建模扩展为目标。

### 领域服务（如有）
- 当前 Spec 不预设新增领域服务。
- 如失败根因落在 domain 规则本身，只允许围绕导致现有失败的最小必要修改，不借机抽象新服务层。

### 端口接口
- 当前 Spec 不预设新增 Repository / Domain Service 端口。
- 若失败来自测试装配或基础设施实现偏差，优先修正现有测试支撑、配置、Repository 实现或 start 装配，而不是引入新端口。

## 关键算法/技术方案

### 方案主线
1. 先执行一次失败归类 pass，收集全量 `mvn test` 的失败测试类、首个异常、所在模块与层级。
2. 将失败划分为两类：
   - 029 回归风险：与 MySQL Testcontainers、Flyway 迁移、仓储测试基座直接相关
   - 029 之后暴露的独立既有失败：如 adapter/start 集成装配、测试上下文配置、消息流集成等
3. 以“保住 029、收敛 full suite”为边界逐类处理：
   - 对 029 路径先建立回归守护，确保 MySQL 仓储/Flyway 路径持续为绿
   - 对 full suite 失败按根因最小修复，不做 broad refactor，不顺手修 unrelated smell
4. 在每个修复单元中坚持 TDD：先写或调整能稳定复现失败的测试，看 Red，再做最小实现转 Green。
5. 最后执行 end-state verification，确认全量测试、checkstyle、entropy-check 全过。

### 推荐方案与边界
采用用户给定边界作为推荐方案：
- 保留 029 已验证通过的 MySQL 8 Testcontainers 路径，不撤销仓储测试与 `FlywayVerificationTest` 的真实 MySQL 校验。
- 优先做 failure classification，避免在未识别完整失败版图时盲目改代码。
- 对 029 的 MySQL path 增加回归守护，避免“修 full suite 时重新打坏 029”。
- 如果全量失败中存在与当前恢复主线无直接关系、且需要独立需求/设计讨论的 surviving unrelated failures，应再次拆分新任务，不折叠进 030。

### 备选方案与取舍
- 方案 A：回退 029 的 Testcontainers 改动，恢复 H2 以快速把 `mvn test` 拉绿。
  - 不采用：违背本任务前提，也会丢失 029 已通过的真实 MySQL 迁移保真度。
- 方案 B：一次性统一重构 start 集成测试装配与所有失败测试公共基座。
  - 不优先：扩散面过大，容易违反外科式变更原则。
- 方案 C：按失败聚类逐个建立最小复现与修复，并持续跑 029 回归守护。
  - 采用：与当前目标、边界和 TDD/Goal-Driven 原则一致。

### 假设与待确认
- 假设 029 的定向目标已稳定：`OrderRepositoryImplTest`、`PaymentRepositoryImplTest`、`NotificationRepositoryImplTest`、`FlywayVerificationTest` 继续作为 030 的回归保护样本。
- 假设当前 full suite 失败主要集中在测试装配、集成配置或既有用例对运行环境的依赖，而不是 029 引入的持久化层回归；该假设必须通过 failure classification 先验证。
- 假设 030 允许修改测试代码、测试配置、必要的最小生产代码/装配代码，但不允许因“顺手优化”做跨聚合大改。
- 待 architect 评审确认：若失败清单中出现多个彼此独立的子问题，是否在 030 内仅处理主阻断簇，剩余问题另拆任务。

## API 设计
本任务不新增对外 API。

若修复触及既有 Controller 或集成入口，仅允许为了恢复现有测试契约而做兼容性修正，不新增需求外接口。

## 数据库设计（如有)
本任务默认不新增业务表结构。

允许的数据库相关改动边界：
- 为恢复 full suite 所必须的测试迁移脚本、测试数据初始化、Testcontainers/Flyway 装配修正
- 为保住 029 MySQL 真实迁移路径所需的最小兼容性修正

不在本任务内主动进行：
- 与当前失败无关的 schema 重整
- 跨聚合索引/字段顺手优化
- 为未来需求预埋表结构

## 影响范围
- **domain**: 默认无新增；仅当 failure classification 证明 domain 规则直接导致现有失败时，做最小必要修复与对应单测。
- **application**: 默认无新增；仅当现有应用编排导致失败时做最小必要修复与 Mockito 单测。
- **infrastructure**: 可能涉及现有 Repository 测试支撑、Flyway/Testcontainers 集成、测试资源与持久化实现的最小修复。
- **adapter**: 可能涉及 `OrderControllerTest` 等 Web 层失败的测试装配、MockMvc 契约或最小兼容修复。
- **start**: 可能涉及 Spring Boot 测试装配、Flyway 校验、消息集成测试环境配置。

## 非目标
- 不回退 029 的 MySQL Testcontainers 改进。
- 不把 030 扩展为全面重构测试框架或统一改写所有集成测试。
- 不处理 failure classification 之外的“看起来也该顺手修”的历史问题。
- 不修改 029 文档/产物；029 仅作为上下文输入。
- 若识别出仍然存活、且与当前主阻断链无关的失败，应该再次拆任务，而不是继续折叠进 030。

## 验收条件
- failure classification 形成可执行的失败清单与分层归因，并在任务过程中持续维护。
- 029 回归守护路径保持通过：真实 MySQL Testcontainers 仓储/Flyway 校验不回退。
- 对纳入 030 范围的失败完成最小修复，并具备 Red-Green 证据。
- 全量 `mvn test` 通过。
- `mvn checkstyle:check` 通过。
- `./scripts/entropy-check.sh` 通过。

## 架构评审

**评审人**：@architect
**日期**：2026-05-09
**结论**：通过

### 评审检查项（15 维四类）

**架构合规（7 项）**
- [x] 聚合根边界合理（本任务不新增聚合，仅恢复测试套件与装配）
- [x] 值对象识别充分（无新增值对象；若暴露业务不变量缺口需另案处理）
- [x] Repository 端口粒度合适（不预设新增端口，优先修正现有实现/装配）
- [x] 与已有聚合无循环依赖（不新增聚合依赖；跨聚合失败不得借机引入直接写依赖）
- [x] DDL 设计与领域模型一致（默认不新增业务表；仅允许与 029 MySQL/Flyway 保真度直接相关的最小修正）
- [x] API 设计符合 RESTful 规范（不新增对外 API）
- [x] 对象转换链正确（如触及代码，仍必须保持 Request/Response ↔ DTO ↔ Domain ↔ DO 边界）

**需求质量（3 项）**
- [x] 需求无歧义：030 与 QA-approved 029 明确隔离，目标限定为 full suite recovery
- [x] 验收条件可验证：failure classification、029 定向回归、三项 end-state verification 均可用命令验证
- [x] 业务规则完备：本任务无新增业务规则；修复中若暴露业务规则缺口需按最小失败簇处理或另拆任务

**计划可执行性（2 项）**
- [x] task-plan 粒度合格：已按 failure classification、029 targeted verification、clustered fix、reclassification、end-state verification 拆分，并包含文件路径、验证命令、预期输出
- [x] 依赖顺序正确：先归类与测试边界确认，再按受影响层最小修复；不得在未归因前改生产代码

**可测性保障（3 项）**
- [x] AC 自动化全覆盖：当前 AC 可由既有 targeted tests、full `mvn test`、checkstyle、entropy-check 覆盖；本阶段尚无 QA 测试设计文档
- [x] 可测的注入方式：若引入或调整 Spring Bean，必须使用构造函数注入，禁止字段注入
- [x] 配置校验方式合规：若涉及敏感/跨环境配置校验，必须遵循 ADR-005，使用 `@ConfigurationProperties + @Validated`

**心智原则（Karpathy — 动手前自检）**
- [x] 简洁性：不得为 029 回归守护预设新增测试方法；优先复跑既有 targeted tests，只有 classification 证明覆盖缺口时才新增最小测试
- [x] 外科性：仅处理阻断 full suite 的已归因失败簇；029 文档/产物保持不动
- [x] 假设显性：surviving unrelated failures 的 split-again 边界已在设计中显式列出

### 架构基线检查
- 命令：`./scripts/entropy-check.sh`
- 退出码：0
- 结果：PASS，FAIL 0，WARN 14
- 主要 WARN：auth 相关既有测试缺口、ADR 状态节格式提示、029 已验收未归档、归档目录历史修改提示；均非 030 设计阻断项

### 评审意见
- 通过。030 与 029 边界清晰：030 不修改 029 文档/产物，不回退 MySQL 8 Testcontainers/Flyway 路径，只把 029 的 targeted tests 作为回归保护输入。
- failure-bucketing-first 策略符合目标驱动与外科式变更原则；Build 阶段必须先产出失败分类，再决定是否修改测试装配、配置或最小生产代码。
- 对 task-plan 的执行约束：2.1/2.2 的“029 regression guards”应优先理解为复跑并保护既有 `OrderRepositoryImplTest`、`PaymentRepositoryImplTest`、`NotificationRepositoryImplTest`、`FlywayVerificationTest`，不得在未证明覆盖缺口前新增 `should_keep_*` 占位测试方法。
- 若 full suite 中仍存在与 029 MySQL/Flyway path 和当前主阻断簇无关的 surviving failures，必须在 dev-log 中记录证据并另拆后续任务，不折叠进 030。
- 三项最终验证命令必须保持为用户要求的边界：`mvn test`、`mvn checkstyle:check`、`./scripts/entropy-check.sh`；如因 Docker/Testcontainers 需要环境变量或 Maven settings，可在 dev-log/handoff 中记录等价执行命令与原因。

### 需要新增的 ADR
无需新增 ADR。本任务不引入新的架构决策，仅执行既有六边形、ADR-005 配置校验、ADR-006 跨聚合事件边界与测试保真度约束。
