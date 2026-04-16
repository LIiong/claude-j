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
| cart | `com.claudej.*.cart` | 购物车服务 |
| auth | `com.claudej.*.auth` | 认证服务 |

## 关键约定
- 包根 `com.claudej`，Java 8 语法（禁止 var/records/text blocks/List.of）
- 聚合根封装业务不变量，禁止贫血模型，禁止公开 setter
- 对象转换链：Request/Response ↔ DTO ↔ Domain ↔ DO（MapStruct）
- DO 对象不得泄漏到 infrastructure 之外
- 表名 `t_{entity}`，列名 snake_case
- 重要决策记录 ADR（`docs/architecture/decisions/`）

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
- 开发规范：`docs/standards/java-dev.md`
- 测试规范：`docs/standards/java-test.md`
- 执行计划模板：`docs/exec-plan/templates/`
