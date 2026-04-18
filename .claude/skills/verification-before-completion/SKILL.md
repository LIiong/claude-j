---
name: verification-before-completion
description: "任何声称'通过/完成/修复/无问题'之前必须运行验证命令并附证据。适用于 @dev/@architect/@qa/Ralph 在写入 handoff/test-report/dev-log 或口头汇报结果时。"
user-invocable: false
disable-model-invocation: true
allowed-tools: "Read Bash(mvn *) Bash(./scripts/*) Bash(./.claude/skills/full-check/scripts/*) Bash(git *)"
---

# 声称前必须验证（Verification Before Completion）

## 核心原则

> **证据先于声称（Evidence before claims）。**
>
> 在本消息中未运行验证命令前，不得声称任何"通过/完成/修复/无问题"。

违反字面规则 = 违反精神。**没有例外。**

## 铁律（Iron Law）

```
NO COMPLETION CLAIMS WITHOUT FRESH VERIFICATION EVIDENCE
```

未在本消息中看到验证命令的输出 → 不得声称通过。
上一次运行的结果 ≠ 当前证据。

## 门控流程（所有"完成"声称前）

```
1. IDENTIFY: 证明这个声称的命令是什么？
2. RUN:      在本消息中完整执行该命令（不截取、不复用历史）
3. READ:     读取完整输出 + 退出码 + 失败计数
4. VERIFY:   输出是否真的支持该声称？
             - 否 → 如实陈述真实状态 + 证据
             - 是 → 声称 + 附证据
5. ONLY THEN 才能做出声称
```

跳过任一步 = 说谎，不是验证。

## 声称 → 证据映射表（claude-j 本土版）

| 声称 | 必需证据 | 不充分 |
|------|---------|--------|
| 单测通过 | `mvn test` 输出 `BUILD SUCCESS` + 测试数 0 failures | 上次结果 / "应该能过" |
| 架构合规 | `mvn test` 含 ArchUnit 14 条全过 | 仅编译通过 |
| 代码风格合规 | `mvn checkstyle:check` 退出码 0 | 仅 IDE 提示 |
| 熵检查通过 | `./scripts/entropy-check.sh` 退出码 0 | Hook 未报警 ≠ entropy 通过 |
| 三项预飞通过 | 3 个命令 3 段输出齐全 | 只报"全绿" |
| Bug 已修复 | 重新运行复现命令 + 原失败用例现在通过 | 改了代码 / "我改了问题点" |
| 回归测试有效 | Red-Green 循环：撤销修复 → 测试失败 → 恢复 → 测试通过 | "我写了一个测试" |
| Agent 已完成 | `git diff` + `git log` 确认提交 | 子 agent 自述"成功" |
| 需求已满足 | 对照验收条件逐项 ✅ + 证据 | 测试过 ≠ 需求满足 |

## 禁用词汇（立即 STOP）

出现以下任一措辞 → **停下来，先跑验证**：

- "应该能过" / "should pass"
- "看起来正确" / "looks correct"
- "可能没问题" / "probably fine"
- "代码改好了" （未跑测试）
- "完成" / "Done" / "Great!" / "Perfect!"
- 任何暗示成功但未附证据的措辞
- 准备 commit / 更新 handoff status 前未跑验证

## 理性化预防表

| 借口 | 真相 |
|------|------|
| "我刚才跑过了" | 再跑一次。每条消息独立取证 |
| "我很有信心" | 信心 ≠ 证据 |
| "就这一次" | 没有例外 |
| "Checkstyle 过了所以 mvn test 也过" | Checkstyle ≠ 编译 ≠ 测试 |
| "子 agent 报告成功了" | 独立验证 git diff |
| "部分检查够了" | 部分 = 未知 |
| "我累了" | 疲倦 ≠ 借口 |
| "换个说法就不算声称" | 看精神不看字面 |

## claude-j 专项规则

### @dev 在 Build 阶段提交 QA 前
在 `handoff.md` 的 `pre-flight:` 字段写入三项结果前，**必须**：
```bash
mvn clean test -B
mvn checkstyle:check -B
./scripts/entropy-check.sh
```
在 `handoff.md` 的 `summary` 里附：
```
pre-flight:
  mvn-test: pass       # Tests run: 142, Failures: 0, Errors: 0
  checkstyle: pass     # Exit 0
  entropy-check: pass  # 12/12 checks passed
```

### @qa 在 Verify 阶段声称验收通过前
- **独立重跑三项**（不信任 @dev 的 pre-flight 标记）
- test-report.md 的"验收结论"章节必须附每项命令的输出摘要

### Ralph 主 Agent 在推进阶段前
- 子 agent 返回"完成"后，Ralph 必须 `git diff HEAD~1` 确认实际有改动
- 读 handoff.md 时验证 pre-flight 字段是否都带证据

### @architect 声称设计评审通过前
- 运行 `./scripts/entropy-check.sh` 确认当前基线无违规
- 在评审结论附该命令的退出码

## 集成点

- `@dev` 工作流第 8 步（三项验证）、第 11 步（通知 QA）→ 强制本 skill
- `@qa` 工作流第 3 步（自动化验证）、第 10 步（验收通过）→ 强制本 skill
- `@architect` 工作流第 3 步（架构基线检查）→ 强制本 skill
- Ralph 在阶段切换前 → 强制本 skill
- 规则文件 `.claude/rules/verification-gate.md` 自动加载（operating on handoff/test-report/dev-log 时）

## 底线

**没有验证捷径。**

跑命令。读输出。然后再声称。

这条是不可协商的。
