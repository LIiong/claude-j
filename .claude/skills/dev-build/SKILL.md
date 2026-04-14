---
name: dev-build
description: "@dev Build 阶段：TDD 开发，按 domain→application→infrastructure→adapter 顺序编码，三项检查通过后交接 @qa。"
user-invocable: true
disable-model-invocation: true
argument-hint: "[task-id]-[task-name]"
allowed-tools: "Read Write Edit Glob Grep Bash(mvn *) Bash(./scripts/*) Bash(mkdir *) Bash(ls *) Bash(git *) Bash(echo *)"
---

# @dev Build 阶段 — TDD 开发

你是 claude-j 项目的高级 Java 后端开发工程师，正在执行 Build 阶段。

## 输入
- 任务标识：`$ARGUMENTS`（如 `002-order-service`）
- 已通过 @architect 评审的 `docs/exec-plan/active/$ARGUMENTS/requirement-design.md`

## 执行前：注册角色标记（Hook 自动识别用）
```bash
echo "dev" > .claude-current-role
```

## 前置条件（必须先完成）
1. 阅读 `docs/exec-plan/active/$ARGUMENTS/requirement-design.md` — 确认架构评审已通过
2. 阅读 `docs/exec-plan/active/$ARGUMENTS/task-plan.md` — 了解任务清单
3. 阅读 `docs/exec-plan/active/$ARGUMENTS/handoff.md` — 确认 status: approved
4. 若使用 Ralph Loop，阅读 `docs/exec-plan/active/$ARGUMENTS/progress.md` — 了解当前进度

## 参考文档
- `docs/standards/java-dev.md` — 开发规范
- `docs/standards/java-test.md` — 测试规范
- 已有聚合代码（如 shortlink）— 参考实现模式

## 开发顺序（严格遵守）

### 第一步：Domain 层（纯 Java，禁止 Spring）
模块：`claude-j-domain`

**值对象**（`domain.{aggregate}.model.valobj/`）：
- 所有字段 `private final`
- 构造函数中校验不变量（非空、格式、范围）
- 校验失败抛出 `BusinessException(ErrorCode.XXX)`
- Lombok：`@Getter @EqualsAndHashCode @ToString`
- 需要新 ErrorCode 时在 `domain.common.exception.ErrorCode` 枚举中添加

**实体**（`domain.{aggregate}.model.entity/`）：
- Lombok：`@Getter`（禁止 @Setter、@Data）
- 状态变更通过方法，非 setter

**聚合根**（`domain.{aggregate}.model.aggregate/`）：
- Lombok：仅 `@Getter`
- 工厂方法 `create(...)` 封装创建逻辑
- `reconstruct(...)` 从持久化恢复（不执行业务校验）
- 业务方法封装不变量（如状态机转换、金额计算）

**Repository 端口**（`domain.{aggregate}.repository/`）：
- 纯 Java 接口，返回 Domain 对象
- 方法：save、findByXxx

**领域测试**（`claude-j-domain/src/test/java/`）：
- JUnit 5 + AssertJ，**禁止 Spring 上下文和 Mockito**
- 测试：不变量强制、状态转换、计算正确性、边界情况
- 命名：`should_{预期行为}_when_{条件}`

### 第二步：Application 层（编排层）
模块：`claude-j-application`

**命令**（`application.{aggregate}.command/`）：`@Data`
**DTO**（`application.{aggregate}.dto/`）：`@Data`
**组装器**（`application.{aggregate}.assembler/`）：MapStruct `@Mapper(componentModel = "spring")`
**应用服务**（`application.{aggregate}.service/`）：`@Service @Transactional`
- 编排领域对象，不含业务逻辑
- 依赖 Repository 端口（接口），不依赖实现

**应用层测试**（`claude-j-application/src/test/java/`）：
- `@ExtendWith(MockitoExtension.class)`
- `@Mock` Repository 端口，`@InjectMocks` 应用服务
- 验证编排顺序和 save() 调用

### 第三步：Infrastructure 层（持久化适配器）
模块：`claude-j-infrastructure`

**DO**（`infrastructure.{aggregate}.persistence.dataobject/`）：`@Data @TableName("t_xxx")`
**Mapper**（`infrastructure.{aggregate}.persistence.mapper/`）：继承 `BaseMapper<XxxDO>`
**转换器**（`infrastructure.{aggregate}.persistence.converter/`）：静态方法 `toDataObject()` / `toDomain()`
**Repository 实现**（`infrastructure.{aggregate}.persistence.repository/`）：`@Repository`，实现 domain 层接口

**基础设施测试**（`claude-j-infrastructure/src/test/java/`）：
- `@SpringBootTest` + H2 内存数据库
- 测试保存→查询往返、DO↔Domain 转换

### 第四步：Adapter 层（REST 端点）
模块：`claude-j-adapter`

**请求/响应**（`adapter.{aggregate}.web.request/` 和 `response/`）：`@Data`
**控制器**（`adapter.{aggregate}.web/`）：`@RestController`
- 仅依赖 Application 层服务
- 使用 `@Valid` 校验请求
- 返回 `ApiResult<T>` 包装

**适配器测试**（`claude-j-adapter/src/test/java/`）：
- `@WebMvcTest` + MockMvc + `@MockBean`
- 测试 HTTP 状态码、请求校验、响应格式

### 第五步：Start 模块
- DDL 如未存在则写入 `claude-j-start/src/main/resources/db/schema.sql`
- 必要时更新 application 配置

## 验证（三项全过才可交接）
```bash
mvn clean test              # 所有测试通过（含 ArchUnit 14 条规则）
mvn checkstyle:check -B     # 代码风格通过
./scripts/entropy-check.sh  # 12 项架构检查通过
```

## 完成后
1. 更新 `task-plan.md` — 标记开发任务为"单测通过"
2. 更新 `dev-log.md` — 记录问题和决策
3. 更新 `handoff.md`：
   ```yaml
   from: dev
   to: qa
   status: pending-review
   pre-flight:
     mvn-test: pass
     checkstyle: pass
     entropy-check: pass
   ```
4. 若使用 Ralph Loop，更新 `progress.md` — 标记完成的任务

## 下一步
开发完成后，告知用户运行 `/qa-verify $ARGUMENTS` 进行验收。

## 上下文边界（严格遵守）
**可写**：`src/main/java/`、`src/test/java/`、exec-plan 文档（设计/计划/日志/交接/进度）、ADR、schema.sql
**禁写**：`test-case-design.md`、`test-report.md`（@qa 职责）、`docs/standards/`、`.claude/`
