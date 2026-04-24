# 测试用例设计 — 018-actuator-health

## 测试类型

- **集成测试**：验证 Actuator 端点在 Spring Boot 上下文中可用

## 测试环境

- **Profile**: dev（使用 H2 内存数据库）
- **测试类**: `ActuatorHealthIntegrationTest`
- **位置**: `claude-j-start/src/test/java/com/claudej/actuator/`

## AC 与测试用例映射

| AC | 测试方法 | 验证内容 |
|----|---------|---------|
| 1. /actuator/health 返回 200 | `should_return_200_when_actuator_health_endpoint` | 状态码 200 + 响应包含 status 字段 |
| 2. liveness 端点可用 | `should_return_200_when_actuator_health_liveness_endpoint` | 状态码 200 + 响应包含 status 字段 |
| 3. readiness 端点可用 | `should_return_200_when_actuator_health_readiness_endpoint` | 状态码 200 + 响应包含 status 字段 |
| 4. dev 环境 show-details: always | `should_show_details_when_dev_environment_health_endpoint` | 响应包含 components 字段 |
| 5. /actuator/info 可用 | `should_return_200_when_actuator_info_endpoint` | 状态码 200 |
| 6. dev 环境 metrics 端点可用 | `should_return_200_when_actuator_metrics_endpoint_in_dev` | 状态码 200 |

## 测试命名规范

所有测试方法遵循 `should_{预期行为}_when_{条件}` 格式（ArchUnit 强制）。

## 测试覆盖范围

### 端点可用性

| 端点 | 测试覆盖 | 环境差异 |
|------|---------|---------|
| `/actuator/health` | 基础健康状态 | dev: show-details=always, staging: when-authorized, prod: never |
| `/actuator/health/liveness` | K8s 存活探针 | 所有环境启用 |
| `/actuator/health/readiness` | K8s 就绪探针（含 db 检查） | 所有环境启用 |
| `/actuator/info` | 应用信息 | dev/staging 启用，prod 禁用（防止信息泄露） |
| `/actuator/metrics` | 指标数据 | 仅 dev 启用 |

### 环境配置验证

- **dev**: 通过集成测试验证（`@ActiveProfiles("dev")`）
- **staging/prod**: 配置文件检查（需在部署时验证）

## 手动验证步骤（QA 参考）

```bash
# 1. 启动开发环境
mvn spring-boot:run -pl claude-j-start -Dspring-boot.run.profiles=dev

# 2. 验证健康端点
curl -s http://localhost:8080/actuator/health | jq .

# 3. 验证 liveness 端点
curl -s http://localhost:8080/actuator/health/liveness | jq .

# 4. 验证 readiness 端点
curl -s http://localhost:8080/actuator/health/readiness | jq .

# 5. 验证 info 端点
curl -s http://localhost:8080/actuator/info | jq .

# 6. 验证 metrics 端点（仅 dev）
curl -s http://localhost:8080/actuator/metrics | jq .
```

## 测试数据

无需测试数据。Actuator 端点为基础设施配置，不依赖业务数据。

---

**创建日期**: 2026-04-24
**创建者**: @dev