# 移植指南 — 将本工作流迁移到其他项目

本项目（claude-j）是一个**方法论示范项目**：它演示了如何用 Claude Code 驱动 **DDD + 六边形架构 + Agent 编排** 的交付工作流。本文档说明哪些部分可直接移植、哪些部分需按目标项目调整。

---

## 1. 移植内容一览

| 类别 | 可复用性 | 说明 |
|------|---------|------|
| **编排工作流** | ⭐⭐⭐⭐⭐ 几乎不变 | Spec → Review → Build → Verify → Ship 五阶段，所有项目通用 |
| **Agent 角色模型** | ⭐⭐⭐⭐⭐ 几乎不变 | @dev / @architect / @qa 分工 + Ralph 编排主 Agent |
| **交接协议** | ⭐⭐⭐⭐⭐ 几乎不变 | `handoff.md` 状态机 + 写作域边界 |
| **文档模板** | ⭐⭐⭐⭐⭐ 几乎不变 | exec-plan/templates/ 下 7 份模板文件 |
| **三层守护理念** | ⭐⭐⭐⭐ 需适配工具 | L1 Hook / L2 架构测试 / L3 熵扫描（工具因栈而异） |
| **DDD + 六边形架构约束** | ⭐⭐⭐⭐ 需适配语言 | 依赖方向、对象边界、聚合设计（所有 OOP 语言通用） |
| **Rules（含 globs）** | ⭐⭐⭐ 需适配语言语法 | 抽象规则可复用，具体语法（Java 8）需替换 |
| **Skills 内容** | ⭐⭐⭐ 需替换命令/路径 | 工作流步骤不变，构建命令/包名需替换 |
| **ArchUnit 测试** | ⭐⭐ Java 专用 | 非 Java 项目需寻找等价工具（如 TS 的 dependency-cruiser） |
| **Checkstyle 配置** | ⭐⭐ Java 专用 | 非 Java 项目替换为 ESLint/ktlint 等 |
| **Scripts/Hooks** | ⭐⭐⭐ 需适配栈 | Bash 结构可复用，内部 grep 规则按新栈重写 |

---

## 2. 项目特定变量清单

移植时需要替换的全部变量集中在这里。本清单是**移植的唯一事实来源**。

### 2.1 项目身份
| 变量 | 示例（claude-j） | 说明 |
|------|-----------------|------|
| `PROJECT_NAME` | `claude-j` | 项目名，出现在路径、说明文字 |
| `PROJECT_DESCRIPTION` | `Java 电商订单系统` | 一句话项目描述 |
| `MODULE_PREFIX` | `claude-j` | 多模块时模块名前缀（如 `claude-j-domain`） |

### 2.2 技术栈
| 变量 | 示例 | 替换示例（其他栈） |
|------|------|-----|
| `LANGUAGE` | `Java` | `TypeScript` / `Kotlin` / `Python` |
| `LANGUAGE_VERSION` | `Java 8` | `TypeScript 5.x` / `Python 3.11` |
| `FRAMEWORK` | `Spring Boot 2.7.18` | `NestJS 10` / `FastAPI 0.104` / `Ktor 2.x` |
| `BUILD_TOOL` | `Maven` | `pnpm` / `Gradle` / `Poetry` |
| `ORM` | `MyBatis-Plus 3.5.5` | `Prisma` / `SQLAlchemy` / `Exposed` |
| `MAPPER_TOOL` | `MapStruct 1.5.5` | `class-transformer` / `Pydantic` |
| `TEST_FRAMEWORK` | `JUnit 5 + AssertJ + Mockito` | `Jest + ts-mockito` / `pytest` |
| `DB_DEV` | `H2 (内存)` | `SQLite` / `PostgreSQL Docker` |
| `DB_PROD` | `MySQL 8` | `PostgreSQL` / `MySQL` / `DynamoDB` |

### 2.3 命名空间
| 变量 | 示例（claude-j） | 说明 |
|------|-----------------|------|
| `PACKAGE_ROOT` | `com.claudej` | 根包名 / 根命名空间 |
| `LAYER_MODULE_DOMAIN` | `claude-j-domain` | 领域层模块名 |
| `LAYER_MODULE_APPLICATION` | `claude-j-application` | 应用层模块名 |
| `LAYER_MODULE_INFRASTRUCTURE` | `claude-j-infrastructure` | 基础设施模块名 |
| `LAYER_MODULE_ADAPTER` | `claude-j-adapter` | 适配器模块名 |
| `LAYER_MODULE_START` | `claude-j-start` | 启动模块名 |

### 2.4 构建命令
| 变量 | 示例（Maven） | 替换示例 |
|------|--------------|----------|
| `BUILD_ALL` | `mvn clean install` | `pnpm build` / `gradle build` |
| `RUN_TESTS` | `mvn test` | `pnpm test` / `pytest` |
| `STYLE_CHECK` | `mvn checkstyle:check` | `pnpm lint` / `ruff check` |
| `ARCH_TEST` | `mvn test`（ArchUnit 类） | `pnpm test:arch` / 自定义脚本 |

### 2.5 命名约定
| 变量 | 示例（Java/MySQL） | 说明 |
|------|-------------------|------|
| `TABLE_PREFIX` | `t_` | 表名前缀 |
| `COLUMN_CASE` | `snake_case` | 列命名风格 |
| `CLASS_CASE` | `PascalCase` | 类命名 |
| `METHOD_CASE` | `camelCase` | 方法命名 |

---

## 3. 移植步骤（推荐顺序）

### Step 1 — 复制移植基座（0 风险）

**核心资产打包结构**：skill 专属脚本已随 skill 打包在 `.claude/skills/<skill>/scripts/` 下，复制 `.claude/` 时自动跟随。`scripts/` 下保留符号链接向后兼容。

```bash
# 在新项目根目录
cp -r /path/to/claude-j/.claude           ./.claude
cp -r /path/to/claude-j/docs/exec-plan    ./docs/exec-plan
cp -r /path/to/claude-j/docs/templates    ./docs/templates
cp -r /path/to/claude-j/scripts/hooks     ./scripts/hooks          # 守护 Hook 脚本（独立）
cp    /path/to/claude-j/scripts/setup.sh      ./scripts/setup.sh     # 安装脚本
cp    /path/to/claude-j/scripts/claude-gate.sh ./scripts/claude-gate.sh
cp    /path/to/claude-j/scripts/dev-gate.sh   ./scripts/dev-gate.sh
cp -r /path/to/claude-j/scripts/githooks  ./scripts/githooks
cp    /path/to/claude-j/PORTING.md        ./PORTING.md

# 重建打包脚本的符号链接（兼容旧路径调用）
cd scripts
ln -sf ../.claude/skills/ralph/scripts/ralph-init.sh       ralph-init.sh
ln -sf ../.claude/skills/ralph/scripts/ralph-loop.sh       ralph-loop.sh
ln -sf ../.claude/skills/ralph/scripts/ralph-auto.sh       ralph-auto.sh
ln -sf ../.claude/skills/full-check/scripts/entropy-check.sh  entropy-check.sh
ln -sf ../.claude/skills/full-check/scripts/quick-check.sh    quick-check.sh
cd ..
```

这些全部是**方法论级**资产，复制后按 Step 2 做变量替换即可生效。

> **打包说明**：`entropy-check.sh` / `quick-check.sh` 属于 `full-check` skill 的子能力；`ralph-*.sh` 属于 `ralph` skill。移植时只需整体复制 `.claude/skills/`，符号链接保证现有 Hook、CI、其他 skill 的旧调用方式继续工作。

> **易被忽略的已随 `.claude/` 打包的脚本**（`cp -r .claude` 已自动包含，单独列出便于校验）：
> - `.claude/skills/using-claude-j-workflow/scripts/session-start.sh` — SessionStart hook 目标脚本；`.claude/settings.json` 已指向该路径，**无需再从 `scripts/hooks/` 单独复制**。
> - `.claude/skills/qa-ship/scripts/pre-archive-check.sh` — 归档前置闸；`.claude/agents/qa.md` 第 10 步 **机械依赖** 此脚本的 PASS 输出。

### Step 2 — 变量替换
按"变量清单"做全局替换。推荐工具：
```bash
# macOS/Linux
find .claude docs scripts -type f \( -name "*.md" -o -name "*.sh" \) \
  -exec sed -i '' 's/claude-j/YOUR-PROJECT/g' {} +

find .claude docs scripts -type f \( -name "*.md" -o -name "*.sh" \) \
  -exec sed -i '' 's/com\.claudej/com\.yourcompany/g' {} +
```
或用脚手架脚本（见 Step 5）。

### Step 3 — 重写项目说明
替换以下文件内容（保留结构，改写内容）：
- `CLAUDE.md` → 参考 `docs/templates/project/CLAUDE.template.md`
- `docs/architecture/overview.md` → 参考 `docs/templates/project/architecture-overview.template.md`
- `docs/standards/java-dev.md` → 按目标语言重写（规则项结构不变，语法细节替换）
- `docs/standards/java-test.md` → 同上

### Step 4 — 适配三层守护

**L1（编辑期 Hook）**：
- 复制 `scripts/hooks/guard-*.sh`
- 修改 `guard-java-layer.sh` 中的路径和 import 识别逻辑为目标语言
- 在 `.claude/settings.json` 注册 Hook

**L2（架构测试）**：
- Java 项目：沿用 ArchUnit（仅替换包名）
- TypeScript 项目：改用 [dependency-cruiser](https://github.com/sverweij/dependency-cruiser)
- Python 项目：改用 [import-linter](https://github.com/seddonym/import-linter)
- 关键：保留"依赖方向"、"domain 纯净"、"测试命名"三条规则

**L3（熵扫描）**：
- `scripts/entropy-check.sh` 为 13 项 grep 规则（第 13 项为"归档后篡改检测"，仅 WARN 级）
- 按新栈重写 grep 模式（依据目标语言 import 语法）

### Step 5 — 运行脚手架（可选）
```bash
./scripts/bootstrap-project.sh \
  --project-name "your-project" \
  --package-root "com.yourcompany" \
  --description "项目说明"
```
脚本会执行 Step 2 + Step 3 的自动化部分，生成项目骨架（见 `scripts/bootstrap-project.sh`）。

### Step 6 — 验证
```bash
# 产物自检（必需文件 + 符号链接 + settings.json hook 路径三件套）
./scripts/verify-portable.sh /path/to/new-project
# 预期：所有项 ✅，退出码 0

# 确认 hooks 加载
ls .claude/settings.json

# 跑通一个最小任务（用模板中的示例）
/ralph 001-hello-world 实现一个 hello 端点
```
若 Spec → Review → Build → Verify → Ship 能走通，移植成功。

---

## 4. 按目标栈的已知适配工作量

| 目标栈 | 估计工作量 | 关键改动 |
|--------|-----------|---------|
| Java + Spring Boot 3 | 2-4 小时 | 仅需变量替换 + Java 17 语法放开 |
| Kotlin + Spring Boot | 4-6 小时 | 语法规则重写 + Kotlin 特性调整 |
| TypeScript + NestJS | 1-2 天 | L1 Hook 改 regex / L2 换 dependency-cruiser / 模板重写 |
| Python + FastAPI | 1-2 天 | L1 Hook 改 regex / L2 换 import-linter / 去除 Lombok 相关 |
| Go + gin/echo | 2-3 天 | DDD 语义保留，但 Go 无继承需调整聚合根建模建议 |

---

## 5. 不可移植内容（需项目本地重建）

- **具体业务聚合代码**（shortlink / order / coupon 等）—— 新项目必须自己建
- **docs/architecture/decisions/ 下 ADR**—— 均是本项目决策，新项目从 000-template.md 开始
- **docs/exec-plan/archived/**—— 本项目的历史任务，无需带走
- **pom.xml / build.gradle / package.json**—— 按新栈重写

---

## 6. 移植后的维护

1. **上游更新回流**：本项目持续演进时，定期 `diff` `.claude/`、`docs/templates/`、`scripts/hooks/` 获取改进
2. **不下游耦合**：新项目的业务代码不依赖 claude-j 任何类，保证干净
3. **贡献回上游**：新项目发现的通用改进建议提交 PR 到本项目

---

## 7. 相关文档
- `docs/templates/project/CLAUDE.template.md` — 项目说明文件的通用模板
- `docs/templates/project/architecture-overview.template.md` — 架构概览模板
- `docs/templates/project/variables.md` — 变量定义参考
- `scripts/bootstrap-project.sh` — 自动化脚手架脚本
- `.claude/skills/ralph/scripts/README.md` — Ralph skill 打包脚本说明
- `.claude/skills/full-check/scripts/README.md` — 验证脚本打包说明
- `.claude/skills/qa-ship/scripts/pre-archive-check.sh` — 归档前置闸脚本（qa.md 第 10 步机械调用）
- `CLAUDE.md` — 当前项目（claude-j）的实际配置，可作为填写示例
