# 012-multi-profile 开发日志

## 设计阶段
- 已分析当前配置结构，确定重构范围
- 确认无领域代码变更，纯配置任务

## 执行记录

### T1: 重构 application.yml
状态: ✅ 已完成
变更: 移除 `spring.profiles.active: dev`，保留通用配置

### T2: 创建 application-staging.yml
状态: ✅ 已完成
配置: MySQL 数据源、${ENV_VAR} 占位、INFO 日志、受限 actuator

### T3: 创建 application-prod.yml
状态: ✅ 已完成
配置: MySQL 数据源、连接池优化、WARN 日志、仅 health actuator

### T4: 更新 Dockerfile
状态: ✅ 已完成
变更: `ENV SPRING_PROFILES_ACTIVE=prod`，移除 dev 硬编码

### T5: 创建 profiles.md
状态: ✅ 已完成
内容: 5 维矩阵 × 3 环境、环境变量清单、故障排查指南

### T6: dev 启动测试
状态: ✅ 已完成
结果: dev 启动成功，actuator/health 返回 UP

### T7: staging 缺 env 报错测试
状态: ✅ 已完成
结果: staging 缺 MYSQL_HOST 时报错 `UnknownHostException: ${MYSQL_HOST}`，明确指向变量名

### T8: 三项预飞
状态: ✅ 已完成
结果: mvn test (142 tests pass) + checkstyle (pass) + entropy-check (0 FAIL) 全部通过

## 与原始设计变更
- 无变更

## 待确认
- 无
