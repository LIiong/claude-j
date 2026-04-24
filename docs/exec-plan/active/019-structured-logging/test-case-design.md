# 测试用例设计 — 019-structured-logging

## 测试范围
验证结构化日志与请求追踪能力的正确性，包括 JSON 格式日志输出、requestId 生成与响应头传播。

## 测试策略
本任务为基础设施配置任务，不涉及业务聚合建模。主要测试集中在集成测试层，验证 Filter 行为和日志配置。

---

## 0. AC 自动化覆盖矩阵（强制，动笔前先填）

| # | 验收条件（AC） | 自动化层 | 对应测试方法 | 手动备注（若有） |
|---|---|---|---|---|
| AC1 | 启动后日志为 JSON 格式，含 timestamp/level/logger/message/requestId 字段 | Start（集成） | `TraceIdIntegrationTest.should_return_x_request_id_header_when_http_request`（验证响应头） + 手动启动验证 JSON 输出 | JSON 格式在测试输出中可见（无需额外测试） |
| AC2 | 每个 HTTP 请求有唯一 requestId，响应头可见 X-Request-Id | Start（集成） | `TraceIdIntegrationTest.should_return_x_request_id_header_when_http_request` + `should_have_different_request_id_for_different_requests` | - |
| AC3 | 三项预飞全过 | Start（集成） | `mvn test` + `mvn checkstyle:check` + `./scripts/entropy-check.sh` | - |
| AC4 | 集成测试验证 requestId 贯穿请求生命周期 | Start（集成） | `TraceIdIntegrationTest.should_return_x_request_id_header_when_http_request` | - |

---

## 一、Domain 层测试场景

> 本任务为基础设施配置，不涉及 Domain 层，无需单元测试。

---

## 二、Application 层测试场景

> 本任务为基础设施配置，不涉及 Application 层，无需 Mock 单元测试。

---

## 三、Infrastructure 层测试场景

> 本任务为基础设施配置，不涉及 Infrastructure 层持久化，无需集成测试。

---

## 四、Adapter 层测试场景

> 本任务不新增 REST API，仅增加响应头，Adapter 层测试已在集成测试中覆盖。

---

## 五、集成测试场景（全链路）

| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E1 | requestId 响应头存在 | GET /actuator/health | 200 + X-Request-Id header 存在 |
| E2 | requestId 格式正确 | GET /actuator/health | requestId 为 32 字符 hex（[a-f0-9]{32}） |
| E3 | requestId 唯一性 | 两次 GET /actuator/health | requestId 不同 |
| E4 | MDC 清理 | 请求结束后的线程池复用场景 | requestId 已从 MDC 清除（由 finally 块保证） |

---

## 六、代码审查检查项

- [x] 依赖方向正确（仅改动 start 模块，不依赖 domain/application/infrastructure 业务层）
- [x] domain 模块无变更（不涉及）
- [x] TraceIdFilter 实现 Filter 接口正确（javax.servlet.Filter）
- [x] MDC.put/remove 在 try-finally 块中正确使用
- [x] Filter 注册使用 FilterRegistrationBean，优先级最高
- [x] logback-spring.xml JSON 格式配置正确
- [x] Java 8 兼容（无 var/records/text blocks/List.of）

## 七、代码风格检查项

- [x] Java 8 兼容（无 var、records、text blocks、List.of）
- [x] TraceIdFilter 使用纯 Java 实现，无 Lombok 注解
- [x] TraceIdConfig 使用 @Configuration + @Bean 标准配置
- [x] 包结构符合 com.claudej.config（start 模块配置包）
- [x] 测试命名符合 should_xxx_when_yyy 规范
- [x] 常量命名符合规范（REQUEST_ID_HEADER, REQUEST_ID_MDC_KEY）