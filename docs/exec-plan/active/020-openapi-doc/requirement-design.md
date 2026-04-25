# 需求拆分设计 — 020-openapi-doc

## 需求描述
集成 springdoc-openapi（Spring Boot 2.7.x 兼容版本），为所有 REST Controller 添加 OpenAPI 注解，使 /swagger-ui.html 和 /v3/api-docs 可访问，并按聚合分组展示 API 文档。

## 领域分析

本任务为技术配置型任务，不涉及领域模型变更，无新增聚合根/值对象。

### 端口接口
无新增 Repository 或领域服务接口。

## 关键算法/技术方案

### 依赖选型
- **springdoc-openapi-ui 1.7.0** — Spring Boot 2.x 兼容版本
- 注意：需求描述中的 `springdoc-openapi-starter-webmvc-ui` 是 Spring Boot 3.x 专用，本项目使用 Spring Boot 2.7.18，必须使用 `springdoc-openapi-ui`

### 注解策略
- **类级别**：`@Tag(name = "聚合名称", description = "聚合描述")`
- **方法级别**：`@Operation(summary = "简要描述", description = "详细描述")`
- **响应级别**：`@ApiResponse(responseCode = "200/400/404", description = "响应说明")`（可选，当前先加 summary/description）

### 分组策略
按聚合自动分组（通过 @Tag 的 name 属性），无需额外配置 GroupedOpenApi。

### 现有 Controller 注解状态分析

| Controller | 已有 @Tag | 已有 @Operation | 需添加 |
|------------|-----------|-----------------|--------|
| ShortLinkController | Yes（短链服务） | createShortLink 有 | 其他方法无需（只有一个端点） |
| ShortLinkRedirectController | No | No | @Tag + @Operation |
| AuthController | Yes（认证服务） | register 有 | 其他 8 个方法需要 @Operation |
| CartController | Yes（购物车服务） | No | 5 个方法需要 @Operation |
| CouponController | Yes（优惠券服务） | No | 8 个方法需要 @Operation |
| OrderController | Yes（订单服务） | No | 11 个方法需要 @Operation |
| UserController | Yes（用户服务） | No | 8 个方法需要 @Operation |
| UserOrderController | No | No | @Tag + 3 个方法 @Operation |
| LinkController | No | No | @Tag + 8 个方法 @Operation |

**端点总数**：约 50 个 REST 端点需要添加 @Operation 注解。

### 访问路径
- Swagger UI：`/swagger-ui.html` 或 `/swagger-ui/index.html`
- API Docs JSON：`/v3/api-docs`

## API 设计

无新增 API，仅添加文档注解。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /swagger-ui.html | Swagger UI 页面 |
| GET | /v3/api-docs | OpenAPI JSON 文档 |

## 数据库设计

无数据库变更。

## 影响范围

| 层 | 变更 |
|---|------|
| **domain** | 无变更 |
| **application** | 无变更 |
| **infrastructure** | 无变更 |
| **adapter** | 9 个 Controller 添加 OpenAPI 注解 |
| **start** | pom.xml 添加 springdoc-openapi-ui 依赖 + 可选配置 |

## 验收条件

1. 三项预飞通过（mvn test / checkstyle / entropy-check）
2. 启动应用后访问 /swagger-ui.html 能看到 Swagger UI 页面
3. Swagger UI 中按聚合分组展示所有 API（短链服务、认证服务、购物车服务等）
4. 每个端点有清晰的 @Operation summary（中文描述）
5. /v3/api-docs 返回有效的 OpenAPI JSON

## 假设与待确认

- **假设**：不需要额外的 API 分组配置（如按版本分组），当前按聚合分组已满足需求
- **假设**：不需要 @SecurityRequirement 注解（Auth 相关的认证说明暂不处理）
- **假设**：不需要为 Request/Response 对象添加 @Schema 注解（当前只关注 Controller 端点描述）