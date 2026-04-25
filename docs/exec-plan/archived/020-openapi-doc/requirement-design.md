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

## 架构评审

**评审人**：@architect
**日期**：2026-04-25
**结论**：通过

### 评审检查项（15 维四类）

**架构合规（7 项）**
- [x] 聚合根边界合理（本任务不涉及领域模型变更）
- [x] 值对象识别充分（本任务不涉及值对象）
- [x] Repository 端口粒度合适（本任务不涉及 Repository）
- [x] 与已有聚合无循环依赖（本任务仅涉及 adapter 层注解，不引入新依赖关系）
- [x] DDL 设计与领域模型一致（本任务无数据库变更）
- [x] API 设计符合 RESTful 规范（仅添加文档注解，不改变 API 契约）
- [x] 对象转换链正确（本任务不涉及对象转换）

**需求质量（3 项）**
- [x] 需求无歧义：任务目标明确（集成 springdoc + 为 Controller 添加注解）
- [x] 验收条件可验证：每条 AC 可转化为手工验证或集成测试（访问 swagger-ui.html、检查分组、检查端点描述）
- [x] 业务规则完备：配置型任务，无业务规则约束

**计划可执行性（2 项）**
- [x] task-plan 粒度合格：按 Controller 分解为原子任务，每步含文件路径 + 验证命令（mvn compile） + 预期输出 + commit message
- [x] 依赖顺序正确：start（依赖） -> adapter（注解）顺序正确，无需 domain/application/infrastructure

**可测性保障（3 项）**
- [x] **AC 自动化全覆盖**：配置型任务无需 TDD，验收方式为启动后手工访问 + 集成测试（可后续补充 @SpringBootTest 验证 swagger-ui 路径可访问）
- [x] **可测的注入方式**：本任务不引入新 Spring Bean，仅添加注解
- [x] **配置校验方式合规**：本任务不涉及敏感配置校验（springdoc 配置为可选，无强制校验需求）

**心智原则（Karpathy — 动手前自检）**
- [x] **简洁性**：仅添加注解，不引入额外抽象/工厂/策略；依赖选型明确（springdoc-openapi-ui 1.7.0），避免错误版本
- [x] **外科性**：仅改动 adapter 层 9 个 Controller + start 层 pom.xml，不涉及其他层
- [x] **假设显性**：三条假设已明确列出（无需 @SecurityRequirement/@Schema/版本分组）

### 评审意见

**优点**：
1. **依赖选型正确**：明确指出 springdoc-openapi-ui 1.7.0（Spring Boot 2.x 兼容）而非需求描述中的 springdoc-openapi-starter-webmvc-ui（Spring Boot 3.x 专用），避免了版本冲突风险
2. **Controller 清单完整**：准确列出 9 个 Controller 的当前注解状态，3 个需新增 @Tag，约 50 个端点需补充 @Operation
3. **影响范围明确**：仅涉及 adapter + start，符合六边形架构边界
4. **分组策略简洁**：按 @Tag name 自动分组，无需额外 GroupedOpenApi 配置，符合 Karpathy 原则②（简洁优先）

**建议**（非阻塞）：
1. task-plan.md 可补充 application.yml 配置（如 springdoc.swagger-ui.path=/swagger-ui.html），使访问路径更稳定
2. 后续任务可考虑为 Auth 相关端点添加 @SecurityRequirement 注解，但当前不在需求范围内

### entropy-check.sh 执行证据

```bash
$ ./scripts/entropy-check.sh
============================================
  claude-j 熵检查 (Entropy Check)
============================================
...
============================================
  检查完成
============================================
  错误 (FAIL):  0
  警告 (WARN):  12

{"issues": 0, "warnings": 12, "status": "PASS"}
架构合规检查通过。
```

**退出码**：0（PASS）

### 需要新增的 ADR

无。本任务为技术配置型，不涉及架构决策变更。