# 开发日志 — {task-id}-{task-name}

## 问题记录

<!--
每条问题必须四段齐全（Issue / Root Cause / Fix / Verification），不得只记"决策"。
理由：违反 VERIFICATION 铁律的举证精神——没有 Verification 行的条目等于未证实。
Build 阶段 handoff 前请 self-check 所有条目；若缺 Verification 行 → 不得提交 handoff。
-->

### 1. {问题标题}
- **Issue**：{复现症状 / 报错片段（命令 + stderr 前 3 行或测试失败断言）}
- **Root Cause**：{根因，不是症状；若属于设计选择，写明排除了哪些方案}
- **Fix**：{具体改了哪个文件哪一行 / 或"采取 X 方案"}
- **Verification**：`{验证命令}` → `{关键输出摘要}`（例：`mvn test` → `Tests: 52, Failures: 0, Errors: 0`）

<!-- 复制上方模板继续添加 -->

## 变更记录

<!-- 与原设计（requirement-design.md）不一致的变更 -->
<!-- 格式：变更内容 + 变更原因 -->
- 无与原设计不一致的变更。
