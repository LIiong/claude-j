# DevOps 工作流指南

## 0. 快速开始

```bash
# 一键搭建开发环境（检查 JDK/Maven、配置 Git Hooks、全量构建）
./scripts/setup.sh
```

如果只想手动配置 Git Hooks：
```bash
git config core.hooksPath scripts/githooks
```

## 1. CI 流水线概览

项目使用 GitHub Actions 自动化所有质量守护。

### 主流水线（`.github/workflows/ci.yml`）

触发条件：push 到 `main`/`develop`、所有 PR

```
build (编译)
  ├── unit-tests (Domain + Application 层测试)
  ├── integration-tests (Infrastructure + Adapter + Start 层测试，含 ArchUnit)
  ├── checkstyle (代码风格检查)
  └── entropy-check (架构漂移检测)
        └── coverage (JaCoCo 覆盖率报告，依赖所有测试通过)
              └── test-summary (PR 自动评论：测试结果 + 覆盖率摘要)
```

### 定时熵管理（`.github/workflows/entropy-check.yml`）

- 每周一 9:00 UTC 自动运行
- 支持手动触发（workflow_dispatch）
- 失败时自动创建 GitHub Issue（label: `entropy`, `automated`），包含结构化修复建议

### PR 自动标签（`.github/workflows/pr-review.yml`）

根据变更文件路径自动添加标签：`layer:domain`、`layer:application`、`docs`、`tests` 等。

## 2. Git Hooks 全览

安装方式：`git config core.hooksPath scripts/githooks`（`scripts/setup.sh` 会自动配置）

| Hook | 触发时机 | 检查内容 | 跳过方式 |
|------|----------|----------|----------|
| **pre-commit** | `git commit` | 编译 + Checkstyle | `--no-verify` |
| **commit-msg** | `git commit` | Conventional Commits 格式 | `--no-verify` |
| **pre-push** | `git push` | 全量测试 + Checkstyle + 熵检查 | `--no-verify` |

### pre-commit

快速检查，确保基本质量：
1. `mvn compile -q` — 编译通过
2. `mvn checkstyle:check -q` — 代码风格合规

### commit-msg

强制 Conventional Commits 格式：
```
{type}({scope}): {description}
```

- **允许的 type**：feat, fix, refactor, test, docs, chore, style, perf
- **允许的 scope**：domain, application, infrastructure, adapter, start, docs, ci
- **长度限制**：首行不超过 72 字符

示例：
```
feat(domain): add Order aggregate root with status machine
fix(infrastructure): fix OrderRepository null pointer on empty result
docs: update DevOps guide with new hooks
```

### pre-push

推送前全量验证（较慢，但确保远程不会失败）：
1. `mvn test -B` — 全部测试通过（含 ArchUnit 14 条规则）
2. `mvn checkstyle:check -B` — 代码风格合规
3. `./scripts/entropy-check.sh` — 10 项熵检查全部通过

### 跳过 Hooks（仅紧急情况）

```bash
git commit --no-verify -m "chore: emergency fix"
git push --no-verify
```

## 3. 本地运行守护

### 快速检查（开发过程中）

```bash
./scripts/quick-check.sh   # 编译 + domain/application 单测 + checkstyle（约 10-15s）
```

### 完整检查（提交前）

```bash
# 全量构建（编译 + 测试 + ArchUnit）
mvn clean install

# 仅运行测试
mvn test

# 测试 + JaCoCo 覆盖率报告
mvn verify

# 代码风格检查
mvn checkstyle:check

# 熵检查（架构漂移检测）
./scripts/entropy-check.sh
```

### 三项必过（提交前）

开发完成后，以下三项必须全部通过才可提交：

1. `mvn test` — 所有测试通过（含 ArchUnit 14 条架构规则）
2. `mvn checkstyle:check` — 代码风格无违规
3. `./scripts/entropy-check.sh` — 10 项熵检查全部通过

## 4. 覆盖率报告（JaCoCo）

### 生成报告

```bash
mvn verify
```

报告位置：`{module}/target/site/jacoco/index.html`

### 覆盖率阈值

| 层 | 模块 | 目标覆盖率 |
|---|------|-----------|
| Domain | claude-j-domain | 90% |
| Application | claude-j-application | 80% |
| Infrastructure | claude-j-infrastructure | 70% |
| Adapter | claude-j-adapter | 70% |

### Lombok 排除

项目已配置 `lombok.config` 添加 `@Generated` 注解，JaCoCo 自动排除 Lombok 生成的代码。

## 5. Docker 构建与运行

### 构建镜像

```bash
docker build -f docs/devops/Dockerfile -t claude-j .
```

### 运行容器

```bash
# 使用 dev profile（H2 内存数据库）
docker run -p 8080:8080 claude-j

# 使用 docker-compose（含 MySQL）
cd docs/devops && docker-compose up
```

### Dockerfile 说明

多阶段构建：
- **Stage 1（builder）**：使用 JDK 8 编译打包
- **Stage 2（runtime）**：使用 JRE 8 运行，仅包含 fat JAR

## 6. PR 审查流程

### 自动化检查（CI 强制）

| 检查项 | 工具 | 说明 |
|--------|------|------|
| 编译 | Maven | 所有模块编译通过 |
| 单元测试 | JUnit 5 | Domain + Application 层 |
| 集成测试 | SpringBootTest + H2 | Infrastructure + Adapter + Start 层 |
| 架构守护 | ArchUnit | 13 条规则：层间依赖、命名等 |
| 代码风格 | Checkstyle | Java 8 兼容、命名、import |
| 熵检查 | entropy-check.sh | 10 项架构漂移检测 |
| PR 摘要 | github-script | 测试结果 + 覆盖率自动回写 PR 评论 |

### 人工审查（PR 模板清单）

PR 模板（`.github/pull_request_template.md`）中的 DDD 架构清单需人工确认：
- 聚合根封装业务不变量
- 值对象不可变
- 对象转换层次正确
- Controller 无业务逻辑

## 7. 熵管理

### entropy-check.sh 检查项

| # | 检查 | 级别 | 说明 |
|---|------|------|------|
| 1 | Domain 层纯净性 | FAIL | 禁止 Spring/MyBatis import |
| 2 | 依赖方向 | FAIL | adapter/application 不引入 infrastructure |
| 3 | Java 8 兼容性 | FAIL | 禁止 var、List.of、Map.of |
| 4 | DO 对象泄漏 | FAIL | DO 不出现在 infrastructure 之上 |
| 5 | 代码整洁度 | WARN | 禁止 * import |
| 6 | 文档同步 | WARN | schema.sql 表数 + CLAUDE.md 存在 |
| 7 | 测试文件存在性 | WARN | Service/Repository/Controller 需有测试 |
| 8 | 死代码检测 | WARN | 查找无引用的 Java 类 |
| 9 | ADR 一致性 | WARN | 验证 ADR 文件格式完整 |
| 10 | 知识库一致性 | WARN | 文档交叉引用验证（引用的文件是否存在） |

### 常见问题修复

- **Domain 层纯净性失败**：检查 domain 模块是否意外引入了 Spring 或 MyBatis 依赖
- **依赖方向失败**：检查 import 语句，确保不跨层引用
- **DO 泄漏**：将 DO 对象的使用限制在 infrastructure 模块内
- **知识库一致性**：更新文档中的断链文件路径引用

## 8. Harness Engineering 守护体系

本项目遵循 Harness Engineering 四大支柱设计守护体系：

```
Constrain (约束) ─── 机械化强制，限制解空间
├── ArchUnit 14 条规则（层间依赖、命名、Domain 纯净性）
├── Checkstyle（Java 8 兼容、代码风格）
├── pre-commit hook（编译 + 风格）
├── commit-msg hook（Conventional Commits 格式）
└── pre-push hook（全量测试门禁）

Inform (上下文) ─── 代码库即唯一真相源
├── CLAUDE.md（项目总入口）
├── docs/（架构、规范、指南、计划）
├── .claude/agents/（dev + qa Agent 定义）
└── ADR（架构决策记录）

Verify (验证) ─── 多层次反馈循环
├── CI 流水线（test + checkstyle + entropy + coverage）
├── PR 测试/覆盖率摘要自动回写
├── PR 模板（DDD 审查清单）
└── scripts/quick-check.sh（快速本地验证）

Correct (纠正) ─── 定期熵管理 / Garbage Collection
├── entropy-check.sh（10 项检查）
├── 定时 entropy workflow（每周一自动运行）
└── 失败自动创建 Issue（结构化修复建议）
```

## 9. 项目目录结构

```
claude-j/
├── CLAUDE.md                           # 项目总入口
├── pom.xml                             # Maven 父 POM
├── lombok.config                       # Lombok 配置（JaCoCo 排除）
├── .github/
│   ├── workflows/
│   │   ├── ci.yml                      # 主 CI 流水线
│   │   ├── entropy-check.yml           # 定时熵管理
│   │   └── pr-review.yml              # PR 自动标签
│   └── pull_request_template.md        # PR 审查模板
├── .claude/
│   ├── agents/                         # Agent 定义
│   └── rules/                          # 规则（symlink → docs/standards/）
├── docs/
│   ├── architecture/                   # 架构文档
│   │   ├── overview.md
│   │   └── decisions/                  # ADR
│   ├── standards/                      # 编码规范
│   │   ├── java-dev.md
│   │   ├── java-test.md
│   │   ├── checkstyle.xml
│   │   └── quality-assurance.md
│   ├── guides/                         # 开发指南
│   │   ├── development-guide.md
│   │   └── devops-guide.md
│   ├── exec-plan/                      # 执行计划
│   └── devops/                         # DevOps 配置
│       ├── Dockerfile
│       └── docker-compose.yml
├── scripts/
│   ├── setup.sh                        # 一键环境搭建
│   ├── quick-check.sh                  # 快速本地检查
│   ├── entropy-check.sh                # 熵检查脚本（10 项）
│   └── githooks/
│       ├── pre-commit                  # 编译 + Checkstyle
│       ├── commit-msg                  # Conventional Commits 格式
│       └── pre-push                    # 全量测试门禁
└── claude-j-{domain,application,infrastructure,adapter,start}/
```
