# CLAUDE.md

> 此文件为 Claude Code 的项目入口指令文件。移植时请替换所有 `${VARIABLE}` 占位符。
> 变量定义详见 `docs/templates/project/variables.md`。

## 项目概述
${PROJECT_DESCRIPTION}。

本项目基于 **DDD（领域驱动设计）+ 六边形架构（端口与适配器）**。
技术栈：${LANGUAGE_VERSION} / ${FRAMEWORK} / ${BUILD_TOOL} / ${ORM} / ${MAPPER_TOOL}

## 快速开始
```bash
# Skill 方式（在 Claude Code 会话内）
/ralph 001-feature-name 需求描述           # 全自动交付
/ralph 001-feature-name                    # 续跑（从断点继续）
/ralph 001-feature-name --loop             # Ralph Loop 多会话模式

# Shell 方式（终端直接运行）
./scripts/ralph-auto.sh 001-feature-name "需求描述"
```

## 构建命令
```bash
${CMD_BUILD_ALL}                 # 全量构建
${CMD_RUN_DEV}                   # 开发环境运行（${DB_DEV}）
${CMD_RUN_TESTS}                 # 测试（含架构规则 ${ARCHUNIT_RULE_COUNT} 条）
${CMD_STYLE_CHECK}               # 代码风格
${CMD_QUICK_CHECK}               # 快速检查（编译+单测+风格）
${CMD_ENTROPY_CHECK}             # 架构漂移检测（${ENTROPY_CHECK_COUNT} 项）
```

## 架构
```
adapter → application → domain ← infrastructure → start(assembles all)
```
- **domain**：纯领域模型，禁止框架依赖
- **application**：用例编排（@Service / @Transactional 等），仅依赖 domain
- **infrastructure**：实现 domain 定义的接口（${ORM}）
- **adapter**：REST 入站适配器，仅依赖 application
- **start**：应用启动 + 配置 + 集成测试

## 当前聚合

<!-- 新项目初始为空，随业务开发逐步填入 -->

| 聚合 | 包名 | 说明 |
|------|------|------|
| example | `${PACKAGE_ROOT}.*.example` | 示例聚合（可删除） |

## 关键约定
- 包根 `${PACKAGE_ROOT}`，${LANGUAGE_VERSION} 语法
- 聚合根封装业务不变量，禁止贫血模型，禁止公开 setter
- 对象转换链：Request/Response ↔ DTO ↔ Domain ↔ DO
- DO 对象不得泄漏到 infrastructure 之外
- 表名 `${TABLE_PREFIX}{entity}`，列名 ${COLUMN_CASE}
- 重要决策记录 ADR（`docs/architecture/decisions/`）

## 自动化守护

以下约束由 Hooks + 工具自动执行，**无需手动检查**：

| 守护 | 执行方式 | 触发时机 |
|------|---------|---------|
| 分层依赖 + Domain 纯净 + DO 泄漏 + 语法版本 | `guard-layer.sh` Hook | 每次 Edit/Write |
| 开发准入（需有执行计划 + 评审通过） | `guard-dev-gate.sh` Hook | 写源码时 |
| Agent 写作域边界（dev/qa/architect 越权拦截） | `guard-agent-scope.sh` Hook | 每次 Edit/Write |
| 编译 + 单测 + 风格（提交时阻断） | `post-commit-check.sh` Hook | git commit 后 |
| 架构规则（${ARCHUNIT_RULE_COUNT} 条，含测试命名规范） | 架构测试 | `${CMD_RUN_TESTS}` |
| 代码风格 | 风格检查工具 | `${CMD_STYLE_CHECK}` |
| Pre-commit / Pre-push | Git Hooks | git commit/push |

## Skill 命令

| 命令 | 说明 |
|------|------|
| `/ralph <task-id> <需求>` | **一键交付**：Spec→Review→Build→Verify→Ship |
| `/ralph <task-id>` | 续跑：自动检测当前阶段，从断点继续 |
| `/ralph <task-id> --loop` | Ralph Loop：初始化 + 输出多会话循环命令 |
| `/dev-spec [task-id]` | 单独执行 Spec 阶段 |
| `/architect-review [task-id]` | 单独执行架构评审 |
| `/dev-build [task-id]` | 单独执行 TDD 开发 |
| `/qa-verify [task-id]` | 单独执行 QA 验收 |
| `/qa-ship [task-id]` | 归档已验收任务 |
| `/task-status [task-id]` | 查看任务状态一览 |
| `/full-check` | 三项完整验证 |

## Agent 团队

采用**编排主 Agent + 子 Agent** 模式，杜绝上下文溢出：

```
Ralph（编排主 Agent — 只做决策和调度）
  ├── Agent(@dev)       → Spec / Build（独立上下文）
  ├── Agent(@architect) → Review（独立上下文）
  ├── Agent(@qa)        → Verify（独立上下文）
  └── 主 Agent 直接执行  → Ship（轻量操作）
```

- **Ralph** — 编排调度（详见 `.claude/skills/ralph/SKILL.md`）
- **@dev** — 编码（详见 `.claude/agents/dev.md`）
- **@qa** — 测试与审查（详见 `.claude/agents/qa.md`）
- **@architect** — 设计评审（详见 `.claude/agents/architect.md`）

协作流程、上下文隔离规则、返工限制见 `.claude/rules/agent-collaboration.md`

## 参考文档
- 架构详解：`docs/architecture/overview.md`
- 开发规范：`docs/standards/${LANGUAGE}-dev.md`
- 测试规范：`docs/standards/${LANGUAGE}-test.md`
- 执行计划模板：`docs/exec-plan/templates/`
- 移植指南：`PORTING.md`
