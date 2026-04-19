# Handoff: 013-swagger-api-docs

## 当前状态

```yaml
task-id: 013-swagger-api-docs
from: dev
to: qa
status: pending-review
pre-flight:
  mvn-test: pass       # 142 tests run, 0 failures, 0 errors
  checkstyle: pass     # Exit 0
  entropy-check: pass  # 0 FAIL
```

## 变更摘要
- 添加 springdoc-openapi-ui 1.7.0 依赖
- 创建 OpenApiConfig 配置类
- 配置 7 个 API 分组（user/order/cart/coupon/shortlink/auth/all）
- 配置 JWT Bearer Token 认证
- 为 6 个 Controller 添加 @Tag 注解
- 为核心 DTO 添加 @Schema 注解（示例）

## 验证记录
- Swagger UI: ✅ http://localhost:8081/swagger-ui.html
- API JSON: ✅ http://localhost:8081/v3/api-docs
- 三项预飞: ✅ 全部通过

## 访问地址

| 环境 | URL |
|------|-----|
| Swagger UI | http://localhost:8081/swagger-ui.html |
| API Docs (JSON) | http://localhost:8081/v3/api-docs |
| API Docs (YAML) | http://localhost:8081/v3/api-docs.yaml |

## 文档清单
- requirement-design.md: ✅
- task-plan.md: ✅
- dev-log.md: ✅

## 下一步
QA 验收或直接进入 Ship（纯配置任务）
