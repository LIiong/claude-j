# 开发日志 — 018-actuator-health

## 问题记录

### Issue 1: liveness/readiness 端点未启用
- **Issue**: 测试运行前，liveness 和 readiness 端点返回 404
- **Root Cause**: application.yml 未启用 `management.endpoint.health.probes.enabled=true`
- **Fix**: 在 application.yml 和各环境配置文件中添加 probes.enabled=true 和 health group 配置
- **Verification**:
  ```bash
  # 修复前
  mvn test -pl claude-j-start -Dtest=ActuatorHealthIntegrationTest
  # 输出: Tests run: 6, Failures: 2, Errors: 0 (liveness/readiness 返回 404)

  # 修复后
  mvn test -pl claude-j-start -Dtest=ActuatorHealthIntegrationTest
  # 输出: Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
  ```

## 变更记录

<!-- 与原设计（requirement-design.md）不一致的变更 -->
<!-- 格式：变更内容 + 变更原因 -->
- 无与原设计不一致的变更。

## Red-Green-Refactor 循环记录

### Round 1: liveness/readiness 端点
1. **Red**: 创建 `ActuatorHealthIntegrationTest`，运行测试 → 2 failures（liveness/readiness 404）
2. **Green**: 修改 application.yml/application-dev.yml 启用 probes → 6 tests pass
3. **Refactor**: 无需重构（配置结构已按设计模板组织）