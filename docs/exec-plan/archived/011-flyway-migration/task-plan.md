# 任务执行计划 — 011-flyway-migration

## 任务状态跟踪

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | POM: 父 pom 添加 flyway-core 依赖 | dev | **完成** | Spring Boot 2.7 兼容版本 |
| 2 | Migration: 创建 V1__user_init.sql | dev | **完成** | 拆分自 schema.sql |
| 3 | Migration: 创建 V2__order_init.sql | dev | **完成** | 拆分自 schema.sql |
| 4 | Migration: 创建 V3__shortlink_init.sql | dev | **完成** | 拆分自 schema.sql |
| 5 | Migration: 创建 V4__link_init.sql | dev | **完成** | 拆分自 schema.sql |
| 6 | Migration: 创建 V5__coupon_init.sql | dev | **完成** | 拆分自 schema.sql |
| 7 | Migration: 创建 V6__cart_init.sql | dev | **完成** | 拆分自 schema.sql |
| 8 | Migration: 创建 V7__auth_init.sql | dev | **完成** | 拆分自 schema.sql |
| 9 | Config: 修改 application-dev.yml 启用 flyway | dev | **完成** | 移除 spring.sql.init |
| 10 | Delete: 删除旧的 schema.sql | dev | **完成** | 确认迁移成功后删除 |
| 11 | Docs: 新增 db-migrations.md 规范文档 | dev | **完成** | 含命名规范与模板 |
| 12 | CI: 新增 flyway:validate job | dev | **完成** | GitHub Actions |
| 13 | Verify: 启动验证（空库自动建表） | dev | **完成** | H2 内存模式 |
| 14 | Verify: flyway_schema_history 检查 | dev | **完成** | 7 条记录验证 |
| 15 | Verify: 修改历史 migration 验证失败 | dev | **完成** | validate-on-migrate |
| 16 | Preflight: mvn test 全量通过 | dev | **完成** | 含 ArchUnit，52/52 通过 |
| 17 | Preflight: checkstyle 通过 | dev | **完成** | 0 violations |
| 18 | Preflight: entropy-check 通过 | dev | **完成** | 0 FAIL, 11 WARN(既有) |
| 19 | QA: 验收测试与代码审查 | qa | **完成** | 52/52 测试通过，0 Critical/Major 问题 |

## 执行顺序

```
POM修改 → Migration拆分(V1-V7) → 配置修改 → 删除旧schema →
文档编写 → CI配置 → 启动验证 → flyway历史表验证 →
validate失败验证 → 三项预飞 → QA验收
```

## 原子任务分解

### 1.1 POM 添加 flyway-core 依赖
- **文件**: `pom.xml`
- **骨架**:
  ```xml
  <properties>
      <flyway.version>8.5.13</flyway.version>
  </properties>

  <dependencyManagement>
      <dependencies>
          <dependency>
              <groupId>org.flywaydb</groupId>
              <artifactId>flyway-core</artifactId>
              <version>${flyway.version}</version>
          </dependency>
      </dependencies>
  </dependencyManagement>

  <!-- claude-j-start/pom.xml -->
  <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-core</artifactId>
  </dependency>
  ```
- **验证命令**: `mvn dependency:tree -pl claude-j-start | grep flyway`
- **预期输出**: `org.flywaydb:flyway-core:jar:8.5.13`
- **commit**: `chore(deps): add flyway-core 8.5.13 for Spring Boot 2.7`

### 1.2 Migration V1__user_init.sql
- **文件**: `claude-j-start/src/main/resources/db/migration/V1__user_init.sql`
- **内容**: t_user DDL
- **验证命令**: `ls claude-j-start/src/main/resources/db/migration/`
- **预期输出**: `V1__user_init.sql`
- **commit**: `chore(db): add V1 migration for user aggregate`

### 1.3 Migration V2__order_init.sql
- **文件**: `claude-j-start/src/main/resources/db/migration/V2__order_init.sql`
- **内容**: t_order, t_order_item DDL
- **commit**: `chore(db): add V2 migration for order aggregate`

### 1.4 Migration V3__shortlink_init.sql
- **文件**: `claude-j-start/src/main/resources/db/migration/V3__shortlink_init.sql`
- **commit**: `chore(db): add V3 migration for shortlink aggregate`

### 1.5 Migration V4__link_init.sql
- **文件**: `claude-j-start/src/main/resources/db/migration/V4__link_init.sql`
- **commit**: `chore(db): add V4 migration for link aggregate`

### 1.6 Migration V5__coupon_init.sql
- **文件**: `claude-j-start/src/main/resources/db/migration/V5__coupon_init.sql`
- **commit**: `chore(db): add V5 migration for coupon aggregate`

### 1.7 Migration V6__cart_init.sql
- **文件**: `claude-j-start/src/main/resources/db/migration/V6__cart_init.sql`
- **commit**: `chore(db): add V6 migration for cart aggregate`

### 1.8 Migration V7__auth_init.sql
- **文件**: `claude-j-start/src/main/resources/db/migration/V7__auth_init.sql`
- **commit**: `chore(db): add V7 migration for auth aggregate`

### 2.1 修改 application-dev.yml
- **文件**: `claude-j-start/src/main/resources/application-dev.yml`
- **骨架**:
  ```yaml
  spring:
    datasource:
      driver-class-name: org.h2.Driver
      url: jdbc:h2:mem:claude_j;DB_CLOSE_DELAY=-1;MODE=MySQL
      username: sa
      password:
    h2:
      console:
        enabled: true
        path: /h2-console
    flyway:
      enabled: true
      locations: classpath:db/migration
      baseline-on-migrate: true
      validate-on-migrate: true

  jwt:
    secret: dev-jwt-secret-key-at-least-32-bytes-for-local
  ```
- **验证命令**: `mvn test -pl claude-j-start`
- **预期输出**: `Tests run: X, Failures: 0, Errors: 0`
- **commit**: `chore(config): enable flyway in dev profile`

### 2.2 删除旧的 schema.sql
- **文件**: `claude-j-start/src/main/resources/db/schema.sql`
- **验证命令**: `ls claude-j-start/src/main/resources/db/`
- **预期输出**: 只有 `migration/` 目录，无 `schema.sql`
- **commit**: `chore(db): remove legacy schema.sql`

### 3.1 编写 db-migrations.md 文档
- **文件**: `docs/ops/db-migrations.md`
- **内容**: 命名规范、新增聚合模板、rollback 策略
- **验证命令**: `ls docs/ops/`
- **commit**: `docs(ops): add db migration guidelines`

### 4.1 CI 新增 flyway:validate job
- **文件**: `.github/workflows/ci.yml`（或创建新 workflow）
- **骨架**:
  ```yaml
  flyway-validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 8
        uses: actions/setup-java@v4
        with:
          java-version: '8'
          distribution: 'temurin'
      - name: Validate Flyway Migrations
        run: mvn flyway:validate -pl claude-j-start
  ```
- **commit**: `ci: add flyway validate job`

### 5.1 启动验证 - 空库自动建表
- **验证命令**: `mvn spring-boot:run -pl claude-j-start -Dspring-boot.run.profiles=dev`
- **预期输出**: 应用正常启动，控制台可见 Flyway migration 日志
- **手动验证**: 访问 /h2-console，确认所有表存在
- **commit**: `N/A - 验证步骤`

### 5.2 flyway_schema_history 验证
- **验证命令**: H2 console 执行 `SELECT * FROM flyway_schema_history`
- **预期输出**: 7 条记录，version 1-7，success=1
- **commit**: `N/A - 验证步骤`

### 5.3 validate 失败验证
- **验证步骤**:
  1. 修改 V1__user_init.sql（如加空格）
  2. 重启应用
- **预期输出**: 启动失败，报错 checksum mismatch
- **commit**: `N/A - 验证步骤`

### 6.1 三项预飞检查
- **验证命令1**: `mvn test`
- **验证命令2**: `mvn checkstyle:check`
- **验证命令3**: `./scripts/entropy-check.sh`
- **commit**: `N/A - 验证步骤`

## 风险与假设

### 风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Flyway 与 Spring Boot 2.7 版本兼容性 | 应用启动失败 | 选用 flyway-core 8.5.13，已在 Spring Boot 2.7 验证兼容 |
| H2 内存模式与 MySQL MODE 差异 | DDL 语法不兼容 | 保留 MODE=MySQL，验证所有 DDL 在 H2 可执行 |
| 现有集成测试依赖 schema.sql | 测试失败 | 确认 Flyway 在测试环境同样生效 |

### 假设

1. Spring Boot 2.7 自动配置 Flyway，无需额外 Java 配置类
2. H2 内存模式支持所有 MySQL DDL 语法（已由现有 schema.sql 验证）
3. flyway_schema_history 表由 Flyway 自动创建
4. 开发环境使用 H2，生产环境使用 MySQL，Flyway 迁移脚本两者兼容

## 验证策略

### 功能验证

| 验证项 | 方法 | 预期结果 |
|--------|------|----------|
| 空库启动 | 删除 H2 内存库重启 | 所有表自动创建 |
| migration 历史 | 查询 flyway_schema_history | 7 条成功记录 |
| validate 机制 | 修改 V1 文件后启动 | 启动失败并提示 checksum mismatch |
| 应用功能 | 运行既有集成测试 | 全部通过 |

### 回归验证

- 所有既有测试用例通过
- ArchUnit 架构规则通过
- Checkstyle 代码风格通过
- Entropy-check 架构漂移检测通过

## 开发完成记录

- 全量 `mvn clean test`: **52/52 用例通过**
- flyway_schema_history 记录数: **7**
- validate 机制验证: **通过**
- 通知 @qa 时间: **2026-04-18**

## QA 验收记录

- 全量测试（含集成测试）: **52/52 用例通过**
- 代码审查结果: **通过**，0 Critical/Major 问题，1 Minor 建议
- 代码风格检查: **0 violations**
- 问题清单: 详见 test-report.md
- **最终状态**: **验收通过**
