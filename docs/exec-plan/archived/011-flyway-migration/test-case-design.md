# 测试用例设计 — 011-flyway-migration

## 测试范围

引入 Flyway 数据库迁移框架的 QA 验收测试，覆盖 migration 文件完整性、配置正确性、功能验证和代码审查。


## 测试策略

按测试金字塔分层测试 + 集成测试 + 代码审查 + 风格检查。

---

## 0. AC 自动化覆盖矩阵（强制，动笔前先填）

> **背景**：010-secret-externalize 的 AC1「未设 JWT_SECRET 启动失败」被标为"手动验证"，关键安全保证失去回归保障。从 011 起每条 AC 必须在此表留下可回溯的自动化证据。
>
> **规则**：
> 1. 列出 `requirement-design.md#验收标准` 的每一条 AC
> 2. 每条必须映射到至少 1 个自动化测试（哪一层、哪个测试方法名）
> 3. 若标「手动」，必须说明**为什么不能自动化** + **替代自动化测试**（即便是弱化版）
> 4. @architect 评审时会校验本表是否填写完整，不全则 changes-requested

| # | 验收条件（AC） | 自动化层 | 对应测试方法 | 手动备注（若有） |
|---|---|---|---|---|
| AC1 | 空库启动自动建表 | Start（集成） | `FlywayVerificationTest.should_create_11_tables_when_migrations_complete` | - |
| AC2 | flyway_schema_history 表可见 7 条成功记录 | Start（集成） | `FlywayVerificationTest.should_record_7_migrations_when_flyway_migrates` | - |
| AC3 | 修改历史 migration 后 validate 失败 | Start（集成） | `FlywayVerificationTest.should_fail_when_migration_checksum_mismatch` | 需动态修改文件，用 @DynamicPropertySource 模拟 |
| AC4 | 三项预飞检查通过 | CI | `mvn test`, `mvn checkstyle:check`, `./scripts/entropy-check.sh` | - |

**反模式（禁止）**：
- ❌ "启动失败无法自动化" → 错。用 `ApplicationContextRunner` / `@SpringBootTest(properties=...)` 均可模拟启动失败
- ❌ "性能场景只能手测" → 错。用 JMH / `@Timeout` 可自动化门限
- ❌ "CI 环境才触发" → 错。把 CI 的环境变量注入等价物放到 `@TestPropertySource` 即可

---

## 一、Domain 层测试场景

本任务不涉及领域模型变更，无需新增 Domain 层测试。

---

## 二、Application 层测试场景

本任务不涉及应用服务变更，无需新增 Application 层测试。

---

## 三、Infrastructure 层测试场景

本任务不涉及 Repository 实现变更，无需新增 Infrastructure 层测试。

---

## 四、Adapter 层测试场景

本任务不涉及 REST API 变更，无需新增 Adapter 层测试。

---

## 五、集成测试场景（全链路）

<!-- @SpringBootTest + H2，在 start 模块 -->

### 现有测试验证

| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E1 | 短链接服务 | 运行 ShortLinkIntegrationTest | 10 个测试全部通过 |
| E2 | 购物车服务 | 运行 CartIntegrationTest | 9 个测试全部通过 |
| E3 | 既有 ArchUnit | 运行 ArchitectureTest | 14 条规则全部通过 |
| E4 | Flyway migration 历史 | FlywayVerificationTest | 7 条记录，全部成功 |
| E5 | 表创建完整性 | FlywayVerificationTest | 11 张表全部创建 |

### 建议补充测试

| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E6 | validate 机制验证 | 修改 V1 文件后启动应用 | 应用启动失败，抛出 checksum mismatch 异常 |

---

## 六、代码审查检查项

### Migration 文件审查

- [x] V1__user_init.sql 包含 t_user 表，DDL 完整
- [x] V2__order_init.sql 包含 t_order, t_order_item 表
- [x] V3__shortlink_init.sql 包含 t_short_link 表
- [x] V4__link_init.sql 包含 t_link 表
- [x] V5__coupon_init.sql 包含 t_coupon 表
- [x] V6__cart_init.sql 包含 t_cart, t_cart_item 表
- [x] V7__auth_init.sql 包含 t_auth_user, t_user_session, t_login_log 表
- [x] 共 11 张表，与 schema.sql 原内容一致
- [x] 文件名格式 V{version}__{description}.sql 符合规范
- [x] 使用 CREATE TABLE IF NOT EXISTS 语法
- [x] 表名 t_{entity}，列名 snake_case

### 配置审查

- [x] application-dev.yml 启用 flyway: enabled: true
- [x] locations: classpath:db/migration 配置正确
- [x] baseline-on-migrate: true 适合新库场景
- [x] validate-on-migrate: true 启用校验
- [x] 移除 spring.sql.init 配置，避免重复执行
- [x] H2 console 保留便于调试

### POM 依赖审查

- [x] 父 pom 定义 flyway.version=8.5.13（Spring Boot 2.7 兼容）
- [x] 父 pom 定义 flyway-core dependencyManagement
- [x] claude-j-start pom 引入 flyway-core
- [x] 版本兼容性已验证

### 文档审查

- [x] docs/ops/db-migrations.md 存在
- [x] 包含命名规范说明
- [x] 包含新增聚合模板
- [x] 包含 rollback 策略
- [x] 包含验证命令
- [x] 包含注意事项（不修改已执行 migration）

### CI 配置审查

- [x] flyway-validate job 存在
- [x] 使用标准 actions（checkout@v4, setup-java@v4）
- [x] 执行 mvn flyway:validate -pl claude-j-start

---

## 七、代码风格检查项

- [x] Java 8 兼容（无 var、records、text blocks、List.of）
- [x] Migration 文件为 SQL，无 Java 代码
- [x] 配置 YAML 格式正确
- [x] 测试命名 should_xxx_when_xxx（FlywayVerificationTest 符合）
- [x] Checkstyle 0 violations

---

## 八、验证命令清单

```bash
# 1. 全量测试
mvn clean test

# 2. 代码风格检查
mvn checkstyle:check

# 3. 架构漂移检测
./scripts/entropy-check.sh

# 4. Flyway 验证
mvn flyway:validate -pl claude-j-start

# 5. H2 内存库启动验证
mvn spring-boot:run -pl claude-j-start -Dspring-boot.run.profiles=dev
```

---

## 九、问题严重度分级

| 级别 | 描述 | 示例 |
|------|------|------|
| **Critical** | 架构违规、数据损坏风险、安全问题 | 依赖方向错误、DO 泄漏到外层 |
| **Major** | 逻辑错误、缺失校验、行为不正确 | migration 文件缺失、DDL 不完整 |
| **Minor** | 风格问题、命名不一致、缺失注释 | 文件名不规范、缺少注释 |

