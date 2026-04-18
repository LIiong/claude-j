# 需求拆分设计 — 010-secret-externalize

## 需求描述

将 JWT secret 从 application.yml 外置化为环境变量 JWT_SECRET，增强生产环境安全性：
- application.yml 中的 secret 改为 ${JWT_SECRET:} 占位符，无默认值
- application-dev.yml 补本地开发默认值供开发启动
- 新增 JwtSecretValidator 组件在启动时校验 JWT_SECRET 非空且长度 >=32
- 新增 docs/ops/secrets.md 运维文档说明环境变量配置
- CI 配置在测试任务中注入测试密钥

## 领域分析

这是一个配置安全增强任务，不涉及新聚合或领域模型，属于基础设施层配置管理。

### 关键设计点

**1. 配置外置化策略**
- 生产配置 (application.yml): `${JWT_SECRET:}` — 无默认值，强制依赖环境变量
- 开发配置 (application-dev.yml): 提供本地默认值，便于开发调试
- 启动校验: Spring Boot `ApplicationRunner` 实现启动时校验

**2. JwtSecretValidator 设计**
- 实现 `org.springframework.boot.ApplicationRunner` 接口
- 注入 `@Value("${jwt.secret}")` 获取实际配置值
- 校验规则:
  - 非空检查: `secret != null && !secret.isEmpty()`
  - 长度检查: `secret.length() >= 32`
- 校验失败抛出 `IllegalStateException` 阻止启动

**3. 现有代码适配**
- `JwtTokenServiceImpl` 当前有 fallback 默认值，需移除 fallback，改为强依赖配置
- 构造函数中直接使用注入的 secret，不再做 pad 处理（因已在 validator 中校验长度）

### 端口接口

无新增 Repository 或 DomainService 端口，此任务为纯配置变更。

## 关键算法/技术方案

**1. 启动校验机制**
```java
@Component
public class JwtSecretValidator implements ApplicationRunner {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public void run(ApplicationArguments args) {
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT_SECRET environment variable is required");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
        }
    }
}
```

**2. 配置变更方案**
| 文件 | 变更前 | 变更后 |
|------|--------|--------|
| application.yml | `secret: claude-j-secret-key...` | `secret: ${JWT_SECRET:}` |
| application-dev.yml | (无 jwt 配置) | 添加 `jwt.secret: dev-jwt-secret-key-at-least-32-bytes` |

**3. CI 环境变量注入**
- 在 `ci.yml` 的 build/test jobs 中添加 `env: JWT_SECRET: test-jwt-secret-key-for-ci-at-least-32-bytes`

## API 设计

无新增 REST API，此任务为配置变更。

## 数据库设计

无 DDL 变更，不涉及数据库。

## 影响范围

- **start**:
  - `claude-j-start/src/main/resources/application.yml` — 修改 jwt.secret 配置
  - `claude-j-start/src/main/resources/application-dev.yml` — 添加 jwt.secret 默认值

- **infrastructure**:
  - `claude-j-infrastructure/src/main/java/com/claudej/infrastructure/auth/token/JwtTokenServiceImpl.java` — 移除 fallback 默认值
  - `claude-j-infrastructure/src/main/java/com/claudej/infrastructure/auth/config/JwtSecretValidator.java` — 新增启动校验器
  - `claude-j-infrastructure/src/test/resources/application.yml` — 添加测试用 jwt.secret

- **ci**:
  - `.github/workflows/ci.yml` — 注入 JWT_SECRET 环境变量

- **docs**:
  - `docs/ops/secrets.md` — 新增运维文档

## 验收标准

1. 未设置 JWT_SECRET 环境变量时，应用启动失败并提示明确错误
2. 设置 JWT_SECRET（长度>=32）后，应用正常启动，登录功能正常
3. JWT_SECRET 长度 <32 时，应用启动失败并提示密钥长度不足
4. 开发环境使用 application-dev.yml 可正常启动（使用默认值）
5. CI 测试通过（环境变量已注入）
6. 三项预飞检查通过: mvn test / checkstyle / entropy-check
