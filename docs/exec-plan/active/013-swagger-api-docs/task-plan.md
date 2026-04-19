# 013-swagger-api-docs 任务计划

## 原子任务

### T1: 添加 springdoc 依赖
**验证**: `grep -q "springdoc" pom.xml && echo "PASS"`

### T2: 创建 OpenAPI 配置类
**验证**: `[ -f claude-j-adapter/src/main/java/com/claudej/adapter/common/config/OpenApiConfig.java ] && echo "PASS"`

### T3: 配置 API 分组
**验证**: 启动后访问 /swagger-ui.html 看到分组

### T4: 配置 JWT 认证
**验证**: 有 Authorize 按钮，可输入 Bearer Token

### T5: 添加 Controller 注解
**验证**: 所有 Controller 有 @Tag 注解

### T6: 添加 DTO 注解
**验证**: Request/Response 类有 @Schema 注解

### T7: 验证 swagger-ui 访问
**验证**: `curl -s http://localhost:8081/swagger-ui.html | grep -q "Swagger" && echo "PASS"`

### T8: 验证 API JSON 导出
**验证**: `curl -s http://localhost:8081/v3/api-docs | grep -q "openapi" && echo "PASS"`

### T9: 三项预飞
**验证**: `mvn test && mvn checkstyle:check && ./scripts/entropy-check.sh`
