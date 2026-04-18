---
name: dispatching-parallel-agents
description: "Ralph 与编排 agent 决定何时可以并行派发子 agent 的判定手册。显式列出 claude-j 流水线中的依赖边界：哪些能并行（只读研究、多文件 grep）、哪些禁止并行（TDD Red-Green、聚合间状态修改、QA 验收）。"
user-invocable: false
disable-model-invocation: true
allowed-tools: "Read Grep Glob"
---

# 派发并行子 Agent（dispatching-parallel-agents）

## 核心结论（先给答案）

> **Ralph 5 阶段本身不能并行**（Spec→Review→Build→Verify→Ship 有刚性因果依赖）。
>
> **但阶段"内部"的只读/无状态步骤可以并行**：研究、多聚合 grep、跨文件检查。这是并行的唯一合法区。

盲目并行化 Ralph = 破坏 TDD 因果 + 破坏 handoff 状态机 + 破坏上下文隔离承诺。

## 可 / 不可并行：一张判定表

### ✅ 可以并行（独立、只读、无共享状态）

| 场景 | 举例 | 工具 |
|------|------|------|
| **Spec 前置研究** | 同时读 `docs/architecture/overview.md` + `docs/standards/java-dev.md` + 已有聚合代码 | 多个 Read 工具并行 |
| **@architect 交叉验证** | 同时 grep 多个 ADR + 查 CLAUDE.md 聚合表 + 跑 entropy-check | 多个 Grep / Bash 并行 |
| **多文件同质检查** | 跨 7 个聚合检查某反模式（字段注入、`List.of`）| Grep 模式 + multiple Bash |
| **Build 前置阅读** | @dev 开始编码前并行读 requirement-design + java-dev.md + 示例聚合 | Read 并行 |
| **@qa 独立 re-run** | 同时跑 mvn test / checkstyle / entropy-check（互不依赖） | 3 个 Bash 并行 |
| **多任务全无交集的研究子 agent** | 对 2 个完全独立的 task 各派一个 research-only agent | Agent 工具并行 |

### ❌ 禁止并行（有状态、有序、有产物依赖）

| 场景 | 为什么禁止 |
|------|------|
| **TDD Red → Green** | 必须先看到 RED（铁律①），再 Green；并行执行会导致"绿了就通过"的假阳性 |
| **同聚合内 Domain → Application → Infra → Adapter** | 依赖方向严格：后层依赖前层的编译产物 |
| **Spec 和 Review 并行** | Review 必须读 Spec 产物（requirement-design.md）|
| **Build 和 Verify 并行** | QA 必须读 Build 提交的代码和 pre-flight 证据 |
| **同一 task 内多个修复单元并行** | 见 receiving-code-review：单问题闭环，并行会丢失因果证据 |
| **多个 agent 同时改同一聚合** | 写冲突 + `guard-agent-scope.sh` 会拦 |
| **`@dev` 编码 + `@qa` 写测试用例设计** | QA 必须基于 Build 产物写 test-case-design，时机错位 |
| **Ralph 返工循环并行** | 第 N 轮 dev 修复必须等第 N 轮 qa 验收结束 |

### 🤔 灰区：看情况

| 场景 | 条件 |
|------|------|
| **跨聚合独立扩展**（如"order 加支付" + "coupon 加过期清理"） | 各自在**独立 worktree** 里用 Ralph 各走一遍（用 `using-git-worktrees`） |
| **Spec 阶段多方案调研** | 可以让 2 个只读子 agent 研究不同方向，最后主 agent 做决策；但不要并行让它们各写一份 requirement-design |

## Ralph 主 agent 的并行决策流程

每次准备调度子 agent 前问 4 个问题：

```
Q1: 两个任务是否共享写入目标（同文件 / 同 handoff.md / 同 .claude-current-role）？
    → 是 → 必须串行
Q2: 后任务是否依赖前任务的产出物（commit / 文档 / 测试输出）？
    → 是 → 必须串行
Q3: 两个任务是否都只 Read / Grep / Bash（read-only 命令）？
    → 是 → 可以并行 ✅
Q4: 两个任务是否改动同一模块的源码？
    → 是 → 必须串行
```

**通过 Q1+Q2+Q4 的否决 + Q3 的肯定**，才算真正可并行。

## 并行派发的具体做法

### 方式 A：同一消息内多个工具调用（最常用）

Ralph 主 agent 在**一条消息**里发多个独立工具调用：

```
- Read(docs/architecture/overview.md)
- Read(docs/standards/java-dev.md)
- Grep("@Value", path="claude-j-*/src/main/java")
- Bash("./scripts/entropy-check.sh")
```

执行框架并行发起，回包后主 agent 汇总。这是**零风险并行**。

### 方式 B：多个只读子 Agent 并行

用 Agent 工具同时派发 2+ 个 **research-only** 子 agent：

```python
# 仅用于调研 / 多方向探索，每个 agent 都不得写源码
Agent(subagent_type="Explore", prompt="调研 A 方向...")
Agent(subagent_type="Explore", prompt="调研 B 方向...")
```

约束：
- 每个 prompt 明确"只读、不得修改文件"
- 返回 < 500 字总结，主 agent 整合
- **不允许并行派 @dev 子 agent**（@dev 会写文件，违反并行前提）

### 方式 C：跨 worktree 多任务并行

不同 task 放到**不同 worktree**（见 `using-git-worktrees`），用户在各自 worktree 启动独立 claude 会话。这是**进程级隔离**，Ralph 各跑各的，互不干扰。

这不是 Ralph 内部并行，是**用户级并行**。

## Ralph 现有流程的并行化点（具体建议）

对照当前 `.claude/skills/ralph/SKILL.md`，合法并行机会：

### Spec 阶段内
- @dev 前置阅读（overview.md / java-dev.md / 已有聚合）→ 同一消息多 Read 并行
- 同时 Grep 参考聚合的 Repository 端口 / Converter 模式

### Review 阶段内
- @architect 交叉验证：同一消息内并行
  - `Grep("ADR", path="docs/architecture/decisions")`
  - `Read(docs/architecture/overview.md)`
  - `Bash("./scripts/entropy-check.sh")`

### Build 阶段内（有限）
- 只在**前置阅读**阶段可并行（读规则 + 读设计 + 读参考聚合）
- **编码阶段严格串行**：Domain → Application → Infra → Adapter，每层一个 commit
- 每层内部的测试可以写完一批后**并行运行**（`mvn -T 2C test`），但不要并行编辑代码

### Verify 阶段内
- 独立重跑三项检查 → 并行发三个 Bash
  - `mvn clean test`
  - `mvn checkstyle:check -B`
  - `./scripts/entropy-check.sh`
- 三者无依赖，耗时并行节省 30-50%

### 跨 task
- 用 `using-git-worktrees` 做物理隔离
- **不要**在同一 Ralph 会话里并行调度 2 个 task 的 @dev 子 agent（handoff 状态会打架）

## 反模式（立即停）

| 反模式 | 为什么不行 |
|-------|-----------|
| "同时派 @dev 写 Domain 和 @qa 写测试用例设计" | QA 要读 Build 产物，时机错位 |
| "并行修复 Critical #1 + Critical #2（不同根因）" | Red-Green 因果被破坏，两次 Green 共用一次 commit → 丢证据 |
| "用 2 个 subagent 并行读 + 写 requirement-design 再合并" | 写合并冲突，违反「一个 task 一个 spec」 |
| "并行跑 mvn test + git commit" | commit 依赖 test 结果 |
| "Review 和 Spec 同时跑，让 Review agent 边读边写评审" | 评审前提是 Spec 已稳定；并行会评审到半成品 |

## 性能与正确性的权衡

并行的**真实收益**来自：
- 多个 Read / Grep 的 IO 等待可同时进行（每个约 100-500ms）
- `mvn test` + `checkstyle` + `entropy-check` 并行（约省 10-30s）

对比**风险**：
- 破坏 handoff 状态机 → QA 验收失败 → 返工一轮（数小时）
- 丢失 TDD 证据 → QA 打回要求重做 → 返工一轮

**因此默认态度：** 保守并行，只在 4 问全部通过时才并行。

## 当需要更高并行度时：换 worktree

单 Ralph 会话内再怎么并行也有上限（handoff 状态机限制）。真需要并行 2+ task？

→ **开 worktree**（`using-git-worktrees new`）→ 每个 task 独立 claude 会话独立 Ralph

这比在单会话里塞并行安全得多。

## 引用

- `agent-collaboration.md`：handoff 状态机、返工轮次上限
- `using-git-worktrees`：跨 task 物理隔离
- `receiving-code-review`：单问题闭环，禁止并行修复
- `verification-before-completion`：并行跑验证命令的合法场景
