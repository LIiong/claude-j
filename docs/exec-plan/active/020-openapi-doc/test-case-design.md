# 测试用例设计 — 020-openapi-doc

## 测试范围

验证 springdoc-openapi-ui 集成配置正确，Swagger UI 可访问，所有 Controller 有正确的 OpenAPI 注解。

## 测试策略

本任务为配置型任务，不涉及业务逻辑变更。验收方式为：
- 代码审查：确认注解完整性、命名规范
- 手工验证：启动应用后访问 Swagger UI 和 API Docs
- 集成测试（可选）：验证 springdoc 端点可访问

---

## 0. AC 自动化覆盖矩阵（强制，动笔前先填）

| # | 验收条件（AC） | 自动化层 | 对应测试方法 | 手动备注（若有） |
|---|---|---|---|---|
| AC1 | 三项预飞通过（mvn test / checkstyle / entropy-check） | — | `mvn test` + `mvn checkstyle:check` + `./scripts/entropy-check.sh` | — |
| AC2 | 启动应用后访问 /swagger-ui.html 能看到 Swagger UI 页面 | Start（集成） | 待编写 `SwaggerUiIntegrationTest.should_accessSwaggerUi_when_appStarted` | 当前阻塞：application.yml 配置错误 |
| AC3 | Swagger UI 中按聚合分组展示所有 API | Start（集成） | 待编写 `SwaggerUiIntegrationTest.should_displayAllGroups_when_swaggerUiAccessed` | 当前阻塞：应用无法启动 |
| AC4 | 每个端点有清晰的 @Operation summary（中文描述） | Adapter（代码审查） | 手工代码审查确认 | 代码审查已完成，注解完整 |
| AC5 | /v3/api-docs 返回有效的 OpenAPI JSON | Start（集成） | 待编写 `ApiDocsIntegrationTest.should_returnValidJson_when_apiDocsAccessed` | 当前阻塞：应用无法启动 |

---

## 一、Domain 层测试场景

> 本任务为配置型任务，不涉及 Domain 层变更，已按模板说明省略。

---

## 二、Application 层测试场景

> 本任务为配置型任务，不涉及 Application 层变更，已按模板说明省略。

---

## 三、Infrastructure 层测试场景

> 本任务为配置型任务，不涉及 Infrastructure 层变更，已按模板说明省略。

---

## 四、Adapter 层测试场景

> 本任务为配置型任务，不涉及 Adapter 层业务逻辑测试。验证方式为代码审查确认注解完整性。

### 代码审查检查项

| # | 检查项 | 预期结果 |
|---|--------|----------|
| C1 | ShortLinkController 有 @Tag("短链服务") + @Operation | ✅ 已有 |
| C2 | ShortLinkRedirectController 有 @Tag("短链跳转") + @Operation | ✅ 新增 |
| C3 | LinkController 有 @Tag("链接管理") + 8 个 @Operation | ✅ 新增 |
| C4 | UserOrderController 有 @Tag("用户订单") + 3 个 @Operation | ✅ 新增 |
| C5 | AuthController 有 @Tag("认证服务") + 所有方法 @Operation | ✅ 新增 7 个 |
| C6 | CartController 有 @Tag("购物车服务") + 5 个 @Operation | ✅ 新增 |
| C7 | CouponController 有 @Tag("优惠券服务") + 7 个 @Operation | ✅ 新增 |
| C8 | OrderController 有 @Tag("订单服务") + 10 个 @Operation | ✅ 新增 |
| C9 | UserController 有 @Tag("用户服务") + 8 个 @Operation | ✅ 新增 |

---

## 五、集成测试场景（全链路）

> **阻塞说明**：当前 application.yml 存在重复 `spring` key 配置错误，应用无法启动。集成测试待修复后执行。

| # | 场景 | 操作 | 预期结果 | 状态 |
|---|------|------|----------|------|
| E1 | Swagger UI 可访问 | GET /swagger-ui/index.html | HTTP 200，返回 HTML 页面 | ⏸️ 阻塞 |
| E2 | API Docs JSON 有效 | GET /v3/api-docs | HTTP 200，返回有效 JSON | ⏸️ 阻塞 |
| E3 | 分组正确显示 | 检查 /v3/api-docs 响应 | 包含所有 9 个 Tag 分组 | ⏸️ 阻塞 |

---

## 六、代码审查检查项

- [x] 依赖方向正确（adapter → application → domain ← infrastructure）
- [x] domain 模块无 Spring/框架 import（本任务不涉及）
- [x] Controller 注解完整（@Tag + @Operation）
- [x] 注解命名规范（中文 summary）
- [x] springdoc-openapi-ui 依赖版本正确（1.7.0）

---

## 七、代码风格检查项

- [x] Java 8 兼容（无 var、records、text blocks、List.of）— 注解使用无问题
- [x] 注解 import 正确（io.swagger.v3.oas.annotations）
- [x] 注解位置正确（类级别 @Tag，方法级别 @Operation）