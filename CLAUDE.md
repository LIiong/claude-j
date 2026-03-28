# CLAUDE.md

## 项目概述
Java 电商订单系统，演示 DDD + 六边形架构最佳实践。
技术栈：Java 8（源码兼容）/ Spring Boot 2.7.18 / Maven / MyBatis-Plus 3.5.5 / MapStruct 1.5.5

## 构建与验证
```bash
./scripts/setup.sh                                                     # 一键环境搭建（首次）
mvn clean install                                                      # 全量构建
mvn spring-boot:run -pl claude-j-start -Dspring-boot.run.profiles=dev  # 开发环境（H2）
mvn test                                                               # 运行全部测试（含 ArchUnit 架构守护）
mvn verify                                                             # 测试 + JaCoCo 覆盖率报告
mvn checkstyle:check                                                   # 代码风格检查
./scripts/quick-check.sh                                               # 快速检查（编译+单测+风格，开发中用）
./scripts/entropy-check.sh                                             # 熵检查（架构漂移检测，10 项）
docker build -f docs/devops/Dockerfile -t claude-j .                   # Docker 镜像构建
```

## 架构：DDD + 六边形（端口与适配器）

### 模块依赖方向（自动化守护，ArchUnit 每次测试强制检查）
```
┌─────────┐     ┌─────────────┐     ┌────────┐
│ adapter  │────▶│ application │────▶│ domain │
└─────────┘     └─────────────┘     └────────┘
                                        ▲
                ┌────────────────┐       │
                │ infrastructure │───────┘
                └────────────────┘
                        ▲
┌───────┐               │
│ start │───────────────┤ (assembles all)
└───────┘
```

### 模块职责
- **domain**：纯领域模型。禁止 Spring、禁止框架依赖。包含：实体、值对象、聚合根、领域服务、Repository 端口接口、领域事件。
- **application**：用例编排。应用服务（@Service, @Transactional）、命令、查询、DTO、Assembler。
- **infrastructure**：实现 domain/application 定义的接口。MyBatis-Plus Mapper、Repository 实现（@Repository）、外部服务客户端。
- **adapter**：入站适配器。REST Controller、请求/响应对象、异常处理器。仅依赖 application 层。
- **start**：Spring Boot 启动模块。配置文件、主类、ArchUnit 架构测试、集成测试。

### 包结构速查
```
com.claudej
├── domain.{aggregate}
│   ├── model.aggregate/     聚合根
│   ├── model.valobj/        值对象
│   ├── repository/          Repository 端口（接口）
│   ├── service/             领域服务端口（接口）
│   └── event/               领域事件
├── application.{aggregate}
│   ├── command/             命令对象
│   ├── dto/                 数据传输对象
│   ├── assembler/           Domain ↔ DTO 转换（MapStruct）
│   └── service/             应用服务（编排层）
├── infrastructure.{aggregate}
│   ├── persistence.dataobject/   DO（@Data @TableName）
│   ├── persistence.mapper/       MyBatis-Plus Mapper
│   ├── persistence.converter/    DO ↔ Domain 转换
│   ├── persistence.repository/   Repository 实现（@Repository）
│   └── service/                  领域服务实现（@Component）
├── adapter.{aggregate}
│   └── web/                 Controller + Request + Response
└── adapter.common/          ApiResult, GlobalExceptionHandler
```

### 当前聚合列表

| 聚合 | 包名 | 状态 | 说明 |
|------|------|------|------|
| shortlink | `com.claudej.*.shortlink` | ✅ 已完成 | 短链服务（创建/重定向/去重） |
| order | `com.claudej.*.order` | 📋 DDL 已定义 | 电商订单（schema.sql 中有表，代码未实现） |

## 关键约定
- 包根：`com.claudej`
- 仅使用 Java 8 语法（禁止 var、records、text blocks）— Checkstyle 自动检查
- 聚合根必须封装业务不变量（禁止贫血模型）
- Repository 接口（端口）在 domain，实现（适配器）在 infrastructure
- 对象转换层次：DO（持久化）↔ Domain ↔ DTO（应用层）↔ Request/Response（适配器层）
- 使用 MapStruct 进行对象转换，Lombok 减少样板代码
- MyBatis-Plus 使用 BaseMapper 进行持久化
- 命名：实体无后缀、DO 后缀、DTO 后缀、Mapper 后缀
- 重要技术决策必须记录 ADR（`docs/architecture/decisions/{编号}-{标题}.md`）

## 自动化守护（Harness）

| 守护层 | 工具 | 触发时机 | 说明 |
|--------|------|----------|------|
| **架构依赖方向** | ArchUnit | `mvn test` | 13 条规则：层间依赖、domain 纯净性、命名规范 |
| **代码风格** | Checkstyle | `mvn checkstyle:check` | Java 8 兼容、命名、import 规范 |
| **熵检查** | entropy-check.sh | 手动 / CI 定期 | 10 项检查：依赖、纯净性、泄漏、文档同步、测试覆盖、死代码、ADR、知识库一致性 |
| **分层测试** | JUnit 5 | `mvn test` | 测试金字塔：Domain→Application→Infrastructure→Adapter→集成 |
| **代码覆盖率** | JaCoCo | `mvn verify` | 分层阈值：domain 90%、application 80%、infra/adapter 70% |
| **CI 流水线** | GitHub Actions | Push / PR | 编译、测试、Checkstyle、entropy-check、覆盖率 + PR 摘要回写 |
| **Pre-commit** | Git Hooks | `git commit` | 编译检查 + Checkstyle |
| **Commit-msg** | Git Hooks | `git commit` | Conventional Commits 格式强制 |
| **Pre-push** | Git Hooks | `git push` | 全量测试 + Checkstyle + 熵检查 |
| **PR 质量门禁** | PR Template | PR 创建 | DDD 代码审查清单（自动 + 人工） |

## Agent 团队
- **@dev** — 开发 Agent：负责按 DDD 模式编写功能代码
- **@qa** — QA Agent：负责编写测试和代码审查

## Dev-QA 协作工作流

模板目录：`docs/exec-plan/templates/`（所有文档必须按模板填写）

```
用户需求
  │
  ▼
@dev ── Spec 阶段 ──────────────────────────────────────────────
  │  1. 从模板创建任务目录 docs/exec-plan/active/{task-id}-{task-name}/
  │  2. 填写 requirement-design.md（领域分析、API、DDL）
  │  3. 填写 task-plan.md（任务拆解、状态跟踪）
  │  4. 重要决策写 ADR（docs/architecture/decisions/）
  │
  ▼
@dev ── Build 阶段 ─────────────────────────────────────────────
  │  5. TDD：先写测试 → 编码 → 验证
  │  6. 开发顺序: domain → application → infrastructure → adapter → start
  │  7. 记录 dev-log.md（问题 + 决策 + 变更）
  │  8. mvn test + mvn checkstyle:check + entropy-check.sh 全部通过
  │  9. 标记"待验收" → 通知 @qa
  │
  ▼
@qa ── Verify 阶段 ─────────────────────────────────────────────
  │  10. 填写 test-case-design.md（分层用例 + 集成用例 + 审查项）
  │  11. 执行测试 + 编写集成测试（start 模块）
  │  12. 代码 Review（ArchUnit 已自动覆盖依赖方向，聚焦业务逻辑）
  │  13. 填写 test-report.md
  │
  ├── 有问题 → 通知 @dev 修复 → 回到步骤 5
  │
  ▼
@qa ── Ship 阶段 ───────────────────────────────────────────────
  14. 标记"验收通过" → 归档到 docs/exec-plan/archived/
  15. 更新 CLAUDE.md 聚合列表（新增聚合、入口等）
```

## 新功能开发顺序
1. domain 模块：定义聚合根、实体、值对象
2. domain 模块：定义 Repository 端口接口
3. application 模块：创建应用服务 + 命令/DTO
4. infrastructure 模块：实现持久化适配器
5. adapter 模块：创建 REST 端点
6. start 模块：DDL 写入 db/schema.sql
7. 各层编写测试（参见 @qa agent 或 docs/standards/java-test.md）
8. 验证：`mvn test` + `mvn checkstyle:check` + `./scripts/entropy-check.sh`

## 文档导航

| 文档 | 路径 | 用途 |
|------|------|------|
| 架构详解 | `docs/architecture/overview.md` | 架构设计决策 |
| 决策记录 | `docs/architecture/decisions/` | 架构决策记录（ADR） |
| 开发规则 | `docs/standards/java-dev.md` | Java 编码规范 |
| 测试规则 | `docs/standards/java-test.md` | 各层测试规范 |
| 质保标准 | `docs/standards/quality-assurance.md` | 测试策略和覆盖率要求 |
| 开发流程 | `docs/guides/development-guide.md` | Dev-QA 协作详细流程 |
| DevOps 指南 | `docs/guides/devops-guide.md` | CI/CD、Docker、Pre-commit 工作流 |
| 执行计划模板 | `docs/exec-plan/templates/` | 需求/计划/日志/测试模板 |
