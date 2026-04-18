# 012-multi-profile 需求设计

## 需求概述

引入 staging / prod 多 profile 配置体系，实现环境配置的清晰分离与生产安全。

## 领域分析

本任务为**纯配置重构**，不涉及领域模型、API 或数据库变更。

### 当前问题
1. `application.yml` 硬编码 `spring.profiles.active: dev`，生产环境需手动覆盖
2. 缺少 staging/production 环境的配置占位文件
3. 环境配置差异（数据源/JWT/日志/actuator/CORS）未文档化
4. Dockerfile 默认激活 dev profile，存在生产安全隐患

### 目标配置架构

```
application.yml          # 通用配置（jackson/mybatis-plus/server）
application-dev.yml      # H2 + 调试日志（已存在）
application-staging.yml  # 占位配置（${ENV_VAR}）
application-prod.yml     # 占位配置（${ENV_VAR}）
```

## 配置矩阵

| 维度 | dev | staging | prod |
|-----|-----|---------|------|
| **数据源** | H2 (内存) | MySQL (占位) | MySQL (占位) |
| **JWT** | hardcoded | ${JWT_SECRET} | ${JWT_SECRET} |
| **日志** | DEBUG | INFO | WARN |
| **Actuator** | 全开 | health 只读 | health 只读 |
| **CORS** | 全允许 | 受限域 | 受限域 |

## 实施计划

### P1: 配置重构
- [ ] 重构 application.yml：移除 profile 激活，保留通用配置
- [ ] 创建 application-staging.yml：占位式配置 + 缺失 env 报错
- [ ] 创建 application-prod.yml：占位式配置 + 缺失 env 报错
- [ ] 更新 Dockerfile：SPRING_PROFILES_ACTIVE=prod

### P2: 文档
- [ ] 创建 docs/ops/profiles.md：5 维配置矩阵 + 环境变量清单

## 验收条件

1. ✅ dev 启动 ok：`mvn spring-boot:run -pl claude-j-start -Dspring-boot.run.profiles=dev`
2. ✅ staging 缺 env 时报错指向具体变量名（启动失败 + 明确错误）
3. ✅ profiles.md 矩阵完整（5 维度 × 3 环境）
4. ✅ 三项预飞通过：mvn test + checkstyle + entropy-check

## 风险

- 低风险：纯配置变更，不影响业务逻辑
- 注意：确保 dev profile 仍能正常启动（H2 配置保留）
