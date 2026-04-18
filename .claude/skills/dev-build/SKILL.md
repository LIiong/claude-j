---
name: dev-build
description: "@dev Build 阶段：TDD 开发，按 domain→application→infrastructure→adapter 顺序编码，三项检查通过后交接 @qa。"
user-invocable: true
disable-model-invocation: true
argument-hint: "[task-id]-[task-name]"
allowed-tools: "Read Write Edit Glob Grep Bash(mvn *) Bash(./scripts/*) Bash(mkdir *) Bash(ls *) Bash(git *) Bash(echo *)"
---

# @dev Build 阶段 — TDD 分层开发

你是 claude-j 项目的高级 Java 后端开发工程师，正在执行 Build 阶段。

## 输入
- 任务标识：`$ARGUMENTS`（如 `007-shopping-cart`）
- `docs/exec-plan/active/$ARGUMENTS/` 下由 Spec 阶段产出的设计文档
- `handoff.md` 状态必须为 `approved + to: architect`（评审通过，可开始编码）

## 执行前：注册角色标记
```bash
echo "dev" > .claude-current-role
```

## 前置条件（必须先完成）
1. 阅读 `requirement-design.md` — 确认含「架构评审」章节且结论为「通过」
2. 阅读 `task-plan.md` — 了解任务清单与拆解
3. 读取 `handoff.md` — 必须 `status: approved` 且 `to: architect`
4. 若使用 Ralph Loop，阅读 `progress.md` — 了解当前进度

## 参考文档
- `docs/standards/java-dev.md` — 开发规范
- `docs/standards/java-test.md` — 测试规范
- 已有聚合代码（如 shortlink、coupon）— 参考实现模式

## TDD 原则

**铁律**：
```
NO PRODUCTION CODE WITHOUT A FAILING TEST FIRST
```

**每层内部遵循：先写测试（红）→ 再写实现（绿）→ 重构（必要时）**。

- **Red 必须真实**：`mvn test` 看到失败 + 失败原因符合预期（不是编译错误/找不到类）。
- **违反 = 删除重来**：若发现生产代码无对应 Red 测试 → 删除该段代码，重新走 Red-Green-Refactor。
- 在动手写代码前对照 `docs/standards/java-test.md#tdd-反模式对照表` 自检 11 项借口；任一命中即视为违规。
- 详尽的 TDD 规则与反模式见 `docs/standards/java-test.md`（规则会在编辑 `src/test/java/` 时自动注入）。

### 🔴 Red / Green commit 必须分离（强制）

> **背景**：010-secret-externalize 发现生产类与测试类写入同一个 `feat` commit，无法从 git 历史证明 Red 真实出现过。从 011 起强制两段式提交。

**模式**：
```bash
# 第 1 个 commit — Red（只含失败测试，跑 mvn test 必须红）
git add src/test/java/.../XxxTest.java
git commit -m "test(layer): add failing tests for Xxx (Red)"

# 第 2 个 commit — Green（加生产代码让测试通过）
git add src/main/java/.../Xxx.java
git commit -m "feat(layer): implement Xxx to pass tests (Green)"

# 必要时 — Refactor（不改行为，保持绿）
git commit -m "refactor(layer): extract YyyHelper from Xxx (Refactor)"
```

**例外**：仅修复现有 bug（已有覆盖测试）、重命名、import 整理等无新增行为的改动，可合并为单 commit。

**举证**：`handoff.md` 的 `pre-flight.tdd-evidence` 字段必须列出每个**新增**生产类的 `red-commit` + `green-commit` 两个 hash，@qa/@architect 可按 hash 独立回溯。

## 开发顺序（严格自下而上，每层一个 commit）

### 第 1 层：Domain（纯 Java，禁止 Spring/MyBatis）
模块：`claude-j-domain`

**产出物**（`domain.{aggregate}.model.*`）：
| 类型 | 位置 | 约束 |
|------|------|------|
| 值对象 | `model/valobj/` | `private final` 字段；构造校验；`@Getter @EqualsAndHashCode @ToString` |
| 实体 | `model/entity/` | `@Getter`；状态变更通过方法，禁止 `@Setter` |
| 聚合根 | `model/aggregate/` | `@Getter`；工厂 `create(...)` + `reconstruct(...)` + 业务方法封装不变量 |
| Repository 端口 | `repository/` | 纯 Java 接口，返回 Domain，含 `save` / `findByXxx` |

**ErrorCode**：在 `domain.common.exception.ErrorCode` 枚举中添加新错误码。

**TDD 顺序**：
1. 先写 Domain 测试（JUnit 5 + AssertJ，**禁止 Spring、禁止 Mockito**）
2. 覆盖：不变量强制、状态转换、值对象相等性、边界场景
3. 命名：`should_{预期行为}_when_{条件}`
4. 运行 `mvn test -pl claude-j-domain` 确认红→绿
5. `git commit` — `feat(domain): $ARGUMENTS 领域模型与聚合根`

### 第 2 层：Application（用例编排）
模块：`claude-j-application`

**产出物**（`application.{aggregate}.*`）：
| 类型 | 位置 | 注解 |
|------|------|------|
| 命令 | `command/` | `@Data` |
| DTO | `dto/` | `@Data` |
| 组装器 | `assembler/` | MapStruct `@Mapper(componentModel = "spring")` |
| 应用服务 | `service/` | `@Service @Transactional`，依赖 Repository 端口接口 |

**TDD 顺序**：
1. 先写 Application 测试（`@ExtendWith(MockitoExtension.class)`）
2. `@Mock` Repository 端口，`@InjectMocks` 应用服务
3. 验证编排顺序（方法调用）+ save() 发生次数 + 命令校验
4. 运行 `mvn test -pl claude-j-application` 确认红→绿
5. `git commit` — `feat(application): $ARGUMENTS 应用服务与组装器`

### 第 3 层：Infrastructure（持久化适配器）
模块：`claude-j-infrastructure`

**产出物**（`infrastructure.{aggregate}.persistence.*`）：
| 类型 | 位置 | 注解 |
|------|------|------|
| DO | `dataobject/` | `@Data @TableName("t_xxx")` |
| Mapper | `mapper/` | 继承 `BaseMapper<XxxDO>` |
| 转换器 | `converter/` | 静态 `toDataObject()` / `toDomain()` |
| Repository 实现 | `repository/` | `@Repository`，实现 domain 端口 |

**TDD 顺序**：
1. 先写 Infrastructure 测试（`@SpringBootTest` + H2）
2. 覆盖：保存→查询往返 + DO↔Domain 映射准确性
3. 运行 `mvn test -pl claude-j-infrastructure` 确认红→绿
4. `git commit` — `feat(infrastructure): $ARGUMENTS 持久化适配器`

### 第 4 层：Adapter（REST 端点）
模块：`claude-j-adapter`

**产出物**（`adapter.{aggregate}.web.*`）：
| 类型 | 位置 | 注解 |
|------|------|------|
| Request/Response | `request/`、`response/` | `@Data` + `@Valid` 字段约束 |
| Controller | 根包 | `@RestController`，仅依赖 Application，返回 `ApiResult<T>` |

**TDD 顺序**：
1. 先写 Adapter 测试（`@WebMvcTest` + MockMvc + `@MockBean`）
2. 覆盖：HTTP 状态码（200/400/404/500）+ 请求校验 + 响应结构
3. 运行 `mvn test -pl claude-j-adapter` 确认红→绿
4. `git commit` — `feat(adapter): $ARGUMENTS REST 端点`

### 第 5 层：Start（装配）
模块：`claude-j-start`

- DDL 未存在则写入 `claude-j-start/src/main/resources/db/schema.sql`
- 必要时更新 `application.yml`
- 编写 start 模块集成测试（可选，@qa 阶段补充）
- `git commit` — `feat(start): $ARGUMENTS DDL 与装配`

## 交付前三项验证（全过才可交接）
```bash
mvn clean test              # 全部测试通过（含 ArchUnit 14 条规则）
mvn checkstyle:check -B     # 代码风格
./scripts/entropy-check.sh  # 12 项架构检查
```
**任一失败 → 修复后重跑，直到全绿。禁止标记 pass 绕过。**

> **举证铁律**：写 `handoff.md` 的 `pre-flight` 字段前，三项命令必须在**本消息中真实运行**，并在 `summary` 里附输出摘要（测试通过数 / 退出码 / 检查项数）。不得照搬历史输出、不得以"应该/可能"代替证据。完整规则见 `.claude/skills/verification-before-completion/SKILL.md`。

## 交付步骤

### 1. 更新 task-plan.md
将本阶段任务状态改为「单测通过」。

### 2. 更新 dev-log.md
记录：关键决策、踩坑、与原设计的变更。

### 3. 更新 handoff.md
```yaml
---
task-id: "$ARGUMENTS"
from: dev
to: qa
status: pending-review
timestamp: "{ISO-8601}"
pre-flight:
  mvn-test: pass       # Tests run: X, Failures: 0, Errors: 0
  checkstyle: pass     # Exit 0, 0 violations
  entropy-check: pass  # 12/12 checks passed
  tdd-evidence:        # 🔴 每个新增生产类列出红绿 commit
    - class: "XxxAggregate"
      red-commit: "abc1234"
      green-commit: "def5678"
    - class: "XxxRepositoryImpl"
      red-commit: "ghi9abc"
      green-commit: "jkl0def"
summary: "{X 个聚合/端口实现完成，测试 Y 条全绿}"
---
```

### 4. git 提交交接文档
```bash
git add docs/exec-plan/active/$ARGUMENTS/
git commit -m "docs(build): $ARGUMENTS 交接 QA 验收"
```

### 5. 若使用 Ralph Loop
更新 `progress.md` 标记完成任务（附 commit hash）。

## 上下文边界（严格遵守）
**可写**：`src/main/java/`、`src/test/java/`、exec-plan 文档（设计/计划/日志/交接/进度）、ADR、`schema.sql`
**禁写**：`test-case-design.md`、`test-report.md`（@qa 职责）、`docs/standards/`、`.claude/`

## 完成后行为

**独立使用模式**：告知用户运行 `/qa-verify $ARGUMENTS`。

**Ralph 编排模式**：输出开发摘要（代码文件数 / 测试条数 / 三项验证结果），由 Ralph 自动调度 QA 阶段。
