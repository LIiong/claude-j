# 013-swagger-api-docs 需求设计

## 需求概述

集成 Swagger/OpenAPI 3.0 接口文档，支持 API 分组（按聚合）、JWT 认证、离线文档导出。

## 领域分析

本任务为**基础设施增强**，不涉及领域模型变更，仅需在 adapter 层添加配置和注解。

### 目标功能

1. **在线文档**: `/swagger-ui.html` 访问 Swagger UI
2. **API 分组**: 按聚合分组（user/order/cart/coupon/shortlink/auth）
3. **JWT 认证**: 支持 Bearer Token 认证方式
4. **离线文档**: 支持导出 OpenAPI JSON/YAML

## 技术选型

- **springdoc-openapi**: Spring Boot 2.x 推荐方案（替代已停更的 Springfox）
- 版本: 1.7.0（兼容 Spring Boot 2.7.18）

## 实施计划

### P1: 依赖与配置
- [ ] 添加 springdoc-openapi-ui 依赖
- [ ] 创建 OpenAPI 配置类
- [ ] 配置 API 分组
- [ ] 配置 JWT 认证

### P2: 注解完善
- [ ] 为 Controller 添加 @Tag
- [ ] 为 Request/Response 添加 @Schema
- [ ] 为方法添加 @Operation

### P3: 验证与导出
- [ ] 验证 swagger-ui.html 可访问
- [ ] 验证各分组正确
- [ ] 验证 JWT 认证可用
- [ ] 验证离线 JSON 导出

## 验收条件

1. ✅ 访问 http://localhost:8081/swagger-ui.html 显示文档
2. ✅ API 按聚合分组展示
3. ✅ 可输入 JWT Token 进行认证测试
4. ✅ 支持导出 OpenAPI JSON
5. ✅ 三项预飞通过

## 风险

- 低风险：标准库集成
- 注意：避免 springfox 与 springdoc 冲突
