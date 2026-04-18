---
name: ralph
description: "需求交付入口。主编排 Agent 调度 @dev/@architect/@qa 子 Agent 完成全流程。每阶段独立上下文，杜绝上下文溢出。"
user-invocable: true
disable-model-invocation: true
argument-hint: "<task-id> [需求描述] [--skip-review] [--dev-only] [--loop]"
allowed-tools: "Read Write Edit Glob Grep Bash(mvn *) Bash(./scripts/*) Bash(./.claude/skills/ralph/scripts/*) Bash(./.claude/skills/full-check/scripts/*) Bash(mkdir *) Bash(cp *) Bash(mv *) Bash(ls *) Bash(git *) Bash(cat *) Bash(echo *) Agent"
---

<!--
  脚本位置：
  - ralph 自身脚本在 .claude/skills/ralph/scripts/（ralph-init.sh / ralph-loop.sh / ralph-auto.sh）
  - 验证脚本在 .claude/skills/full-check/scripts/（entropy-check.sh / quick-check.sh）
  - scripts/ 下保留符号链接以兼容旧路径（./scripts/ralph-*.sh 等）
-->


# Ralph — 需求交付编排器

## 核心架构

Ralph 是**编排主 Agent**，自身不执行业务代码编写/测试，而是：
1. 解析输入、判断阶段、做决策
2. 通过 **Agent 工具** 调度子 Agent 执行每个阶段
3. 每次子 Agent 调用在**独立上下文**中运行，不受前序阶段的上下文占用影响
4. 子 Agent 完成后，主 Agent 读取产出物验证结果，决定是否进入下一阶段

```
Ralph（编排主 Agent）
  ├── Agent(@dev)  → Spec 阶段
  ├── Agent(@architect) → Review 阶段
  ├── Agent(@dev)  → Build 阶段
  ├── Agent(@qa)   → Verify 阶段
  └── 主 Agent 直接执行 → Ship 阶段（轻量操作）
```

---

## 输入解析

从 `$ARGUMENTS` 中解析：
- **TASK_ID**：第一个词（如 `003-user-management`）
- **REQUIREMENT**：除 task-id 和 flags 外的剩余文字
- **Flags**：
  - `--skip-review` — 跳过架构评审
  - `--dev-only` — 只到 Build 完成，不执行 QA 和 Ship
  - `--loop` — 输出 `ralph-loop.sh` 终端命令（多会话模式），而非在当前会话执行

## 模式路由

```
有需求描述 → 全自动模式（从 Spec 开始）
无需求描述 + 任务已存在 → 续跑模式（从当前阶段继续）
--loop → Ralph Loop 模式（初始化 progress.md + 输出终端命令）
```

---

## 模式 A：全自动模式（有需求描述）

触发条件：`/ralph 003-user-management 实现用户注册、登录、个人信息管理`

### 编排流程

主 Agent 按以下顺序调度子 Agent，每阶段完成后**验证产出物**再进入下一阶段。

#### 阶段 1: Spec → 调度 @dev 子 Agent

**跳过条件**：`docs/exec-plan/active/${TASK_ID}/requirement-design.md` 已存在且内容充实。

**主 Agent 准备**：
```bash
echo "dev" > .claude-current-role
mkdir -p docs/exec-plan/active/${TASK_ID}/
```

**启动子 Agent**（使用 Agent 工具）：
```
prompt:
你是 @dev 开发工程师，执行 Spec 阶段。

任务 ID：${TASK_ID}
需求描述：${REQUIREMENT}
任务目录：docs/exec-plan/active/${TASK_ID}/

请严格按照 .claude/skills/dev-spec/SKILL.md 的步骤执行：
1. 阅读参考文档（CLAUDE.md、java-dev.md、模板、schema.sql、已有聚合代码）
2. 领域建模（聚合根、实体、值对象、端口）
3. 填写 requirement-design.md（领域分析、API、DDL）
4. 填写 task-plan.md（按 domain→app→infra→adapter 拆解）
5. 创建 dev-log.md + handoff.md（to: architect, status: pending-review）
6. git commit 所有产出物

完成后输出简要摘要。
```

**主 Agent 验证**：
- 检查 `docs/exec-plan/active/${TASK_ID}/requirement-design.md` 存在且非空
- 检查 `docs/exec-plan/active/${TASK_ID}/handoff.md` 存在且 status = pending-review
- 若缺失 → 输出错误信息，终止流程

---

#### 阶段 2: Review → 调度 @architect 子 Agent

**跳过条件**：`--skip-review`。跳过时主 Agent 直接修改 handoff.md status 为 `approved`。

**主 Agent 准备**：
```bash
echo "architect" > .claude-current-role
```

**启动子 Agent**（使用 Agent 工具）：
```
prompt:
你是 @architect 架构评审师，执行 Review 阶段。

任务 ID：${TASK_ID}
任务目录：docs/exec-plan/active/${TASK_ID}/

请严格按照 .claude/skills/architect-review/SKILL.md 的步骤执行：
1. 阅读 requirement-design.md 和 handoff.md
2. 交叉验证（架构文档、已有 ADR、CLAUDE.md 聚合列表、已有聚合代码模式）
3. 运行 ./scripts/entropy-check.sh 确认基线
4. 按 9 项清单评审，在 requirement-design.md 追加「架构评审」章节
5. 更新 handoff.md：approved 或 changes-requested
6. 如有需要，创建 ADR 文件
7. git commit

注意：发现严重设计问题时，自行修正 requirement-design.md 后再标记 approved。
完成后输出评审结论和关键发现。
```

**主 Agent 验证**：
- 读取 `handoff.md` 检查 status
- 若 `approved` → 进入阶段 3
- 若 `changes-requested` → 输出评审意见，提示用户修改后续跑 `/ralph ${TASK_ID}`

---

#### 阶段 3: Build → 调度 @dev 子 Agent

**主 Agent 准备**：
```bash
echo "dev" > .claude-current-role
```

**启动子 Agent**（使用 Agent 工具）：
```
prompt:
你是 @dev 开发工程师，执行 Build 阶段（TDD 开发）。

任务 ID：${TASK_ID}
任务目录：docs/exec-plan/active/${TASK_ID}/

请严格按照 .claude/skills/dev-build/SKILL.md 的步骤执行：

前置：阅读 requirement-design.md + task-plan.md + java-dev.md + java-test.md + 已有聚合代码

开发顺序（严格遵守）：
1. Domain 层 — 值对象、实体、聚合根、Repository 端口 + 纯单元测试 → git commit
2. Application 层 — 命令、DTO、组装器、应用服务 + Mockito 测试 → git commit
3. Infrastructure 层 — DO、Mapper、转换器、Repository 实现 + SpringBootTest → git commit
4. Adapter 层 — 请求/响应、Controller + WebMvcTest → git commit
5. Start — DDL 写入 schema.sql → git commit
6. 三项验证：mvn clean test && mvn checkstyle:check -B && ./scripts/entropy-check.sh
   失败则修复后重跑，直到全部通过
7. 更新 task-plan.md + dev-log.md + handoff.md（to: qa, pre-flight: all pass）→ git commit

关键约束：
- Java 8（禁止 var/records/text blocks/List.of）
- Domain 纯 Java，禁止 Spring
- 依赖方向：adapter→application→domain←infrastructure
- 转换链：Request/Response ↔ DTO ↔ Domain ↔ DO
- 测试命名：should_xxx_when_yyy

完成后输出：开发摘要、测试数量、三项验证结果。
```

**主 Agent 验证**：
- 读取 `handoff.md` 检查 status = pending-review, to = qa
- 读取 pre-flight 确认三项检查均 pass
- 运行 `mvn test -q` 做独立编译验证（快速，非全量）
- 若验证失败 → 重新调度 @dev 子 Agent 修复

若 `--dev-only` → 输出完成摘要，流程结束。

---

#### 阶段 4: Verify → 调度 @qa 子 Agent

**主 Agent 准备**：
```bash
echo "qa" > .claude-current-role
```

**启动子 Agent**（使用 Agent 工具）：
```
prompt:
你是 @qa 验收工程师，执行 Verify 阶段。

任务 ID：${TASK_ID}
任务目录：docs/exec-plan/active/${TASK_ID}/

请严格按照 .claude/skills/qa-verify/SKILL.md 的步骤执行：

1. 阅读 handoff.md 确认 from: dev, to: qa, status: pending-review
2. 阅读 requirement-design.md 了解需求设计
3. 独立重跑三项检查（不信任 @dev 的 pre-flight 标记）：
   mvn clean test && mvn checkstyle:check -B && ./scripts/entropy-check.sh
4. 编写 test-case-design.md（七节：分层测试 + 集成测试 + 代码审查 + 风格检查）
5. 编写 start 模块集成测试（@SpringBootTest + MockMvc，全链路穿透 H2）
6. 代码审查（聚合设计、值对象不可变、转换链、Controller 无逻辑）
7. 编写 test-report.md（测试结果 + 审查结果 + 问题清单 + 验收结论）
8. 更新 handoff.md：approved 或 changes-requested
9. git commit

测试命名规范：should_xxx_when_yyy
问题严重级别：Critical（必须修复）/ Major（必须修复）/ Minor（可后续修复）

完成后输出：验收结论、测试数量、问题清单。
```

**主 Agent 验证**：
- 读取 `handoff.md` 检查 status
- 若 `approved` → 进入阶段 5
- 若 `changes-requested` → 读取 test-report.md 中的问题清单，调度 @dev 子 Agent 修复，然后重新调度 @qa 验证（最多 3 轮）

**返工循环**（最多 3 轮）：
```
@qa changes-requested
  → 主 Agent 读取问题清单
  → 调度 @dev 子 Agent 修复（传入具体问题列表）
  → 调度 @qa 子 Agent 重新验收
  → 若仍 changes-requested → 轮次+1
  → 超过 3 轮 → 终止，输出问题清单，请求人类介入
```

---

#### 阶段 5: Ship → 主 Agent 直接执行

Ship 是轻量操作，主 Agent 直接执行，无需子 Agent：

1. 读取 `handoff.md` 确认 status = approved, to = qa
2. 读取 `test-report.md` 确认含"验收通过"
3. `mv docs/exec-plan/active/${TASK_ID}/ docs/exec-plan/archived/${TASK_ID}/`
4. 检查是否有新聚合（对比 domain 层目录与 CLAUDE.md），有则更新 CLAUDE.md
5. `git commit`
6. 清理角色标记：`rm -f .claude-current-role`

### 完成输出
```
✅ 任务 ${TASK_ID} 交付完成！

📋 交付摘要：
- Spec: ✅ 需求设计完成
- Review: ✅ 架构评审通过
- Build: ✅ 代码开发完成（X tests passed）
- Verify: ✅ QA 验收通过
- Ship: ✅ 已归档

📁 归档位置：docs/exec-plan/archived/${TASK_ID}/
```

执行 `git log --oneline -20` 展示完整提交历史。

---

## 模式 B：续跑模式（无需求描述，任务已存在）

触发条件：`/ralph 003-user-management` 或 `/ralph 003-user-management --dev-only`

主 Agent 检测当前阶段，**从该阶段开始调度子 Agent**：

1. 读取 `docs/exec-plan/active/${TASK_ID}/` 下的文档，判断当前阶段：
   - 无 requirement-design.md → 提示用户提供需求描述
   - 有 requirement-design.md 但无 handoff.md → 调度 @dev 完成 Spec 收尾
   - handoff.md status: pending-review, to: architect → 从 Review 开始调度
   - handoff.md status: approved, to: architect → 从 Build 开始调度
   - handoff.md status: pending-review, to: qa → 从 Verify 开始调度
   - handoff.md status: approved, to: qa → 直接执行 Ship
   - handoff.md status: changes-requested → 读取 test-report.md，调度 @dev 修复后重新进入流程
2. 读取 `progress.md`（如有）显示进度概览
3. 从检测到的阶段开始，按模式 A 的顺序依次调度后续阶段

---

## 模式 C：Ralph Loop 模式（--loop）

触发条件：`/ralph 003-user-management --loop` 或 `/ralph 003-user-management --loop dev`

用于超大型需求，每次迭代使用全新 Claude Code 进程（比子 Agent 更彻底的上下文隔离）。

1. 从 `--loop` 后提取 AGENT（默认 `dev`）
2. 验证任务目录 + requirement-design.md + task-plan.md 存在
3. 初始化 progress.md（如尚未存在）：
   ```bash
   ./.claude/skills/ralph/scripts/ralph-init.sh docs/exec-plan/active/${TASK_ID}
   # 或兼容路径：./scripts/ralph-init.sh docs/exec-plan/active/${TASK_ID}
   ```
4. 显示当前状态（已完成/未完成任务数 + 下一个待办）
5. 输出终端命令供用户执行：
   ```
   ./.claude/skills/ralph/scripts/ralph-loop.sh ${AGENT} docs/exec-plan/active/${TASK_ID}
   # 或兼容路径：./scripts/ralph-loop.sh ${AGENT} docs/exec-plan/active/${TASK_ID}
   ```

---

## 子 Agent 调度规则

### 调度方式
使用 **Agent 工具**，每个子 Agent 调用获得**独立上下文窗口**：

```python
# 伪代码 — 实际使用 Agent 工具
Agent(
    description="@dev Spec 阶段",
    prompt="...",  # 包含完整的阶段指令
)
```

### 上下文隔离保证
- 每个子 Agent 是独立调用，不继承主 Agent 的代码上下文
- 子 Agent 通过读取文件（requirement-design.md、handoff.md 等）恢复上下文
- 所有状态通过 **文件系统**（exec-plan 目录）和 **git** 传递，不通过内存

### 错误处理
- 子 Agent 返回后，主 Agent **必须验证产出物**再继续
- 子 Agent 失败（产出物缺失/不合规）→ 主 Agent 可重试一次，附上错误信息
- 重试仍失败 → 终止流程，输出诊断信息，请求人类介入

### 角色标记
每次调度子 Agent 前，主 Agent 写入角色标记文件：
```bash
echo "${ROLE}" > .claude-current-role
```
这确保 `guard-agent-scope.sh` Hook 正确执行写作域校验。

---

## 关键约束
- Java 8（禁止 var/records/text blocks/List.of）
- 依赖方向：adapter→application→domain←infrastructure
- Domain 纯 Java，禁止 Spring
- 转换链：Request/Response ↔ DTO ↔ Domain ↔ DO
- DO 不泄漏到 infrastructure 之外
- 每阶段完成必须 git commit
- 返工循环最多 3 轮，超出请求人类介入
