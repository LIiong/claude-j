# 开发日志 — 029-testcontainers

## 问题记录

### 1. Maven 代理阻塞 Testcontainers 依赖下载
- **Issue**：首次执行 `mvn test -pl claude-j-infrastructure -Dtest=OrderRepositoryImplTest` 时，Testcontainers 依赖无法下载，之前会话报错为 `Connect to 127.0.0.1:7897 failed: Connection refused`。
- **Root Cause**：默认 Maven 配置引用了失效的本地代理，导致 `org.testcontainers:junit-jupiter` 与 `org.testcontainers:mysql` 无法解析。
- **Fix**：新增会话级配置文件 `/Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml`，仅保留 `https://repo.maven.apache.org/maven2` 镜像，并用 `-s` 显式传入所有验证命令；未改动 `/Users/macro.li/.m2/settings.xml`。
- **Verification**：`mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml checkstyle:check` → `BUILD SUCCESS`

### 2. 仓储测试迁移后缺少共享引导与手工 DDL 残留
- **Issue**：仓储测试改为继承共享 MySQL 基类后，部分类缺少 `MySqlRepositoryIntegrationTestSupport` import，payment / notification 仍残留手工建表逻辑。
- **Root Cause**：批量替换只改了 `extends`，未同步补齐 import；028 中的 payment / notification 测试为绕过 H2/Flyway 差异保留了局部 `CommandLineRunner` DDL。
- **Fix**：为全部迁移后的 `*RepositoryImplTest` 补齐 `import com.claudej.infrastructure.test.MySqlRepositoryIntegrationTestSupport;`；删除 `PaymentRepositoryImplTest` 与 `NotificationRepositoryImplTest` 中的手工建表 `CommandLineRunner`，改由容器 datasource + 现有 Flyway 迁移建表。
- **Verification**：`mvn -q -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml -pl claude-j-infrastructure -am test-compile` → `Exit 0`

### 3. Start 模块出现重复 MySQL 测试依赖告警
- **Issue**：`claude-j-start/pom.xml` 存在运行时 MySQL 依赖之外的重复 test-scope MySQL 依赖，Maven 会给出重复声明告警。
- **Root Cause**：Testcontainers 迁移时额外插入了一个 `mysql:mysql-connector-java` test 依赖，但 start 模块本来已有 runtime 作用域 MySQL 驱动。
- **Fix**：删除 `claude-j-start/pom.xml` 中重复的 test-scope MySQL 驱动，保留原 runtime 依赖与新增 Testcontainers 依赖。
- **Verification**：`mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml checkstyle:check` → `BUILD SUCCESS`

### 4. Flyway 8 与 MySQL 8 Testcontainers 不兼容
- **Issue**：Docker 接通后，`OrderRepositoryImplTest` 在 Spring 上下文初始化阶段失败，报错 `org.flywaydb.core.api.FlywayException: Unsupported Database: MySQL 8.0`。
- **Root Cause**：项目父 POM 锁定 `flyway.version=8.5.13`，该版本无法识别当前 Testcontainers 提供的 MySQL 8 元数据；同时仅加 `flyway-core` 不会自动带入新版 MySQL 数据库插件。
- **Fix**：将父 POM 的 `flyway.version` 升到 `9.22.3`，并为 `claude-j-infrastructure` / `claude-j-start` 增加 test-scope `org.flywaydb:flyway-mysql`；为避免新版驱动元数据再触发兼容分支，MySQL 驱动回退到 `8.0.31`，容器镜像固定为 `mysql:8.0.32`。
- **Verification**：`DOCKER_HOST=unix:///Users/macro.li/.docker/run/docker.sock mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml -Dapi.version=1.41 test -pl claude-j-infrastructure -Dtest=OrderRepositoryImplTest` → `Tests run: 9, Failures: 0, Errors: 0`，`BUILD SUCCESS`

### 5. 现有测试迁移脚本仍含 H2 兼容 SQL，MySQL 下执行失败
- **Issue**：切到真实 MySQL 后，`V11__add_payment.sql` 在 Flyway 迁移阶段失败，错误为 `CREATE INDEX IF NOT EXISTS ...` 语法不被当前 MySQL 接受；notification 仓储测试随后又暴露 `t_notification` 表不存在。
- **Root Cause**：`claude-j-infrastructure/src/test/resources/db/migration/` 保留的是 H2 兼容版脚本，不包含 start 模块里的通知迁移，且 payment/inventory 仍使用 `CREATE INDEX IF NOT EXISTS` 这种 H2 风格语法。
- **Fix**：将 infrastructure test resources 中的 `V10__add_inventory.sql`、`V11__add_payment.sql` 改成 MySQL 兼容索引定义；新增 `V12__add_notification.sql` 以便通知仓储测试能在共享 Flyway 路径下建表；同步更新 start 模块中的 `V10__add_inventory.sql` 与 `V11__add_payment.sql`，确保 `FlywayVerificationTest` 验证的真实迁移同样兼容 MySQL。
- **Verification**：`DOCKER_HOST=unix:///Users/macro.li/.docker/run/docker.sock mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml -Dapi.version=1.41 test -pl claude-j-infrastructure -Dtest=PaymentRepositoryImplTest,NotificationRepositoryImplTest` → `Tests run: 11, Failures: 0, Errors: 0`，`BUILD SUCCESS`

### 6. Flyway 验证测试需要隔离 start 模块业务装配
- **Issue**：`FlywayVerificationTest` 初次切到真实 MySQL 后，测试并非因迁移失败，而是因完整 `ClaudeJApplication` 装配 `NotificationApplicationService` 时缺少仓储 Bean 而启动失败。
- **Root Cause**：该测试只需要 DataSource + Flyway + JdbcTemplate，但默认 `@SpringBootTest` 会加载整个 start 模块应用上下文，把无关业务 Bean 缺失也拉进来。
- **Fix**：将 `FlywayVerificationTest` 改为 `@SpringBootTest(classes = FlywayVerificationTest.TestConfig.class)`，并在测试内部提供最小化 `@SpringBootConfiguration` + `@EnableAutoConfiguration`，仅保留 Flyway 验证所需自动配置。
- **Verification**：`DOCKER_HOST=unix:///Users/macro.li/.docker/run/docker.sock mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml -Dapi.version=1.41 test -pl claude-j-start -Dtest=FlywayVerificationTest` → `Tests run: 2, Failures: 0, Errors: 0`，`BUILD SUCCESS`

### 7. 全量预飞未收敛到全绿
- **Issue**：本轮定向验证全部通过，但全量 `mvn test` 仍失败。
- **Root Cause**：将真实 MySQL 兼容 Flyway 版本提升到 `9.22.3` 后，start 模块其余既有 H2 业务集成测试暴露出 31 个历史/环境相关错误；这超出了本任务“infra repository tests + FlywayVerificationTest only”的范围。
- **Fix**：未扩散修改无关 start 业务测试，只记录真实失败证据；保留本任务窄范围实现和通过的定向验证结果。
- **Verification**：`DOCKER_HOST=unix:///Users/macro.li/.docker/run/docker.sock mvn -s /Users/macro.li/aiProject/claude-j/.tmp-maven-settings.xml -Dapi.version=1.41 test` → `Tests run: 69, Failures: 0, Errors: 31`

## 变更记录
- 原设计假设“无生产 DDL 变更”，实现阶段发现 start 模块现有迁移脚本本身包含 H2 风格 SQL，若不改则无法在真实 MySQL 上完成 Flyway 验证；因此仅对 `V10__add_inventory.sql`、`V11__add_payment.sql` 做了 MySQL 兼容性收敛，未扩展业务表结构。
