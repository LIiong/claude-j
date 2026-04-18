# 任务执行计划 — 010-secret-externalize

## 任务状态跟踪

<!-- 状态流转：待办 → 进行中 → 单测通过 → 待验收 → 验收通过 / 待修复 -->

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Infrastructure: JwtSecretValidator 启动校验器 | dev | 单测通过 | 4 个测试通过 |
| 2 | Infrastructure: JwtTokenServiceImpl 移除 fallback | dev | 单测通过 | |
| 3 | Infrastructure: 测试配置添加 jwt.secret | dev | 单测通过 | |
| 4 | Start: application.yml 外置化配置 | dev | 单测通过 | |
| 5 | Start: application-dev.yml 添加默认值 | dev | 单测通过 | |
| 6 | CI: 注入 JWT_SECRET 环境变量 | dev | 单测通过 | 3 个 job 已注入 |
| 7 | Docs: 新增 secrets.md 运维文档 | dev | 单测通过 | |
| 8 | 全量 mvn test | dev | 单测通过 | 44 tests, 0 failures |
| 9 | QA: 测试用例设计 | qa | 已完成 | test-case-design.md |
| 10 | QA: 验收测试 + 代码审查 | qa | 已完成 | 50 tests, 0 failures |
| 11 | QA: 启动场景验证 | qa | 已完成 | 6 个集成测试通过 |

## 执行顺序
infrastructure → start → ci → docs → 全量测试 → QA 验收

## 原子任务分解（每项 10–15 分钟，单会话可完成并 commit）

### 1.1 Infrastructure: JwtSecretValidator
- **文件**：`claude-j-infrastructure/src/main/java/com/claudej/infrastructure/auth/config/JwtSecretValidator.java`
- **测试**：`claude-j-infrastructure/src/test/java/com/claudej/infrastructure/auth/config/JwtSecretValidatorTest.java`
- **骨架**（Red 阶段先写测试）：
  ```java
  @Test
  void should_throw_when_secret_is_null() { ... }
  @Test
  void should_throw_when_secret_is_empty() { ... }
  @Test
  void should_throw_when_secret_length_less_than_32() { ... }
  @Test
  void should_pass_when_secret_is_valid() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-infrastructure -Dtest=JwtSecretValidatorTest`
- **预期输出**：`Tests run: 4, Failures: 0, Errors: 0`
- **commit**：`feat(infrastructure): JwtSecretValidator 启动校验器`

### 1.2 Infrastructure: JwtTokenServiceImpl 移除 fallback
- **文件**：`claude-j-infrastructure/src/main/java/com/claudej/infrastructure/auth/token/JwtTokenServiceImpl.java`
- **变更**：移除构造参数的默认值，改为必需配置
- **验证命令**：`mvn compile -pl claude-j-infrastructure`
- **预期输出**：编译通过
- **commit**：`refactor(infrastructure): JwtTokenServiceImpl 移除 fallback 默认值`

### 1.3 Infrastructure: 测试配置添加 jwt.secret
- **文件**：`claude-j-infrastructure/src/test/resources/application.yml`
- **变更**：添加 jwt.secret 配置
- **验证命令**：`mvn test -pl claude-j-infrastructure`
- **预期输出**：测试通过
- **commit**：`test(infrastructure): 测试配置添加 jwt.secret`

### 2.1 Start: application.yml 外置化
- **文件**：`claude-j-start/src/main/resources/application.yml`
- **变更**：`secret: ${JWT_SECRET:}`
- **验证命令**：`cat claude-j-start/src/main/resources/application.yml | grep -A2 "jwt:"`
- **预期输出**：配置正确变更
- **commit**：`config(start): JWT secret 外置化为环境变量`

### 2.2 Start: application-dev.yml 添加默认值
- **文件**：`claude-j-start/src/main/resources/application-dev.yml`
- **变更**：添加 jwt.secret 默认值（>=32字符）
- **验证命令**：`cat claude-j-start/src/main/resources/application-dev.yml | grep jwt`
- **预期输出**：配置已添加
- **commit**：`config(start): dev 环境添加 JWT secret 默认值`

### 3.1 CI: 注入 JWT_SECRET 环境变量
- **文件**：`.github/workflows/ci.yml`
- **变更**：在 build/unit-tests/integration-tests jobs 添加 env.JWT_SECRET
- **验证命令**：`cat .github/workflows/ci.yml | grep -A1 "JWT_SECRET"`
- **预期输出**：环境变量已配置
- **commit**：`ci: CI 任务注入 JWT_SECRET 测试密钥`

### 4.1 Docs: secrets.md 运维文档
- **文件**：`docs/ops/secrets.md`
- **骨架**：环境变量说明、生产 Vault 扩展路径、本地开发指南
- **验证命令**：`cat docs/ops/secrets.md`
- **预期输出**：文档完整
- **commit**：`docs: 新增 secrets.md 运维文档`

### 5.1 全量验证
- **验证命令**：`mvn test`
- **预期输出**：`Tests run: x, Failures: 0, Errors: 0`
- **验证命令**：`mvn checkstyle:check`
- **预期输出**：`Exit 0`
- **验证命令**：`./scripts/entropy-check.sh`
- **预期输出**：`12/12 checks passed`
- **commit**：`test: 全量验证通过`

## 开发完成记录

- 全量 `mvn clean test`：44/44 用例通过
- 架构合规检查：entropy-check 0 errors, 10 warnings
- 通知 @qa 时间：2026-04-17

## QA 验收记录

- 全量测试（含集成测试）：50/50 用例通过
- 代码审查结果：✅ 通过，依赖方向正确，JwtSecretValidator 位置合理
- 代码风格检查：✅ 通过，0 violations
- 问题清单：详见 test-report.md（0 个阻塞性问题）
- **最终状态**：✅ 验收通过
- 验收时间：2026-04-18
- 提交 commit：`58f05dd`
