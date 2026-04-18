---
name: receiving-code-review
description: "@qa changes-requested 后，@dev 结构化接收并处理反馈。把 test-report.md 问题清单拆成最小修复单元，按严重度执行；每项修复必有失败测试 → Green 证据。防止\"顺手修多项\"式混乱返工。"
user-invocable: true
disable-model-invocation: true
argument-hint: "<task-id>"
allowed-tools: "Read Write Edit Bash(mvn *) Bash(git *) Bash(./scripts/*) Bash(./.claude/skills/full-check/scripts/*) Grep Glob"
---

# 接收 Code Review 反馈（receiving-code-review）

## 核心原则

> **一条问题 = 一次 Red-Green = 一次 commit。不合并、不"顺手修"、不跳严重度。**

Code review（`@qa` 打回或人工 PR 评论）会一次给多条反馈。最坏的做法是全部读一遍、开一个大 diff、通通改掉。这会：
- 把多个根因混在一次修复，丢失因果证据
- 违反 Karpathy ③（外科式变更）
- 让 @qa 第二轮验收时分不清哪条 AC 对应哪处修改

本 skill 强制**单问题闭环**。

## 何时用

- `handoff.md` 显示 `status: changes-requested` 且 `to: qa` → 你（@dev）接手
- PR 评论回来多条必改项
- 人工 review 说"这几处要动"

不需要用的场景：
- 仅 1 条反馈（直接走 `systematic-debugging` 即可）
- QA 的 Minor 建议（非阻塞）→ 记到 dev-log.md「后续改进」，不一定当下修

## 输入

必须存在：
- `docs/exec-plan/active/{task-id}/test-report.md` — 含「问题清单」章节
- `docs/exec-plan/active/{task-id}/handoff.md` — status: changes-requested

## 5 步流程（严格顺序）

### 步骤 1：读清问题，拆成修复单元

打开 `test-report.md`「问题清单」表格，按**严重度 + 根因独立性**排序：

```
Critical（阻塞） → Major（阻塞） → Minor（非阻塞，可选修）
```

**拆分规则**：
- 同一根因的多个症状 → **合为 1 个修复单元**（例如 3 个测试都因 null 检查缺失失败 → 1 个单元）
- 不同根因 → 必须拆成多个单元（例如"NPE + 参数校验缺失" = 2 个单元）
- 严重度不同 → 必须拆（不得把 Minor 搭 Major 顺手修）

**产物**：在 `dev-log.md` 追加「修复计划」章节（模板在末尾）。

### 步骤 2：逐单元走 Phase 1 根因调查

对每个单元，**调用 `systematic-debugging` skill**的 Phase 1：
- 读完整 stack trace（不止顶层）
- 复现命令（`mvn test -pl xxx -Dtest=XxxTest#yyy`）
- 多层诊断（adapter / application / domain / infrastructure 哪层出问题）
- 数据流反向追踪，定位**源头层**

在 `dev-log.md` 对应单元下记录 **"WHAT + WHY"** 一句话结论。

> 没有 WHAT+WHY → 不得进入步骤 3。

### 步骤 3：单单元 Red-Green-Commit（循环）

对每个修复单元：

```bash
# 3.1 写失败测试（或修改已有测试使其精确捕获根因）
# 必须看到 RED：
mvn test -pl <module> -Dtest=<TestClass>#<newMethod>
# 断言：Tests run: 1, Failures: 1  — 确认 RED 成立

git add <test-file>
git commit -m "test(<layer>): reproduce <issue-id> failure (Red)"

# 3.2 最小修复（只动根因，不顺手清理相邻代码）
# 3.3 验证 GREEN：
mvn test -pl <module> -Dtest=<TestClass>#<newMethod>
# 断言：Tests run: 1, Failures: 0

# 3.4 全量回归：
mvn test
# 断言：无新增失败

git add <src-file> <test-file>
git commit -m "fix(<layer>): resolve <issue-id> (Green)"
```

**硬约束**：
- 一个单元 = 一次 Red commit + 一次 Green commit（与 dev-build 的 Red/Green 分离规则一致）
- **不允许**把多个单元的修复合并到一次 commit
- 第 3 次修复仍失败 → 质疑架构（走 systematic-debugging Phase 4.5），不得继续堆修复

### 步骤 4：更新 dev-log 与问题清单回写

在 `dev-log.md`「修复计划」章节，为每个单元补上：
- Red commit hash
- Green commit hash
- 回归测试结果（mvn test 总览）

在 `test-report.md`「问题清单」表格的「处理」列，把每条标为：
- `已修复 @<green-commit-hash>`
- 或 `已确认非 bug，见 dev-log.md` （如经 Phase 1 发现是需求误解）

### 步骤 5：三项预飞 + 切回 @qa

```bash
mvn clean test && mvn checkstyle:check -B && ./scripts/entropy-check.sh
```

三项全绿后更新 `handoff.md`：
```yaml
from: dev
to: qa
status: pending-review
pre-flight:
  mvn-test: pass       # Tests: N run, 0 failures, 0 errors
  checkstyle: pass     # Exit 0
  entropy-check: pass  # 12/12 checks passed
  tdd-evidence:
    - issue: <issue-1>
      red: <commit-hash>
      green: <commit-hash>
    - issue: <issue-2>
      red: <commit-hash>
      green: <commit-hash>
summary: |
  第 N 轮返工：修复 Critical × A / Major × B / Minor × C（见 dev-log.md 修复计划）
```

git commit 后告知 Ralph（或用户）可调度 `@qa` 重新验收。

## dev-log.md「修复计划」章节模板

```markdown
## 修复计划（第 N 轮返工，YYYY-MM-DD）

> 源自 test-report.md「问题清单」，按严重度 + 根因独立性拆分。

### 单元 1: <一句话问题描述>
- **关联问题**：test-report 表格 #1, #3（同一 NPE 根因的两个症状）
- **严重度**：Critical
- **Phase 1 结论**：WHAT = OrderItem.price 为 null；WHY = Converter 漏 map null-check 到默认值
- **复现**：`mvn test -pl claude-j-application -Dtest=OrderApplicationServiceTest#should_fail_when_item_price_missing`
- **Red commit**：<hash>
- **Green commit**：<hash>
- **回归**：mvn test N/N passed

### 单元 2: <...>
...

### 不修项（经 Phase 1 判定非阻塞）
- test-report 表格 #5：Minor，建议性改进，已记入本任务「后续改进」段
```

## 红旗（立即 STOP）

出现以下任一信号 → 回到步骤 1 重新拆分：

- 已开始改但 `dev-log.md` 还没写「修复计划」
- 一次 commit 动了 2+ 个不相关文件，无共同根因
- "顺手"清理了与当前单元无关的代码
- 没看到 Red 就提交了 fix
- 某单元修了 3 次仍 Red → 走架构质疑（Phase 4.5）
- Minor 项未经用户确认就占用返工轮次

## 与其他 skill 的关系

| 相关 skill | 关系 |
|-----------|------|
| `systematic-debugging` | 本 skill 的步骤 2 必须调用其 Phase 1 做根因调查 |
| `dev-build` | 本 skill 的 Red/Green commit 分离规则与 dev-build 一致 |
| `verification-before-completion` | 步骤 5 预飞证据由其 SKILL 强制落地 |
| `dispatching-parallel-agents` | **禁止并行修复单元**：Red-Green 必须串行、可追溯 |

## 返工轮次上限

- **@qa-@dev 最多 3 轮**（由 `agent-collaboration.md` 强制）
- 第 3 轮仍未通过 → 终止自动化，`handoff.md` 追加「人工介入请求」，Ralph 停止调度
- 不允许用"改得更多"绕过轮次限制；根因不对时改再多也过不了
