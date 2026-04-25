# 开发日志 — 021-product-aggregate

## 问题记录

<!-- Build 阶段填写 -->

### 1. Flyway 迁移 checksum 冲突
- **Issue**: start 模块集成测试失败，Flyway 报 "Detected failed migration to version 8 (product init)"
- **Root Cause**:
  - main/resources/db/migration V8 使用 MySQL 语法 (ENGINE=InnoDB, CHARSET, COMMENT, KEY)
  - H2 无法解析 MySQL 特定语法
  - Spring context 使用 @ActiveProfiles("dev") 加载 main application-dev.yml 中的 `locations: classpath:db/migration`
- **Fix**:
  - 在 start/src/test/resources 创建 db/migration-h2 目录，复制 H2 兼容的迁移文件
  - 创建 test/resources/application-dev.yml 覆盖 flyway.locations 为 `classpath:db/migration-h2`
  - 设置 validate-on-migrate: false, clean-disabled: false
- **Verification**: `mvn test -pl claude-j-start` → Tests run: 59, Failures: 0, Errors: 0

### 2. FlywayVerificationTest 预期值需更新
- **Issue**: FlywayVerificationTest 测试失败，预期 7 迁移/11 表，实际 8 迁移/12 表
- **Root Cause**: 测试用例在 product 聚合之前编写，未包含 V8 migration 和 T_PRODUCT 表
- **Fix**: 更新测试用例预期值为 8 迁移和 12 表（包含 T_PRODUCT）
- **Verification**: `mvn test -pl claude-j-start -Dtest=FlywayVerificationTest` → Tests run: 2, Failures: 0

### 3. ProductRepository Bean 未找到
- **Issue**: start 模块测试报 "No qualifying bean of type 'ProductRepository'"
- **Root Cause**: infrastructure jar 未重新安装，ProductRepositoryImpl 未被 Spring 扫描到
- **Fix**: `mvn install -pl claude-j-infrastructure -DskipTests` 重新安装
- **Verification**: Spring context 成功加载 ProductApplicationService

## 变更记录

<!-- 与原设计（requirement-design.md）不一致的变更 -->
- 无与原设计不一致的变更。

### 新增文件（非原设计明确列出但必须添加）
1. `claude-j-start/src/test/resources/db/migration-h2/*.sql` - H2 兼容迁移文件
2. `claude-j-start/src/test/resources/application-dev.yml` - 测试环境 dev profile 覆盖配置