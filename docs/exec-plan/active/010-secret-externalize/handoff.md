---
task-id: "010-secret-externalize"
from: dev
to: qa
status: pending-review
timestamp: "2026-04-17T22:25:00-04:00"
pre-flight:
  mvn-test: pass       # Tests: 44 run, 0 failures, 0 errors
  checkstyle: pass     # Exit 0, 0 violations
  entropy-check: pass  # 12/12 checks passed, 0 errors, 10 warnings (auth聚合遗留)
summary: |
  JWT Secret 外置化任务完成，共 7 个 commit：
  - JwtSecretValidator 启动校验器（4 个测试）
  - JwtTokenServiceImpl 移除 fallback
  - infrastructure 测试配置添加 jwt.secret
  - application.yml 外置化为 ${JWT_SECRET:}
  - application-dev.yml 添加本地默认值
  - CI 配置注入 JWT_SECRET 环境变量（3 个 jobs）
  - 新增 docs/ops/secrets.md 运维文档
---

## 变更清单

### 新增文件
- `claude-j-infrastructure/src/main/java/com/claudej/infrastructure/auth/config/JwtSecretValidator.java`
- `claude-j-infrastructure/src/test/java/com/claudej/infrastructure/auth/config/JwtSecretValidatorTest.java`
- `docs/ops/secrets.md`

### 修改文件
- `claude-j-infrastructure/src/main/java/com/claudej/infrastructure/auth/token/JwtTokenServiceImpl.java` - 移除 fallback 默认值
- `claude-j-infrastructure/src/test/resources/application.yml` - 添加测试用 jwt.secret
- `claude-j-start/src/main/resources/application.yml` - secret 外置化为 ${JWT_SECRET:}
- `claude-j-start/src/main/resources/application-dev.yml` - 添加本地开发默认值
- `.github/workflows/ci.yml` - build/unit-tests/integration-tests jobs 注入 JWT_SECRET

## 关键设计点

1. **JwtSecretValidator**: ApplicationRunner 实现，启动时校验 JWT_SECRET 非空且长度 >=32
2. **配置分层**: application.yml 无默认值（强制依赖环境变量），application-dev.yml 提供本地默认值
3. **CI 注入**: 三个 jobs（build, unit-tests, integration-tests）均已配置 JWT_SECRET 环境变量

## 测试覆盖

- JwtSecretValidatorTest: 4 个测试（null, empty, <32, valid）
- 全量测试: 44 个通过

## 验收要点

1. 未设置 JWT_SECRET 时应用启动失败并提示明确错误
2. 设置 JWT_SECRET（长度>=32）后应用正常启动
3. JWT_SECRET 长度 <32 时应用启动失败
4. 开发环境使用 application-dev.yml 可正常启动
5. CI 测试通过（环境变量已注入）
