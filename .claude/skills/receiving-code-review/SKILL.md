---
name: receiving-code-review
description: "收到 code review 反馈（CI 失败、QA 打回、PR 评论、人工 review）后，结构化处理多条问题。按严重度拆成最小修复单元，每单元独立 Red-Green-commit，保留因果证据。防止\"一个大 diff 顺手修多项\"的混乱返工。"
user-invocable: true
disable-model-invocation: true
argument-hint: "[review-source-file]"
allowed-tools: "Read Write Edit Bash(git *) Grep Glob"
---

# 接收 Code Review 反馈（receiving-code-review）

## 核心原则

> **一条问题 = 一次 Red-Green = 一次 commit。不合并、不"顺手修"、不跳严重度。**

Code review（CI 失败清单、QA 打回报告、PR 评论、人工 review）通常一次给多条反馈。**最坏的做法**是全部读一遍、开一个大 diff、通通改掉。这会：

- 多个根因混在一次修复 → 丢失"哪条 fix 对应哪个问题"的因果证据
- 违反外科式变更原则（Karpathy ③）
- 让 reviewer 第二轮验收时分不清 commit 对应的问题
- bug 回归时 bisect 无法定位

本 skill 强制**单问题闭环**。

## 何时用

- 多于 1 条 review 反馈 **且** 有 2+ 条需要实际修改
- CI 同时挂了多个测试 / lint 规则
- PR 收到多个必改评论
- 任何"工单式"反馈清单 → 编号 / 分类 / 状态管理

**不需要用**：
- 仅 1 条反馈 → 直接走调试流程即可
- 全是可选建议（Nit） → 记到待办，不一定当下修

## 输入

至少一项：
- **review 原始文档**（CI 日志、test-report、PR 评论列表、review 邮件…）
- 问题应有严重度标签或能被归类为 `Critical / Major / Minor`
- 若 review 没打标签 → 本 skill 第一步就是补上

## 5 步流程（严格顺序）

### 步骤 1：读清问题，拆成修复单元

打开 review 源文档，按 **严重度 + 根因独立性** 排序：

```
Critical（阻塞合并） → Major（阻塞合并） → Minor（非阻塞，可选修）
```

**拆分规则**：
- 同一根因的多个症状 → **合为 1 个修复单元**（例如 3 个测试都因同一 null 检查缺失失败）
- 不同根因 → 必须拆成多个单元（例如"NPE + 参数校验缺失" = 2 个单元）
- 严重度不同 → 必须拆（不得把 Minor 搭 Major 顺手修，否则 Minor 占掉 review 轮次）

**产物**：在本地的修复跟踪文档（例如 `FIX-PLAN.md` 或项目约定的 dev-log）写出「修复计划」表格（模板在末尾）。

### 步骤 2：逐单元做根因调查

对每个单元，在动手改之前回答：

1. **WHAT** — 精确描述问题（不是症状，而是根因）
2. **WHY** — 根因发生的机制
3. **复现命令** — 一条可重复运行、稳定红色的测试命令

如果任一项答不出来 → 先停下来调查（读完整 stack trace、多层追踪、找相似的能跑通的例子），而不是凭直觉改。

> 没有 WHAT+WHY+复现命令 → 不得进入步骤 3。

### 步骤 3：单单元 Red-Green-Commit（循环）

对每个修复单元：

```bash
# 3.1 写失败测试（或修改已有测试使其精确捕获根因）
# 运行测试，必须看到 RED：
<project-test-cmd>   # 例如 pytest / mvn test / npm test / cargo test ...
# 断言：该用例 FAIL 且原因符合根因假设

git add <test-file>
git commit -m "test: reproduce <issue-id> (Red)"

# 3.2 最小修复（只动根因，不顺手清理相邻代码）
# 3.3 再跑同一条测试 —— 必须 GREEN：
<project-test-cmd>
# 3.4 全量回归 —— 不得有新红：
<project-test-cmd-all>

git add <src-files>
git commit -m "fix: resolve <issue-id> (Green)"
```

**硬约束**：
- 一个单元 = **一次 Red commit + 一次 Green commit**（TDD 铁律）
- **不允许**把多个单元的修复合并到一次 commit
- 第 3 次修复仍失败 → 质疑架构、升级给人类 / 组长，不得继续堆修复

### 步骤 4：更新修复跟踪 + 回写 review 状态

- 在修复跟踪文档里为每个单元补上 Red / Green 的 commit hash、回归测试总览
- 在 review 源文档（若可编辑）的每条问题旁标注：
  - `已修复 @<green-commit-hash>`
  - `已确认非 bug，理由：<...>`（经根因调查判定）
  - `Minor 延后，理由：<...>`（Minor 项明确不在本轮修）

### 步骤 5：独立重跑质量门 + 重新提交 review

不要信任第一轮 review 里引用的"已经跑过了" —— 第二轮必须独立重跑项目的质量门（测试 / lint / 类型检查 / 架构检查等），附退出码或通过数：

```bash
<project-quality-gates>   # 例如 ci local、pre-push、make verify、npm run verify
```

全绿后通知 reviewer（PR 评论、重新请求 review、更新工单状态等）。附带：
- 每个已修单元的 Red / Green commit 链接
- 回归测试总览
- 延后不修的 Minor 项清单 + 原因

## 修复计划文档模板

```markdown
## 修复计划（第 N 轮返工，YYYY-MM-DD）

> 来源：<review 源文档/URL>
> 按严重度 + 根因独立性拆分。

### 单元 1: <一句话问题描述>
- **关联反馈**：review #1, #3（同一 NPE 根因的两个症状）
- **严重度**：Critical
- **WHAT**：<根因>
- **WHY**：<机制>
- **复现**：`<test-cmd -k ...>`
- **Red commit**：<hash>
- **Green commit**：<hash>
- **回归**：全量测试 N/N passed

### 单元 2: <...>
...

### 不修项（经根因调查判定非阻塞）
- review #5：Minor，建议性改进，已记入「后续改进」
```

## 红旗（立即 STOP）

出现以下任一信号 → 回到步骤 1 重新拆分：

- 已开始改代码，但修复计划文档还没写
- 一次 commit 动了 2+ 个不相关文件，无共同根因
- "顺手"清理了与当前单元无关的代码（类型注解/格式/命名/死代码）
- 没看到 Red 就提交了 fix
- 某单元修了 3 次仍 Red → 走架构质疑，升级而不是继续改
- Minor 项未经确认就占用 review 轮次

## 与其他实践的关系

| 关联 | 关系 |
|-----|------|
| TDD Red-Green-Refactor | 步骤 3 就是 TDD 的循环；本 skill 强制每个 review 单元都走一次 |
| 系统化调试（root cause first） | 步骤 2 = "先根因后动手"；无 WHAT+WHY 禁止进入步骤 3 |
| 完成前举证（verification-before-completion） | 步骤 5 重跑质量门属于举证范畴 |
| 并行调度决策 | **禁止并行修复单元**：Red-Green 必须串行、可追溯 |

## 返工轮次上限

review-dev 往返通常有上限（团队约定或工作流规定）。超限前必须：

- 每轮后写清楚"已修什么 / 延后什么 / 有什么新发现"
- 第 N-1 轮仍未通过 → 主动升级（人类介入、改变方案、拆任务），不要靠"改得更多"绕过上限
- 根因不对时改再多也过不了
