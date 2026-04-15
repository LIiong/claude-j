# Java 测试规则（Claude Rules）

## 适用范围
- 适用于 `src/test/java/` 下所有测试代码。
- 目标：保障分层测试策略一致、行为可验证、测试可维护。

## MUST（强制）

### 测试分层
- Domain：仅纯单元测试（JUnit 5 + AssertJ），不得启动 Spring。
- Application：使用 Mockito 隔离外部端口，验证用例编排和调用顺序。
- Infrastructure：使用 `@SpringBootTest` + H2，验证持久化与转换往返。
- Adapter：使用 `@WebMvcTest` + MockMvc，验证 HTTP 契约和入参校验。

### 测试设计
- 测试文件应与生产代码目录结构镜像对应。
- 测试方法命名必须采用 `should_xxx_when_xxx` 语义格式。
- 测试结构必须使用 AAA（Arrange / Act / Assert）。
- 关键业务规则必须覆盖：状态转换、不变量、异常场景、边界值。

### 断言与验证
- 必须断言业务结果和副作用（如仓储保存是否发生）。
- Web 层必须断言响应结构与状态码（200/400/404/500 等）。
- 集成测试必须断言 DO <-> Domain 映射准确性。

## MUST NOT（禁止）
- 禁止跨层测试依赖（例如 Domain 测试依赖 Infrastructure）。
- 禁止在 Domain/Application 单测中使用 Spring 上下文。
- 禁止在单测中连接真实数据库。
- 禁止 `Thread.sleep()`、`@Order`、共享可变测试状态。
- 禁止直接测试私有方法（通过公开行为验证）。

## 执行检查（每次改动后）
1. 新增功能时同步补齐对应层测试。
2. 运行 `mvn test`，确保所有测试与 ArchUnit 规则通过。
3. 若是 API 改动，重点检查 `adapter` 测试是否覆盖请求校验与错误响应。
