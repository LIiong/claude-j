# 任务执行计划 — {task-id}-{task-name}

## 任务状态跟踪

<!-- 状态流转：待办 → 进行中 → 单测通过 → 待验收 → 验收通过 / 待修复 -->

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Domain: {聚合根} + 值对象 + 测试 | dev | 待办 | |
| 2 | Domain: Repository 端口 | dev | 待办 | |
| 3 | Domain: 领域服务端口（如有） | dev | 待办 | |
| 4 | Application: Command + DTO + Assembler | dev | 待办 | |
| 5 | Application: ApplicationService + 测试 | dev | 待办 | |
| 6 | Infrastructure: DO + Mapper + Converter | dev | 待办 | |
| 7 | Infrastructure: RepositoryImpl + 测试 | dev | 待办 | |
| 8 | Infrastructure: 领域服务实现 + 测试（如有） | dev | 待办 | |
| 9 | Adapter: Controller + Request/Response + 测试 | dev | 待办 | |
| 10 | Start: schema.sql DDL | dev | 待办 | |
| 11 | 全量 mvn test | dev | 待办 | |
| 12 | QA: 测试用例设计 | qa | 待办 | |
| 13 | QA: 验收测试 + 代码审查 | qa | 待办 | |
| 14 | QA: 接口集成测试 | qa | 待办 | |

<!-- 根据实际需求增减任务行，保持编号连续 -->

## 执行顺序
domain → application → infrastructure → adapter → start → 全量测试 → QA 验收 → 集成测试

## 原子任务分解（每项 10–15 分钟，单会话可完成并 commit）

> **目的**：将上表「按层」的粗粒度任务拆到 10–15 分钟的原子级，便于 Ralph Loop 单轮执行完整交付、便于新会话恢复时定位进度。
>
> **要求**：每个原子任务必填 5 个字段 — `文件路径`、`骨架片段`、`验证命令`、`预期输出`、`commit 消息`。

<!-- 示例格式，按聚合与层列出。N.M 编号与上表 N 行对齐 -->

### 1.1 Domain 值对象 `{ValueObject}`
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/{aggregate}/model/valobj/{ValueObject}.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/{aggregate}/model/valobj/{ValueObject}Test.java`
- **骨架**（Red 阶段先写测试）：
  ```java
  // {ValueObject}Test.java — 覆盖构造校验 / 相等性 / 不变量
  @Test
  void should_throw_when_value_is_null() { ... }
  @Test
  void should_equal_when_values_match() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-domain -Dtest={ValueObject}Test`
- **预期输出**：`Tests run: X, Failures: 0, Errors: 0`（先看红再看绿）
- **commit**：`feat(domain): {aggregate} 值对象 {ValueObject}`

### 1.2 Domain 聚合根 `{Aggregate}`
- **文件**：`.../model/aggregate/{Aggregate}.java`
- **骨架**：工厂 `create(...)` + `reconstruct(...)` + 状态转换方法；`@Getter` 无 `@Setter`
- **验证命令**：`mvn test -pl claude-j-domain -Dtest={Aggregate}Test`
- **预期输出**：覆盖不变量 / 状态转换 / 异常场景，全部绿
- **commit**：`feat(domain): {aggregate} 聚合根与不变量`

### 2.1 Application Command + DTO
- **文件**：`.../application/{aggregate}/command/{Xxx}Command.java`、`.../dto/{Xxx}DTO.java`
- **验证命令**：`mvn compile -pl claude-j-application`
- **预期输出**：编译通过
- **commit**：`feat(application): {aggregate} 命令与 DTO`

### 2.2 Application Service + 测试
- **文件**：`.../application/{aggregate}/service/{Xxx}ApplicationService.java`
- **测试**：使用 `@ExtendWith(MockitoExtension.class)` + `@Mock Repository`
- **骨架**：
  ```java
  @Test
  void should_save_aggregate_when_command_valid() {
      // Arrange: mock repo.save
      // Act: service.handle(command)
      // Assert: verify(repo).save(any()) + 返回值
  }
  ```
- **验证命令**：`mvn test -pl claude-j-application`
- **预期输出**：Mockito verify 通过，编排顺序正确
- **commit**：`feat(application): {aggregate} 应用服务与编排`

### 3.1 Infrastructure DO + Converter
- **文件**：`.../infrastructure/{aggregate}/persistence/dataobject/{Xxx}DO.java`、`.../converter/{Xxx}Converter.java`
- **验证命令**：`mvn compile -pl claude-j-infrastructure`
- **预期输出**：无 DO 泄漏到层外（`guard-java-layer.sh` 不报警）
- **commit**：`feat(infrastructure): {aggregate} DO 与转换器`

### 3.2 Infrastructure Repository 实现 + 集成测试
- **文件**：`.../infrastructure/{aggregate}/persistence/repository/{Xxx}RepositoryImpl.java`
- **测试**：`@SpringBootTest` + H2，覆盖保存→查询往返
- **验证命令**：`mvn test -pl claude-j-infrastructure -Dtest={Xxx}RepositoryImplIT`
- **预期输出**：DO↔Domain 映射准确，H2 写入回读一致
- **commit**：`feat(infrastructure): {aggregate} 持久化实现`

### 4.1 Adapter Controller + Request/Response
- **文件**：`.../adapter/{aggregate}/web/{Xxx}Controller.java`、`.../request/{Xxx}Request.java`、`.../response/{Xxx}Response.java`
- **测试**：`@WebMvcTest` + MockMvc + `@MockBean ApplicationService`
- **骨架**：
  ```java
  @Test
  void should_return_200_when_request_valid() { ... }
  @Test
  void should_return_400_when_field_missing() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-adapter -Dtest={Xxx}ControllerTest`
- **预期输出**：HTTP 状态码 200/400/404/500 断言全过
- **commit**：`feat(adapter): {aggregate} REST 端点`

### 5.1 Start DDL + 装配
- **文件**：`claude-j-start/src/main/resources/db/schema.sql`
- **验证命令**：`mvn spring-boot:run -pl claude-j-start -Dspring-boot.run.profiles=dev`（手动）或 Infrastructure 层 H2 测试覆盖
- **预期输出**：表名 `t_{entity}`，列名 snake_case
- **commit**：`feat(start): {aggregate} DDL 与装配`

<!-- Ralph Loop 执行时：每完成一个原子任务 → 立即 commit → 更新 progress.md [x] -->
<!-- 新会话恢复时：grep 上表状态列「单测通过」即可定位下一个原子任务 -->


## 开发完成记录
<!-- dev 完成后填写 -->
- 全量 `mvn clean test`：x/x 用例通过
- 架构合规检查：
- 通知 @qa 时间：

## QA 验收记录
<!-- qa 验收后填写 -->
- 全量测试（含集成测试）：x/x 用例通过
- 代码审查结果：
- 代码风格检查：
- 问题清单：详见 test-report.md
- **最终状态**：
