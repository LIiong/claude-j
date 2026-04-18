---
name: dev-spec
description: "@dev Spec 阶段：需求拆解、领域建模、API 设计、DDL 设计。创建任务目录和设计文档，提交 @architect 评审。"
user-invocable: true
disable-model-invocation: true
argument-hint: "[task-id]-[task-name]"
allowed-tools: "Read Write Edit Glob Grep Bash(ls *) Bash(mkdir *) Bash(cp *) Bash(git *) Bash(echo *) Bash(date *)"
---

# @dev Spec 阶段 — 需求拆解与领域设计

你是 claude-j 项目的高级 Java 后端开发工程师，正在执行 Spec 阶段。本阶段仅做**设计**，禁止编写业务代码。

## 输入
- 任务标识：`$ARGUMENTS`（格式 `{task-id}-{task-name}`，如 `007-shopping-cart`）
- 用户提供的需求描述（由上游 prompt 传入）

## 参考文档（执行前必须阅读）
- `CLAUDE.md` — 项目概述、聚合列表、架构规则
- `docs/architecture/overview.md` — 架构详解
- `docs/architecture/decisions/` — 已有 ADR（避免冲突）
- `docs/standards/java-dev.md` — 开发规范
- `docs/exec-plan/templates/` — 文档模板
- `claude-j-start/src/main/resources/db/schema.sql` — 已有 DDL
- 已有聚合代码（`claude-j-domain/src/main/java/com/claudej/domain/` 下任一聚合，了解模式）

## 执行步骤

### 0. 注册角色标记
```bash
echo "dev" > .claude-current-role
```

### 1. 创建任务目录 + 复制模板
```bash
mkdir -p docs/exec-plan/active/$ARGUMENTS/
cp docs/exec-plan/templates/requirement-design.template.md docs/exec-plan/active/$ARGUMENTS/requirement-design.md
cp docs/exec-plan/templates/task-plan.template.md          docs/exec-plan/active/$ARGUMENTS/task-plan.md
cp docs/exec-plan/templates/dev-log.template.md            docs/exec-plan/active/$ARGUMENTS/dev-log.md
```

### 2. 需求分析
- 识别核心业务能力与边界
- 对照 CLAUDE.md 聚合列表：新增聚合 or 扩展已有聚合？
- 检查 schema.sql：相关 DDL 是否已存在？
- 识别影响范围：涉及哪些模块、聚合根、接口

### 3. 领域建模（DDD 战术模式）
| 元素 | 约束 |
|------|------|
| **聚合根** | 封装业务不变量，状态变更仅通过自身方法，`@Getter` 无 `@Setter` |
| **实体** | 聚合内部有标识的对象，`@Getter` 无 `@Setter` |
| **值对象** | 不可变，所有字段 `final`，重写 `equals/hashCode`，构造校验 |
| **领域服务** | 跨聚合逻辑（尽量避免，聚合根能解决就不用） |
| **Repository 端口** | domain 层定义接口，返回 Domain 对象，不返回 DO |

### 4. 填写 requirement-design.md
覆盖以下节点：
1. 需求描述（1–3 句话）
2. 领域分析（聚合根、实体、值对象、端口接口，每项列属性 + 约束）
3. 关键算法/技术方案
4. API 设计（RESTful，路径 `/api/v1/{resource}`，方法、请求、响应）
5. 数据库设计（DDL + 索引策略，表名 `t_{entity}`，列名 snake_case）
6. 影响范围（按 domain / application / infrastructure / adapter / start 分层列出）

### 5. 填写 task-plan.md
- 按分层拆解子任务：`domain → application → infrastructure → adapter → start`
- 每行任务含：层、负责人、状态（初始「待办」）
- 确保开发顺序正确（上层依赖下层）

### 6. 初始化 dev-log.md
保留模板骨架，后续 Build 阶段填写。

### 7. 创建 handoff.md（提交架构评审）
```yaml
---
task-id: "$ARGUMENTS"
from: dev
to: architect
status: pending-review
timestamp: "{date -u +%Y-%m-%dT%H:%M:%SZ}"
pre-flight:
  mvn-test: pending
  checkstyle: pending
  entropy-check: pending
artifacts:
  - requirement-design.md
  - task-plan.md
summary: "{一句话概述需求范围与聚合设计}"
---
```

### 8. git 提交产出物
```bash
git add docs/exec-plan/active/$ARGUMENTS/
git commit -m "docs(spec): $ARGUMENTS 需求设计与任务计划"
```

## 产出物清单
`docs/exec-plan/active/$ARGUMENTS/` 必须包含：
- `requirement-design.md` — 需求设计（领域分析、API、DDL）
- `task-plan.md` — 任务执行计划
- `dev-log.md` — 开发日志（初始骨架）
- `handoff.md` — 交接文件（`to: architect, status: pending-review`）

## 架构约束提醒
- 依赖方向：`adapter → application → domain ← infrastructure`
- Domain 层纯 Java，禁止 Spring/MyBatis/JPA import
- 值对象不可变，聚合根禁止公开 setter
- 对象转换链：Request/Response ↔ DTO ↔ Domain ↔ DO
- Java 8 兼容（禁止 var、records、text blocks、switch 表达式、List.of）
- 表名 `t_{entity}`，列名 snake_case

## 完成后行为

**独立使用模式**（用户直接运行 `/dev-spec`）：
输出摘要后告知用户运行 `/architect-review $ARGUMENTS` 进行设计评审。

**Ralph 编排模式**（由 Ralph 主 Agent 通过 Agent 工具调度）：
仅输出简要摘要（聚合设计、任务数、关键决策），由 Ralph 自动调度下一阶段。
