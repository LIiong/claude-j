---
name: qa-ship
description: "@qa Ship 阶段：验收通过后归档任务目录，更新 CLAUDE.md 聚合列表，确认 ADR 完整。"
user-invocable: true
disable-model-invocation: true
argument-hint: "[task-id]-[task-name]"
allowed-tools: "Read Edit Glob Grep Bash(mv *) Bash(ls *) Bash(git *) Bash(echo *)"
---

# @qa Ship 阶段 — 归档与文档更新

QA 验收通过后，归档任务并更新项目文档。

## 输入
- 任务标识：`$ARGUMENTS`（如 `002-order-service`）

## 执行前：注册角色标记（Hook 自动识别用）
```bash
echo "qa" > .claude-current-role
```

## 前置条件
1. 阅读 `docs/exec-plan/active/$ARGUMENTS/handoff.md` — 确认 status: approved
2. 阅读 `docs/exec-plan/active/$ARGUMENTS/test-report.md` — 确认含"验收通过"
3. 如果上述条件不满足，**停止执行**，告知用户需先运行 `/qa-verify $ARGUMENTS`

## 执行步骤

### 1. 归档任务目录
```bash
mv docs/exec-plan/active/$ARGUMENTS/ docs/exec-plan/archived/$ARGUMENTS/
```

### 2. 更新 CLAUDE.md 聚合列表
检查本次任务是否引入新聚合（检查 `claude-j-domain/src/main/java/com/claudej/domain/` 下的子目录）：
- 对比 CLAUDE.md 中"当前聚合列表"表格与 domain 层实际目录
- 若有新聚合，追加到表格中（包含：聚合名、包名、状态、说明）

### 3. 确认 ADR 完整
检查 `docs/architecture/decisions/` 是否有本次任务新增的 ADR 文件：
- 确认 ADR 文件含"状态"、"背景"、"决策"三节
- 如缺失，告知用户补充

### 4. 验证归档结果
```bash
ls docs/exec-plan/archived/$ARGUMENTS/
```
确认以下文件存在：
- `requirement-design.md`
- `task-plan.md`
- `dev-log.md`
- `test-case-design.md`
- `test-report.md`
- `handoff.md`

### 5. 输出归档摘要
汇总：
- 归档路径
- 新增聚合（若有）
- 新增 ADR（若有）
- 遗留问题（Minor 级别待修复项，从 test-report.md 提取）

## 下一步
归档完成后，告知用户：
- 任务已归档到 `docs/exec-plan/archived/$ARGUMENTS/`
- 可通过 `git commit` 提交归档变更
- 如需开始新功能，运行 `/dev-spec [new-task-id]-[task-name]`
