---
name: qa-ship
description: "@qa Ship 阶段：验收通过后归档任务目录，更新 CLAUDE.md 聚合列表，确认 ADR 完整，提交归档 commit。"
user-invocable: true
disable-model-invocation: true
argument-hint: "[task-id]-[task-name]"
allowed-tools: "Read Edit Glob Grep Bash(mv *) Bash(ls *) Bash(rm *) Bash(git *) Bash(echo *) Bash(./.claude/skills/qa-ship/scripts/*)"
---

# @qa Ship 阶段 — 归档与文档同步

QA 验收通过后，归档任务目录、同步项目级文档、清理运行时产物。

## 输入
- 任务标识：`$ARGUMENTS`（如 `007-shopping-cart`）

## 前置条件
1. `handoff.md` 必须 `status: approved`
2. `test-report.md` 末尾必须含「验收通过」
3. 条件不满足 → **停止**，告知用户先运行 `/qa-verify $ARGUMENTS`

## 执行步骤

### 0. 注册角色标记
```bash
echo "qa" > .claude-current-role
```

### 1. 核验归档前置条件（原子闸）
```bash
./.claude/skills/qa-ship/scripts/pre-archive-check.sh $ARGUMENTS
```

脚本执行 4 项必查：
1. `handoff.md` 含 `status: approved`（兼容 YAML / Markdown 加粗两种写法）且 `to` ∈ {qa, ralph}
2. `test-report.md` 末尾含「验收通过」
3. `task-plan.md` QA 行状态非「待办 / 进行中 / 待验收 / 待修复」
4. 重跑 `mvn test` + `mvn checkstyle:check` + `./scripts/entropy-check.sh` 三项全退 0

任一失败 → 终止归档并按脚本输出的修复清单处理。  
快速预演可加 `--skip-reverify` 暂跳过第 4 项（生产归档禁止跳过）。

### 2. 归档任务目录
```bash
mkdir -p docs/exec-plan/archived/
mv docs/exec-plan/active/$ARGUMENTS/ docs/exec-plan/archived/$ARGUMENTS/
```

### 3. 同步 CLAUDE.md 聚合列表
对比 `claude-j-domain/src/main/java/com/claudej/domain/` 下子目录与 CLAUDE.md「当前聚合」表格：
- 若本任务引入新聚合，在表格追加一行（聚合名、包名、说明）
- 若仅扩展已有聚合，无需改动

### 4. 确认 ADR 完整
```bash
ls docs/architecture/decisions/ | grep -i "$ARGUMENTS" || echo "无新增 ADR"
```
若有新增 ADR，确认含「状态 / 背景 / 决策」三节。

### 5. 核验归档产物
```bash
ls docs/exec-plan/archived/$ARGUMENTS/
```
必须包含：
- `requirement-design.md`（含架构评审章节）
- `task-plan.md`
- `dev-log.md`
- `test-case-design.md`
- `test-report.md`
- `handoff.md`

### 6. git 提交归档变更
```bash
git add docs/exec-plan/ CLAUDE.md
# 若有新 ADR
git add docs/architecture/decisions/
git commit -m "docs(ship): 归档 $ARGUMENTS"
```

### 7. 清理运行时产物
```bash
rm -f .claude-current-role
```

### 8. 输出归档摘要
| 项 | 值 |
|----|-----|
| 归档路径 | `docs/exec-plan/archived/$ARGUMENTS/` |
| 新增聚合 | {如有} |
| 新增 ADR | {如有} |
| 遗留 Minor 问题 | {从 test-report.md 提取} |
| 总 commit 数 | {本任务相关 commit 总数} |

## 完成后行为

**独立使用模式**：告知用户任务已归档，可开启新任务（`/dev-spec [new-task-id]`）。

**Ralph 编排模式**：输出归档摘要，Ralph 输出整体交付完成报告。
