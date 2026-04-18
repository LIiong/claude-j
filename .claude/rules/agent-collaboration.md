---
description: "Ralph 编排与 @dev / @architect / @qa 子 Agent 协作规则。操作 exec-plan 文档、handoff、agent/skill 配置时生效。"
globs:
  - "docs/exec-plan/**/*.md"
  - ".claude/skills/**/*.md"
  - ".claude/agents/**/*.md"
  - "**/handoff.md"
  - "**/progress.md"
alwaysApply: false
---

# Agent 协作规则

## 适用范围
- **生效时机**：编辑 exec-plan 任务文档、交接文件或 agent/skill 配置时自动注入。
- **目标**：通过明确边界、交接协议和状态机，确保多 Agent 协作稳定可追踪。

## 编排拓扑
```
Ralph（编排主 Agent — 只做决策和调度）
  ├── Agent(@dev)       → Spec / Build（独立上下文）
  ├── Agent(@architect) → Review（独立上下文）
  ├── Agent(@qa)        → Verify（独立上下文）
  └── 主 Agent 直接执行  → Ship（轻量操作）
```
状态通过 **文件系统 + git** 在子 Agent 间传递，不经内存。

## MUST（强制）

### 编排职责
- Ralph 主 Agent 只做调度与决策，不直接承担业务编码与测试实现。
- 每个阶段必须通过子 Agent 执行，确保上下文隔离。
- 阶段推进前必须验证前一阶段产物与 `handoff.md` 状态。

### 写作域边界（由 `guard-agent-scope.sh` 强制执行）
| 角色 | 可写 | 禁写 |
|------|------|------|
| `@dev` | 业务代码、单测、设计/开发文档、schema、ADR | `test-case-design.md`、`test-report.md` |
| `@qa` | 测试代码、测试设计、测试报告、交接文件 | `src/main/java/`、`requirement-design.md`、`dev-log.md` |
| `@architect` | 架构评审章节、`handoff.md`、ADR | `*.java`、`task-plan.md`、`dev-log.md`、测试文件 |

调度子 Agent 前必须写入角色标记：
```bash
echo "{dev|qa|architect}" > .claude-current-role
```

### 交接与状态机
- 每次阶段交接必须更新 `{task-dir}/handoff.md`。
- `handoff.md` 必须包含：`task-id`、`from`、`to`、`status`、`pre-flight`、`summary`。
- 状态机合法转换：
  ```
  (initial)
    → pending-review + to:architect   # Spec 完成
    → approved        + to:architect   # 评审通过
    → pending-review + to:qa           # Build 完成
    → changes-requested + to:qa        # 验收打回
    → approved        + to:qa          # 验收通过（可 Ship）
  ```
- `pending-review + to:architect` 期间禁止编码（由 `guard-dev-gate.sh` 强制）。
- 开发进入验收前必须通过三项预飞：`mvn test`、`mvn checkstyle:check`、`./scripts/entropy-check.sh`。
- `@qa` 验收时必须独立重跑三项检查，不直接信任上游 `pre-flight` 标记。

### 返工循环限制
- `@qa changes-requested → @dev fix → @qa re-verify` 最多 3 轮。
- 超过 3 轮必须终止自动化，输出问题清单，请求人工介入。

## MUST NOT（禁止）
- 禁止子 Agent 修改其职责外文件（由 Hook + 人工双重确认）。
- 禁止在 `pending-review + to:architect` 状态下继续编码。
- 禁止跳过 `handoff.md` 直接推进到下一阶段。
- 禁止无限返工循环（超过 3 轮必须升级）。
- 禁止子 Agent 在完成后跳过 `git commit`（状态传递的唯一载体）。

## 执行检查（每次阶段切换时）
1. 检查写作域是否越权（Hook + 人工双重确认）。
2. 检查 `handoff.md` 状态是否合法并与当前阶段匹配。
3. 检查三项预飞是否通过并可复现。
4. 若为 Ralph Loop，更新 `progress.md` 并保证本轮产物可追溯。
5. 清理 `.claude-current-role`（Ship 完成后）。
