---
task-id: "010-secret-externalize"
from: dev
to: architect
status: pending-review
timestamp: "2025-04-17T13:58:00Z"
pre-flight:
  mvn-test: pending
  checkstyle: pending
  entropy-check: pending
artifacts:
  - requirement-design.md
  - task-plan.md
  - dev-log.md
summary: "JWT secret 外置化：将 secret 从 application.yml 移入环境变量 JWT_SECRET，新增启动校验器确保非空且 >=32 字符，dev 环境保留默认值便于本地开发"
---

## 设计要点

### 核心变更
1. **application.yml**: `secret: ${JWT_SECRET:}` — 无默认值，强制依赖环境变量
2. **application-dev.yml**: 添加默认 JWT secret，便于本地开发启动
3. **JwtSecretValidator**: Spring Boot ApplicationRunner 实现，启动时校验 secret 非空且长度 >=32
4. **JwtTokenServiceImpl**: 移除 fallback 默认值，完全依赖外部配置
5. **CI**: 注入 JWT_SECRET 环境变量供测试使用
6. **docs/ops/secrets.md**: 运维文档说明配置方式

### 关键决策
- Validator 放在 infrastructure 层 auth/config 包（基础设施配置管理）
- 校验失败抛 IllegalStateException 阻止启动（fail-fast）
- 开发环境保留默认值（application-dev.yml 自动加载）

### 无领域模型变更
此任务为配置安全增强，不涉及聚合、实体、值对象或 Repository 端口。
