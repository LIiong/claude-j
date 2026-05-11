# 需求拆分设计 — 029-testcontainers

## 需求描述
- 将 `claude-j-infrastructure` 的仓储集成测试从 H2 切换到共享 MySQL 8 Testcontainers 引导，以提升持久化测试保真度。
- 将 `claude-j-start` 的 `FlywayVerificationTest` 改为验证真实 MySQL 迁移状态；不改变生产行为，不扩展无关 start 业务集成测试。
- 若运行环境缺少 Docker / Testcontainers，可记录阻塞并保留真实验证结论，禁止伪造通过。

## 领域分析

### 聚合根: Persistence Test Bootstrap
- 本任务不新增或修改业务聚合根。
- 影响的是仓储测试装配方式：订单、支付、通知、购物车、优惠券、库存、链接、商品、短链、用户仓储测试。

### 值对象
- 无新增值对象。

### 领域服务（如有）
- 无。

### 端口接口
- 无生产端口签名变化。
- 受影响的是 infrastructure 层仓储实现测试对真实 MySQL/Flyway 的验证路径。

## 关键算法/技术方案
- 方案 A：每个测试类各自声明 `MySQLContainer`。优点是局部清晰；缺点是启动成本高、重复配置多。
- 方案 B：每个模块抽一个共享测试引导基类，使用 `@Testcontainers` + `@DynamicPropertySource` 注入容器连接信息。优点是改动面小、可逐类迁移、便于保留现有窄测试切片。
- 采用方案 B。
- `claude-j-infrastructure` 测试资源配置保留 MyBatis/Flyway 共性配置，清空 H2 专属 datasource，由容器动态注入。
- `FlywayVerificationTest` 改用 MySQL 元数据查询：`DATABASE()` + `information_schema.tables`，避免 H2 `PUBLIC` 与双引号标识符假设。

## API 设计
- 无 API 变更。

## 数据库设计（如有）
- 无生产 DDL 变更。
- 测试数据库改为 MySQL 8 Testcontainers，仍消费现有 `classpath:db/migration`。

## 影响范围
- **domain**: 无
- **application**: 无
- **infrastructure**:
  - `claude-j-infrastructure/pom.xml`
  - `claude-j-infrastructure/src/test/resources/application.yml`
  - `claude-j-infrastructure/src/test/java/com/claudej/infrastructure/test/MySqlRepositoryIntegrationTestSupport.java`
  - 各 `*RepositoryImplTest`
- **adapter**: 无
- **start**:
  - `claude-j-start/pom.xml`
  - `claude-j-start/src/test/java/com/claudej/start/flyway/MySqlFlywayIntegrationTestSupport.java`
  - `claude-j-start/src/test/java/com/claudej/start/flyway/FlywayVerificationTest.java`

## 假设与待确认
- 假设本地或 CI 具备 Docker，可运行 Testcontainers；若不可用，本任务只记录阻塞与失败证据。
- 假设现有 `db/migration` SQL 可直接在 MySQL 8 上运行，无需新增迁移脚本。
- 假设除 `FlywayVerificationTest` 外，start 模块其他测试继续使用现有 H2 测试配置，不做扩散。
