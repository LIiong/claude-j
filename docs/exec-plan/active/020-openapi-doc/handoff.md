---
task-id: "020-openapi-doc"
from: dev
to: architect
status: pending-review
timestamp: "2026-04-25T10:30:00"
pre-flight:
  mvn-test: pending            # Spec阶段无需运行
  checkstyle: pending          # Spec阶段无需运行
  entropy-check: pending       # Spec阶段无需运行
  tdd-evidence: []             # 配置型任务，无需TDD
artifacts:
  - requirement-design.md
  - task-plan.md
  - dev-log.md
summary: ""
---

# 交接文档

## 交接说明

**Spec 阶段完成**，请求 @architect 评审以下设计：

### 设计要点
1. **依赖选型**：使用 `springdoc-openapi-ui 1.7.0`（Spring Boot 2.x 兼容版本），而非需求描述中的 `springdoc-openapi-starter-webmvc-ui`（Spring Boot 3.x 专用）
2. **注解策略**：类级别 @Tag + 方法级别 @Operation(summary, description)
3. **分组策略**：按聚合自动分组（通过 @Tag name），无需额外 GroupedOpenApi 配置
4. **影响范围**：仅 adapter 层 9 个 Controller + start 层 pom.xml

### 现有 Controller 注解状态
- 已有 @Tag：ShortLinkController、AuthController、CartController、CouponController、OrderController、UserController（6 个）
- 已有部分 @Operation：ShortLinkController（1 个）、AuthController（1 个）
- 需新增 @Tag：ShortLinkRedirectController、LinkController、UserOrderController（3 个）
- 需补充 @Operation：约 50 个端点

### 假设与待确认
- 不需要 @SecurityRequirement 注解（Auth 认证说明暂不处理）
- 不需要为 Request/Response 对象添加 @Schema 注解
- 不需要额外的 API 版本分组配置

---

## 评审回复
{architect 填写}

---

## 交接历史

### 2026-04-25 — @dev -> @architect
- 状态：pending-review
- 说明：Spec 阶段完成，请求架构评审