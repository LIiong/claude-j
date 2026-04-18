---
name: using-claude-j-workflow
description: "claude-j 工作流元导航。会话启动时告诉 agent：项目用 Ralph 5 阶段交付、有哪些 skill、应该先做什么。SessionStart hook 自动注入。"
user-invocable: false
disable-model-invocation: true
allowed-tools: "Read"
---

# 使用 claude-j 工作流（元 skill）

> **目的**：这是进入 claude-j 项目时的**第一张地图**。告诉新会话 / 新 agent：不要凭直觉工作，所有改动走 Ralph 5 阶段流水线。

## 核心心智（一句话）

> **任何 `src/main/java/` 下的业务改动都必须走 Ralph 5 阶段流水线。绕过 = 被 Hook 拦。**

## 5 阶段交付流水线

```
Spec → Review → Build → Verify → Ship
@dev   @architect  @dev    @qa    主 Agent
```

每阶段独立上下文，通过文件（`docs/exec-plan/active/{task-id}/`）+ git 传递状态。

**阶段产物**：

| 阶段 | 负责 | 关键产物 | 下一步触发 |
|------|------|---------|-----------|
| Spec | @dev | requirement-design.md, task-plan.md, handoff(to:architect, pending-review) | @architect 评审 |
| Review | @architect | requirement-design.md「架构评审」章节, ADR (如需) | approved → @dev 编码 |
| Build | @dev | src/**, 分层测试, handoff(to:qa, pending-review, pre-flight.all=pass) | @qa 验收 |
| Verify | @qa | test-case-design.md, test-report.md, handoff(approved/changes-requested) | Ship 或返工 |
| Ship | 主 Agent | `docs/exec-plan/archived/{task-id}/` | — |

**返工循环 ≤ 3 轮**（超限必须升级到人工）。

## Skill 入口（按场景）

### 用户可主动调用（`user-invocable: true`）

| 场景 | Skill | 说明 |
|------|-------|------|
| 启动新任务（推荐） | `/ralph <task-id> <需求>` | 一键走完 5 阶段 |
| 已有任务续跑 | `/ralph <task-id>` | 自动检测阶段继续 |
| 超大需求多会话 | `/ralph <task-id> --loop` | 输出 ralph-loop.sh 命令 |
| 单阶段操作 | `/dev-spec` / `/architect-review` / `/dev-build` / `/qa-verify` / `/qa-ship` | 各阶段独立入口 |
| 查看任务状态 | `/task-status [task-id]` | 进度一览 |
| 交付前验证 | `/full-check` | mvn test + checkstyle + entropy-check |
| 接收 code review 反馈 | `/receiving-code-review <task-id>` | @qa 打回后结构化修复 |
| 多任务并行 | `/using-git-worktrees` | worktree 隔离 |

### Agent 自动触发（`user-invocable: false`，不可手动调用）

| 场景 | Skill | 触发方 |
|------|-------|--------|
| 声称"通过/完成"前 | `verification-before-completion` | 所有角色 |
| 遇到 bug / QA 打回 | `systematic-debugging` | @dev |
| 需要并行调度子 agent | `dispatching-parallel-agents` | Ralph 主 agent |

## 会话开始时 agent 自检（从上到下）

1. **读 `CLAUDE.md`** — 项目概述、聚合列表、三条心智铁律、四项 Karpathy 原则
2. **看 active 任务**：`docs/exec-plan/active/` 下是否有进行中的任务？若有，读对应 `handoff.md` 判断断点
3. **看 git status** — 有未提交改动？先弄清它属于哪个任务的哪个阶段
4. **读 `.claude-current-role`** — 当前应以哪个角色工作？（Hook 会据此拦截越权写）
5. **判断用户意图**：
   - 要启动新任务 → `/ralph <task-id> <需求>`
   - 要续跑 → `/ralph <task-id>`
   - 要查状态 → `/task-status`
   - 纯问答/探查 → 直接回答，不启动流水线

## 红旗（立即停）

以下任一成立 → **停下，不要直接编码**，先明确用什么 skill / 走哪个阶段：

- 用户说"改一下 X" 但 `docs/exec-plan/active/` 下无相关任务 → 问：这是新任务吗？要走 `/ralph` 吗？
- 准备写 `src/main/java/**` 但没有 `handoff.md` 或 handoff 状态不是 `approved` → 会被 `guard-dev-gate.sh` 拦；先完成 Spec + Review
- `.claude-current-role` 里是 `qa`，但用户让你改 `src/main/java/` → 写作域越权，`guard-agent-scope.sh` 会拦
- 声称"搞定了/修好了"但没跑 `mvn test` → 违反铁律②（VERIFICATION）

## 三条铁律（零例外）

```
1. TDD          —  NO PRODUCTION CODE WITHOUT A FAILING TEST FIRST
2. VERIFICATION —  NO COMPLETION CLAIMS WITHOUT FRESH VERIFICATION EVIDENCE
3. DEBUG        —  NO FIXES WITHOUT ROOT CAUSE INVESTIGATION FIRST
```

## 四项原则（面对选择时往哪偏）

1. 想清楚再写（显式假设、多解法列出、不懂就停）
2. 简洁优先（零投机、无需求外功能）
3. 外科式变更（只动该动的、不顺手优化）
4. 目标驱动（可验证的成功标准）

## 参考锚点

| 问题 | 去哪查 |
|------|-------|
| 架构分层 | `docs/architecture/overview.md`, `.claude/rules/architecture.md` |
| Java 规则 | `docs/standards/java-dev.md` |
| 测试规则 | `docs/standards/java-test.md` |
| ADR 决策 | `docs/architecture/decisions/` |
| Agent 协作 | `.claude/rules/agent-collaboration.md` |
| Karpathy | `.claude/rules/karpathy-guidelines.md` |
| 工业级缺口 | `docs/roadmap/industry-gap-analysis.md` |

## 与其他 skill 的关系

```
using-claude-j-workflow  ← 你现在看的（入口地图）
    │
    └─ 指向 →  ralph                      （交付主入口）
               ├─ dev-spec / architect-review / dev-build / qa-verify / qa-ship（单阶段）
               ├─ using-git-worktrees     （多任务并行隔离）
               ├─ receiving-code-review   （QA 打回后的修复流程）
               ├─ dispatching-parallel-agents（子 agent 并行派发决策）
               └─ 自动触发
                  ├─ verification-before-completion
                  └─ systematic-debugging
```

**不要死记这张图。只记一句：不确定时，先读这个 skill 再动手。**
