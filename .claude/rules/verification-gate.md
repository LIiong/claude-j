---
description: "声称前必须举证：操作 handoff/test-report/dev-log 时强制附带验证命令输出，禁止'应该能过'类措辞。"
globs:
  - "**/handoff.md"
  - "**/test-report.md"
  - "**/dev-log.md"
  - "**/progress.md"
alwaysApply: false
---

# 声称前举证规则（Verification Gate）

## 适用范围
- **生效时机**：写入/编辑 `handoff.md`、`test-report.md`、`dev-log.md`、`progress.md` 时自动注入。
- **目标**：防止未经验证的"通过/完成/修复"类声称落入任务交接文档，造成下游 agent 基于假状态推进。

## MUST（强制）

### 写 handoff.md 的 `pre-flight` 字段前
- 必须在本消息中实际运行：`mvn test`、`mvn checkstyle:check`、`./scripts/entropy-check.sh`
- `pre-flight` 每一项后必须附真实输出摘要（如测试通过数、退出码）
- 示例：
  ```yaml
  pre-flight:
    mvn-test: pass       # Tests: 142 run, 0 failures, 0 errors
    checkstyle: pass     # Exit 0
    entropy-check: pass  # 12/12 checks passed
  ```

### 写 test-report.md 的"验收结论"前
- @qa 必须独立重跑三项检查（不复用 @dev 的 pre-flight）
- 每项结论附命令 + 输出片段
- 失败项目必须附完整错误信息 + 定位文件

### 写 dev-log.md 的"修复完成"/"问题解决"前
- 必须附：复现命令 + 修复前失败输出 + 修复后通过输出
- Bug 修复必须带 Red-Green-Refactor 循环证据

### 写 progress.md 的 `[x]` 标记前
- 任务完成标记必须附：commit hash + 关键验证命令退出码

## MUST NOT（禁止）

- 禁止在未运行命令的同一消息内写入 `status: approved` / `status: pending-review`（带 pre-flight pass）
- 禁止使用"应该"、"可能"、"看起来"等不确定词汇在上述文档中做正向声称
- 禁止照搬上次 session 的输出作为本次证据
- 禁止以"子 agent 报告成功"作为最终声称依据，必须独立验证 git diff / 测试输出
- 禁止部分检查（只跑 mvn test 不跑 entropy-check）就标 pre-flight 全 pass

## 执行检查（每次写入上述文档后）
1. 对照 `.claude/skills/verification-before-completion/SKILL.md` 的映射表，确认每条声称都有证据
2. 检查是否出现禁用词汇（应该/可能/看起来/probably）
3. 若状态流转至 approved，回退自查：命令真的跑了吗？输出真的存在吗？
