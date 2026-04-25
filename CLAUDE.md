# CLAUDE.md

## 项目概述
Java 电商订单系统，演示 DDD + 六边形架构最佳实践。
技术栈：Java 8 / Spring Boot 2.7.18 / Maven / MyBatis-Plus 3.5.5 / MapStruct 1.5.5

## 快速开始
```bash
# Skill 方式（在 Claude Code 会话内）
/ralph 003-feature-name 需求描述           # 全自动交付
/ralph 003-feature-name                    # 续跑（从断点继续）
/ralph 003-feature-name --loop             # Ralph Loop 多会话模式

# Shell 方式（终端直接运行，多会话 Ralph Loop）
./scripts/ralph-auto.sh 003-feature-name "需求描述"
```

## 构建命令
```bash
mvn clean install                                                      # 全量构建
mvn spring-boot:run -pl claude-j-start -Dspring-boot.run.profiles=dev  # 开发环境（H2）
mvn test                                                               # 测试（含 ArchUnit 14 条架构规则）
mvn checkstyle:check                                                   # 代码风格
./scripts/quick-check.sh                                               # 快速检查（编译+单测+风格）
./scripts/entropy-check.sh                                             # 架构漂移检测（12 项）
```

## 架构
```
adapter → application → domain ← infrastructure ← start(assembles all)
```
- **domain**：纯领域模型，禁止 Spring/框架依赖
- **application**：用例编排（@Service, @Transactional），仅依赖 domain
- **infrastructure**：实现 domain 定义的接口（MyBatis-Plus, @Repository）
- **adapter**：REST 入站适配器，仅依赖 application
- **start**：Spring Boot 启动 + 配置 + 集成测试

## 当前聚合

| 聚合 | 包名 | 说明 |
|------|------|------|
| shortlink | `com.claudej.*.shortlink` | 短链服务 |
| link | `com.claudej.*.link` | 链接管理 |
| order | `com.claudej.*.order` | 电商订单 |
| user | `com.claudej.*.user` | 用户管理 |
| coupon | `com.claudej.*.coupon` | 优惠券服务 |
| cart | `com.claudej.*.cart` | 质物车服务 |
| auth | `com.claudej.*.auth` | 认证服务 |
| product | `com.claudej.*.product` | 商品服务 |

## 关键约定
- 包根 `com.claudej`，Java 8 语法（禁止 var/records/text blocks/List.of）
- 聚合根封装业务不变量，禁止贫血模型，禁止公开 setter
- 对象转换链：Request/Response ↔ DTO ↔ Domain ↔ DO（MapStruct）
- DO 对象不得泄漏到 infrastructure 之外
- 表名 `t_{entity}`，列名 snake_case
- 重要决策记录 ADR（`docs/architecture/decisions/`）

## 三条心智铁律（Iron Laws）
这三条约束跨越所有阶段与所有角色，优先级高于任何"效率/紧急"借口：

```
1. TDD          —  NO PRODUCTION CODE WITHOUT A FAILING TEST FIRST
2. VERIFICATION —  NO COMPLETION CLAUMS WITHOUT FRESH VERIFICATION EVIDENCE
3. DEBUG        —  NO FIXES WITHOUT ROOT CAUSE INVESTIGATION FIRST
```

- TDD 细则 + 11 项反模式对照：`docs/standards/java-test.md`
- 举证规则 + 禁用词汇：`.claude/skills/verification-before-completion/SKILL.md` + `.claude/rules/verification-gate.md`
- 4 阶段调试铁轨：`.claude/skills/systematic-debugging/SKILL.md`

## 四项心智原则（Karpathy Guidelines）

铁律管"绝对红线"，原则管"选择时默认往哪偏"。所有任务都先往这四条上靠：

1. **想清楚再写** — 显式声明假设，多解法全部列出，不懂就停。
2. **简洁优先** — 最少代码解决问题，零投机，无需求外功能/抽象/配置。
3. **外科式变更** — 只动该动的，匹配既有风格，不"顺手优化"相邻代码。
4. **目标驱动执行** — 任务必须转成可验证目标（"写测试先红后绿"而非"工作正常"）。

完整规则与本土化触发点：`.claude/rules/karpathy-guidelines.md`（alwaysApply，所有角色通用）。

## 自动化守护

以下约束由 Hooks + 工具自动执行，**无需手动检查**：

| 守护 | 执行方式 | 触发时机 |
|------|---------|---------|
| 分层依赖 + Domain 纯净 + DO 泄漏 + Java 8 | `guard-java-layer.sh` Hook | 每次 Edit/Write |
| 开发准入（需有执行计划 + 评审通过） | `guard-dev-gate.sh` Hook | 写 src/main/java 时 |
| Agent 写作域边界（dev/qa/architect 越权拦截） | `guard-agent-scope.sh` Hook | 每次 Edit/Write |
| 编译 + 单测 + 风格（Java commit 时阻断） | `post-commit-check.sh` Hook | git commit 后 |
| 架构规则（14 条，含测试命名规范） | ArchUnit | mvn test |
| 代码风格 | Checkstyle | mvn checkstyle:check |
| Pre-commit / Pre-push | Git Hooks | git commit/push |
| 会话启动上下文注入（active task / 角色 / 未提交改动） | `session-start.sh` Hook | 新会话开始 |

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
| `/task-status [task-id]` | 查看任务状态一览（傻瓜式进度查询） |
| `/full-check` | mvn test + checkstyle + entropy-check |
| `/receiving-code-review <task-id>` | QA 打回后结构化修复：按严重度拆单元，每单元 Red-Green-Commit |
| `/using-git-worktrees new\|list\|remove` | 多任务并行隔离（worktree 级，不是单任务内部并行） |

### 自动触发 Skill（不可手动调用）
这些 skill 由 agent 在满足条件时自动触发，无需用户调用：

| Skill | 触发时机 | 作用 |
|-------|---------|------|
| `using-claude-j-workflow` | 会话开始（session-start hook 注入） | 工作流入口地图，告诉 agent 先读哪、先做什么 |
| `verification-before-completion` | 任何角色声称"通过/完成/修复"前 | 强制先跑命令、再附证据 |
| `systematic-debugging` | @dev 收到 QA changes-requested 或遇测试失败 | 4 阶段根因调查铁轨 |
| `dispatching-parallel-agents` | Ralph 准备派发多个子 agent / 工具调用前 | 判定何时可并行（只读研究）何时必须串行（TDD/handoff） |

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

## Skill 打包结构

Skill 专属脚本随 skill 一起打包，整体移植：

```
.claude/skills/ralph/        ├── SKILL.md
                             └── scripts/ (ralph-init.sh, ralph-loop.sh, ralph-auto.sh)
.claude/skills/full-check/   ├── SKILL.md
                             └── scripts/ (entropy-check.sh, quick-check.sh)
scripts/                     ├── hooks/          (守护 Hook，独立)
                             ├── githooks/      (Git Hook，独立)
                             ├── setup.sh / claude-gate.sh / dev-gate.sh
                             └── *.sh → 指向 .claude/skills/.../scripts/ 的符号链接
```

**为什么**：将脚本与 skill 打包可独立移植（复制整个 `.claude/skills/<skill>/` 即可），`scripts/` 下的符号链接保证 Hook、CI、已有调用方式零破坏性兼容。

## 参考文档
- 架构详解：`docs/architecture/overview.md`
- 开发规范：`docs/standards/java-dev.md`
- 测试规范：`docs/standards/java-test.md`
- 执行计划模板：`docs/exec-plan/templates/`
- 工业级功能缺口路线图：`docs/roadmap/industry-gap-analysis.md`（半生产级升级待办清单 + Top 10 必做视图）

## 移植到其他项目
本项目同时是一个**方法论示范**。如需将工作流（Ralph 编排、Agent 协作、5 阶段交付、三层守护）移植到其他项目（Java/Kotlin/TypeScript/Python/Go 等）：

- 📘 移植指南：[`PORTING.md`](./PORTING.md)
- 🧩 通用模板：[`docs/templates/project/`](./docs/templates/project/)
- 🛠 脚手架脚本：`./scripts/bootstrap-project.sh --help`
