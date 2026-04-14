---
name: task-status
description: "查看所有活跃任务和已归档任务的状态一览表，快速了解每个任务当前阶段。"
user-invocable: true
disable-model-invocation: true
argument-hint: "[task-id]（可选，不填则显示全部）"
allowed-tools: "Read Glob Grep Bash(ls *)"
---

# 任务状态一览

快速查看所有任务的当前执行阶段和状态。

## 输入
- `$ARGUMENTS`（可选）：指定 task-id 只看单个任务，留空查看全部

## 执行步骤

### 1. 扫描活跃任务
遍历 `docs/exec-plan/active/*/`，对每个任务目录：

1. 读取 `handoff.md` 中的 `status:` 和 `to:` 字段
2. 读取 `handoff.md` 中的 `from:` 和 `pre-flight:` 字段
3. 检查以下文件是否存在，判断当前阶段：

| 条件 | 阶段 | 说明 |
|------|------|------|
| 无 requirement-design.md | 未开始 | 任务目录已创建但未设计 |
| 无 handoff.md | Spec 进行中 | 正在设计 |
| to: architect, status: pending-review | 等待评审 | @dev 已提交，等 @architect |
| to: architect, status: changes-requested | 评审打回 | @architect 要求修改设计 |
| to: architect, status: approved | Build 待开始 | 评审通过，可开始编码 |
| status: coding-in-progress | Build 进行中 | 正在编码 |
| to: qa, status: pending-review | 等待验收 | @dev 已完成，等 @qa |
| to: qa, status: changes-requested | 验收打回 | @qa 要求修复 |
| to: qa, status: approved | 待归档 | 验收通过，可 Ship |
| to: ship | 已归档 | 任务完成 |

### 2. 扫描已归档任务
遍历 `docs/exec-plan/archived/*/`，标记为"已完成"。

### 3. 输出状态表

```markdown
## 活跃任务

| Task ID | 阶段 | 状态 | From → To | Pre-flight |
|---------|------|------|-----------|------------|
| 005-user-management | Build 进行中 | coding-in-progress | dev → qa | mvn:pass checkstyle:pass entropy:pass |

## 已归档任务

| Task ID | 完成状态 |
|---------|---------|
| 002-order-service | ✅ 已归档 |
| 003-link-management | ✅ 已归档 |
```

### 4. 如指定了 task-id
只显示该任务的详细信息：
- 当前阶段和状态
- handoff.md 完整内容
- progress.md 进度（如有）
- 最近 5 条相关 git commit
