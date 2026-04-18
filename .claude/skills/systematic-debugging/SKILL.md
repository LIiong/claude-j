---
name: systematic-debugging
description: "遇到测试失败/缺陷/意外行为时强制走 4 阶段根因调查。适用于 @dev 处理 QA 问题清单、测试失败排查、production 报障等。禁止先修再调查。"
user-invocable: false
disable-model-invocation: true
allowed-tools: "Read Bash(mvn *) Bash(git *) Bash(./scripts/*) Bash(./.claude/skills/full-check/scripts/*) Grep Glob"
---

# 系统化调试（Systematic Debugging）

## 核心原则

> **先找根因，再动手修。症状修复 = 失败。**

随机试错浪费时间、制造新 bug；快速补丁掩盖真正问题。

## 铁律（Iron Law）

```
NO FIXES WITHOUT ROOT CAUSE INVESTIGATION FIRST
```

Phase 1 未完成 → 不得提出修复方案。

## 何时使用

**任何技术问题都适用**：
- 测试失败（@qa changes-requested 中报告的任何问题）
- 生产 bug
- 意外行为
- 性能问题
- 构建失败
- 跨层集成异常（adapter ↔ application ↔ domain ↔ infrastructure）

**越紧急越要用**：
- 时间压力下（紧急情况下猜测最诱人，但错误代价最大）
- 感觉"一个小改动就能修"时
- 已经尝试多次修复未成功时
- 前一次修复失败后

**不要跳过的场景**：
- 问题看起来简单（简单 bug 也有根因）
- 赶时间（系统化比乱试更快）

## 四阶段铁轨

**必须按顺序完成，禁止跳阶段。**

---

### Phase 1：根因调查

任何修复尝试之前：

#### 1.1 仔细读错误信息
- 不要跳过 stack trace
- 读完整的异常链（Caused by ...）
- 记下行号、文件路径、异常类、错误码

#### 1.2 稳定复现
- 能可靠触发吗？
- 精确重现步骤是什么？
- 每次都发生还是偶发？
- 无法复现 → 继续收集数据，不要猜

#### 1.3 检查最近变更
```bash
git log --oneline -20
git diff HEAD~5 -- <suspect-area>
```
- 什么改动可能导致？
- 新依赖、配置变更、环境差异？

#### 1.4 多层系统诊断（claude-j 专用）

claude-j 是六边形架构，问题可能在任一边界。**为每个边界插诊断日志收集证据，再定位哪一层失败：**

```
Adapter boundary（入口）
  └─ 入参：Request/Response、@Valid 错误
  └─ 出参：HTTP 状态码、Result<T> 结构
Application boundary（编排）
  └─ 入参：Command/DTO
  └─ 出参：是否调对 Repository、事务边界
Domain boundary（领域规则）
  └─ 入参：值对象、实体状态
  └─ 不变量：是否抛 BusinessException
Infrastructure boundary（持久化）
  └─ 入参：Domain → DO 转换
  └─ 出参：MyBatis SQL 参数、DB 返回
```

**示例诊断脚本**（临时调试用，修复后必删）：
```java
// Adapter 层
log.debug("[adapter-in] request={}", request);
log.debug("[adapter-out] response={}, status={}", response, status);

// Application 层
log.debug("[app-in] command={}", command);
log.debug("[app-out] aggregate-state={}", aggregate);

// Infrastructure 层
log.debug("[infra-in] domain={}", domain);
log.debug("[infra-sql] do={}", doObj);
log.debug("[infra-out] saved-id={}", id);
```

跑一次 → 看哪一层的入参正常、哪一层的出参异常 → 那一层就是嫌疑人。

#### 1.5 数据流反向追踪
当错误在调用栈深处时，从异常点向上追：
- 坏值起源于哪？
- 谁把坏值传进来？
- 一路往上直到源头
- **在源头修，不在症状处修**

**Phase 1 出口条件**：能用一句话回答"WHAT 是什么问题 + WHY 为什么发生"。

---

### Phase 2：模式分析

动手改之前：

#### 2.1 找工作的例子
- 同 codebase 里有没有类似能跑通的代码？（如已实现的 shortlink 聚合）
- 能跑通的和坏掉的差在哪？

#### 2.2 对照参考实现
- 如果是实现某个已知模式（如 Repository、Converter），读**整个**参考实现
- 不要跳读，每行都看
- 先完全理解，再动手

#### 2.3 列出所有差异
- 工作的 vs 坏的，每一处不同都列出
- 不要假设"这个不可能有关系"

#### 2.4 理解依赖
- 还需要哪些组件？
- 哪些配置、环境、假设？

---

### Phase 3：假设与测试

科学方法：

#### 3.1 形成单一假设
- 写下来：**"我认为 X 是根因，因为 Y"**
- 具体，不要模糊

#### 3.2 最小测试
- 做能验证假设的最小改动
- 一次只动一个变量
- 不要"顺手把其他也修了"

#### 3.3 测试前验证
- 假设成立？→ Phase 4
- 假设不成立？→ 形成新假设，不要"在旧修复上加新修复"

#### 3.4 不知道时承认
- 说"我不理解 X"
- 不要装懂
- 求助、研究

---

### Phase 4：实施修复

#### 4.1 创建失败测试用例
- 最小可复现
- 自动化测试（优先 JUnit 5）
- **必须先看到 RED**（参考 `.claude/skills/verification-before-completion/SKILL.md` 的 Red-Green-Refactor）

#### 4.2 单一修复
- 只改根因
- 一次一个改动
- 不要"既然来了顺便改"

#### 4.3 验证修复
- 新测试从 RED → GREEN？
- 其他测试有没有被破坏？`mvn test` 全量
- 原症状彻底消失？

#### 4.4 修复失败时
- **STOP**
- 数数：这是第几次尝试？
- < 3 次 → 回 Phase 1，用新信息重新调查
- **≥ 3 次 → 停下来质疑架构**（Phase 4.5）
- 不要在失败修复上堆新修复

#### 4.5 3+ 次失败 = 架构问题

以下模式说明架构有问题，不是 bug：
- 每次修复在不同位置发现新的共享状态/耦合
- 修复需要"大规模重构"才行
- 每个修复制造新症状

**停下来，问根本性问题**：
- 这个模式本身是否正确？
- 是不是在"惯性中硬扛"？
- 应该重构架构 vs. 继续补症状？

**和人类伙伴讨论再动手**。这不是失败的假设，是错误的架构。

---

## 红旗（立即 STOP）

如果发现自己想：
- "先快修一下，之后再查"
- "随便改 X 看看能不能通过"
- "一次改几个地方，跑测试"
- "跳过写测试，手工验一下"
- "应该是 X，修了看看"
- "虽然不完全懂，但可能这样行"
- "再试一次"（已经 2+ 次失败后）

**全部 = STOP，回 Phase 1**。

## 理性化借口表

| 借口 | 真相 |
|------|------|
| "问题简单不用走流程" | 简单 bug 也有根因，流程对简单 bug 也很快 |
| "紧急没时间走流程" | 系统化调试比瞎试快得多 |
| "先试一下再查" | 第一次修复定基调，一开始就要做对 |
| "修好后再补测试" | 未测试的修复不稳定，测试先行才证明 |
| "一次改几处省时间" | 分不清哪个起作用，还制造新 bug |
| "参考太长我适配一下" | 部分理解 = bug 保证，完整读 |
| "我看到问题了直接修" | 看到症状 ≠ 懂根因 |
| "再来一次"（2+ 次失败后） | 3+ 次失败 = 架构问题，不是 bug |

## claude-j 集成点

### @dev 处理 QA changes-requested 时
1. 读 `test-report.md` 问题清单
2. **必须先走 Phase 1**：逐条问题读 stack trace、定位源头层、追踪数据流
3. 不得在未完成 Phase 1 前编辑 src/main/java/
4. 每个问题的修复必须带：失败测试 → 修复 → 测试通过的证据（参考 verification-before-completion）
5. 若同一 bug 第 3 次修复仍失败 → 在 `dev-log.md` 记录"架构质疑"，通过 handoff 升级到人类介入

### @qa 发现多个相关失败时
- 在 `test-report.md` 问题清单中标注**疑似共同根因**
- 给 @dev 的 handoff 附"建议先走 systematic-debugging Phase 1"

### 返工 3 轮限制与本 skill 的关系
- **返工 3 轮**：QA-Dev 往返次数上限（@qa 验收 3 次仍不过 → 人工）
- **修复 3 次**：同一 bug 内 @dev 的修复尝试次数（3 次失败 → 质疑架构）
- 两者互相加强：单一 bug 修 3 次失败可能直接消耗返工轮数

## 快速参考

| Phase | 关键行为 | 成功标志 |
|-------|---------|---------|
| 1. 根因 | 读错误、复现、查变更、多层诊断 | 能说清 WHAT + WHY |
| 2. 模式 | 找工作示例、对照参考、列差异 | 列出所有差异 |
| 3. 假设 | 单一假设、最小测试 | 假设被证实/证伪 |
| 4. 实施 | 失败测试、单一修复、验证 | bug 消失、测试全绿 |

## 真正无根因的情况

如果系统化调查后确定是环境、时序、外部因素：
1. 记录已调查范围到 `dev-log.md`
2. 实施合适的处理（重试、超时、友好错误）
3. 加监控/日志便于未来排查

**但：95% 的"无根因"其实是调查不彻底。**

## 底线

**根因先于修复。**

先读错误，再复现，再诊断各层，再提假设，再动手。
