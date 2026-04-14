# Agent 协作规则

## 编排架构

```
Ralph（编排主 Agent）
  ├── Agent(@dev)       → Spec / Build 阶段
  ├── Agent(@architect) → Review 阶段
  ├── Agent(@qa)        → Verify 阶段
  └── 主 Agent 直接执行  → Ship 阶段（轻量）
```

**核心原则**：
- Ralph 主 Agent **只做决策和调度**，不写业务代码/测试
- 每个阶段通过 **Agent 工具** 启动子 Agent，获得**独立上下文窗口**
- 所有状态通过 **文件系统**（exec-plan 目录）和 **git** 传递，不通过内存
- 子 Agent 完成后，主 Agent **验证产出物**再决定下一步

## Agent 角色

| Agent | 职责 | 核心产出 |
|-------|------|---------|
| **Ralph** | 编排调度、阶段验证、流程决策 | 无直接代码产出 |
| **@dev** | 需求分析、设计、编码、单测 | 业务代码 + 单测 + 设计文档 |
| **@qa** | 测试设计、验收测试、代码审查 | 测试用例 + 测试报告 + 集成测试 |
| **@architect** | 架构评审、ADR 编写 | 评审意见 + ADR |

## 上下文隔离规则（严格遵守）

### @dev 可写范围
- `src/main/java/` 下所有业务代码
- `src/test/java/` 下对应模块的单元测试
- `docs/exec-plan/active/{task-id}/requirement-design.md`
- `docs/exec-plan/active/{task-id}/task-plan.md`
- `docs/exec-plan/active/{task-id}/dev-log.md`
- `docs/exec-plan/active/{task-id}/handoff.md`
- `docs/exec-plan/active/{task-id}/progress.md`
- `docs/architecture/decisions/` 下的 ADR 文件
- `claude-j-start/src/main/resources/db/schema.sql`

### @dev 禁止修改
- `test-case-design.md` / `test-report.md`（@qa 职责）
- `docs/standards/`（标准文档，需讨论后修改）
- `.claude/`（配置文件，需讨论后修改）

### @qa 可写范围
- `src/test/java/` 下的测试代码（含 start 模块集成测试）
- `docs/exec-plan/active/{task-id}/test-case-design.md`
- `docs/exec-plan/active/{task-id}/test-report.md`
- `docs/exec-plan/active/{task-id}/handoff.md`

### @qa 禁止修改
- `src/main/java/` 下的业务代码（发现问题通知 @dev 修复）
- `requirement-design.md` / `dev-log.md`（@dev 职责）

### @architect 可写范围
- `docs/exec-plan/active/{task-id}/requirement-design.md`（仅追加「架构评审」章节）
- `docs/exec-plan/active/{task-id}/handoff.md`
- `docs/architecture/decisions/` 下的 ADR 文件

### @architect 禁止修改
- 任何 Java 代码（`*.java`）

### 写作域强制机制
以上规则由 `guard-agent-scope.sh` Hook **技术强制**：
- 每个 Skill/子 Agent 启动时写入 `.claude-current-role` 角色标记
- Hook 读取角色标记，越权写入时 **exit 2 阻断**
- Ralph 主 Agent 在调度子 Agent 前负责写入角色标记

## 交接协议

### 交接文件：handoff.md
每次 Agent 间交接必须更新 `{task-dir}/handoff.md`，含：
- YAML front-matter（task-id、from、to、status、pre-flight、summary）
- 交接说明（发送方填写）
- 评审回复（接收方填写）

### 交接状态机
```
@dev spec完成 → handoff(to:architect, status:pending-review)
  → @architect 评审通过 → handoff(status:approved)
    → @dev build完成 → handoff(to:qa, status:pending-review, pre-flight:all pass)
      → @qa 验收通过 → Ship & Archive
      → @qa 要求修改 → handoff(status:changes-requested) → @dev 修复（最多 3 轮）
  → @architect 要求修改 → handoff(status:changes-requested) → @dev 修改设计
```

### 状态机强制机制
- `guard-dev-gate.sh` Hook 强制：`pending-review + to:architect` 时**阻止编码**
- Ralph 主 Agent 在每个阶段转换时**验证 handoff.md 状态**，不合规则不调度下一阶段

### 三项预飞检查（交接前置条件）
@dev 标记"待验收"前必须通过：
1. `mvn test` — 全部测试通过（含 ArchUnit）
2. `mvn checkstyle:check` — 代码风格通过
3. `./scripts/entropy-check.sh` — 熵检查通过

@qa 验收时必须独立重跑三项检查（不信任 @dev 标记）。

### 返工循环限制
- Ralph 主 Agent 跟踪返工轮次
- 最多 3 轮 @qa changes-requested → @dev 修复 → @qa 重验
- 超过 3 轮 → 终止流程，输出问题清单，请求人类介入

## Ralph 编排流程

### 全自动模式
```
Ralph 解析输入
  → 调度 @dev 子 Agent（Spec）→ 验证产出物
  → 调度 @architect 子 Agent（Review）→ 验证评审结果
  → 调度 @dev 子 Agent（Build）→ 验证三项检查
  → 调度 @qa 子 Agent（Verify）→ 验证验收结论
  → 主 Agent 直接执行（Ship）→ 归档
```

### 续跑模式
Ralph 读取 handoff.md 判断当前阶段，从该阶段开始调度子 Agent。

### Ralph Loop 模式
用于超大型需求，每次迭代使用全新 Claude Code 进程（比子 Agent 更彻底的上下文隔离）。

## Ralph Loop 进度文件协议

当被 `ralph-loop.sh` 调用时，每个 Agent 必须：

### 迭代开始
1. 读取 `{task-dir}/progress.md` 了解当前进度
2. 读取 `git log --oneline -10` 了解最近变更
3. 识别下一个待办任务（progress.md 中第一个 `[ ]` 项）

### 迭代结束
1. 将完成的任务标记为 `[x]`（附 commit hash）
2. 在迭代日志中记录：完成内容、遇到问题、下次应做什么
3. 确保所有变更已 `git commit`

### 单次迭代原则
- 每次迭代只完成 1-2 个任务（保持焦点）
- 遇到阻塞时记录到 progress.md 并退出（让全新上下文重试）
