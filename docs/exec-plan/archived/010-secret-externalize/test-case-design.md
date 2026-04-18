# 测试用例设计 — 010-secret-externalize

## 测试范围
JWT Secret 外置化配置验证：启动校验器、配置外置化、CI 环境变量注入。

## 测试策略
按测试金字塔分层测试 + 代码审查 + 风格检查。本任务为配置变更，无新聚合，重点在基础设施层启动校验器测试和集成测试。

---

## 一、Domain 层测试场景

N/A — 本任务为配置变更，不涉及新领域模型。

---

## 二、Application 层测试场景

N/A — 本任务为配置变更，不涉及应用服务。

---

## 三、Infrastructure 层测试场景

### JwtSecretValidator 启动校验器
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| I1 | secret 为 null | jwtSecret = null | validator.run(args) | 抛 IllegalStateException，提示 JWT_SECRET required |
| I2 | secret 为空字符串 | jwtSecret = "" | validator.run(args) | 抛 IllegalStateException，提示 JWT_SECRET required |
| I3 | secret 长度不足 32 | jwtSecret = "short-secret-16" | validator.run(args) | 抛 IllegalStateException，提示 at least 32 characters |
| I4 | secret 长度等于 32 | jwtSecret = "12345678901234567890123456789012" | validator.run(args) | 正常通过 |
| I5 | secret 长度大于 32 | jwtSecret = "valid-secret-key-at-least-32-bytes-long-ok" | validator.run(args) | 正常通过 |

---

## 四、Adapter 层测试场景

N/A — 本任务不涉及 API 变更。

---

## 五、集成测试场景（全链路）

### 启动场景验证
| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E1 | dev 环境正常启动 | 使用 application-dev.yml 启动 | 应用正常启动，JWT 功能可用 |
| E2 | 未设置 JWT_SECRET 启动失败 | 在非 dev profile 下不设置 JWT_SECRET 启动 | 启动失败，提示 JWT_SECRET required |
| E3 | CI 环境注入测试 | 模拟 CI 环境 JWT_SECRET 注入 | 测试通过，环境变量生效 |

### JWT Token 功能验证
| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E4 | Token 生成与验证 | 调用登录接口获取 Token，再验证 Token | Token 生成成功，验证通过 |
| E5 | Token 过期刷新 | 使用 Refresh Token 获取新 Token | 新 Access Token 生成成功 |

---

## 六、代码审查检查项

- [x] 依赖方向正确（adapter -> application -> domain <- infrastructure）
- [x] JwtSecretValidator 位置合理（infrastructure 层 config 包）
- [x] 配置外置化策略正确（application.yml 无默认值，application-dev.yml 提供默认值）
- [x] CI 环境变量注入完整（build/unit-tests/integration-tests 3 个 jobs）
- [x] 文档完整（docs/ops/secrets.md）

---

## 七、代码风格检查项

- [x] Java 8 兼容（无 var、records、text blocks、List.of）
- [x] 命名规范：JwtSecretValidator
- [x] 包结构符合 com.claudej.infrastructure.auth.config
- [x] 测试命名 should_xxx_when_xxx
