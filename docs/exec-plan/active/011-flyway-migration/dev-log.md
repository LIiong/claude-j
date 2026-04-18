# 开发日志 — 011-flyway-migration

## 问题记录

### 1. Flyway 依赖与测试环境配置

**问题**: 基础设施层测试模块（claude-j-infrastructure）找不到表，因为缺少 Flyway 依赖。

**决策**: 在 claude-j-infrastructure/pom.xml 中添加 flyway-core 的 test scope 依赖。

**原因**: 基础设施测试使用独立的数据源配置（application.yml），需要 Flyway 在测试启动时执行 migration。添加 test scope 依赖可避免影响生产代码。

### 2. 测试资源中的重复 migration 文件

**问题**: 最初在 claude-j-start/src/test/resources/db/migration/ 复制了 migration 文件，导致测试时出现 "duplicate migration" 错误。

**决策**: 删除 test resources 中的 migration 目录，仅保留 main resources 中的 migration 文件。

**原因**: Spring Boot 测试会自动加载 main resources，不需要在 test resources 中重复存放 migration 文件。

### 3. H2 数据库大小写敏感问题

**问题**: FlywayVerificationTest 中查询 flyway_schema_history 表时出现 "Table 'FLYWAY_SCHEMA_HISTORY' not found" 错误。

**决策**: 使用双引号包裹标识符，如 `"flyway_schema_history"`。

**原因**: H2 数据库默认将未加引号的标识符转换为大写，而 Flyway 创建的表名是小写的。使用双引号可以保持原始大小写。

### 4. ArchUnit 测试命名规范

**问题**: FlywayVerificationTest 的测试方法名不符合 `should_xxx_when_yyy` 命名规范，导致 ArchUnit 检查失败。

**决策**: 将方法名从 `should_have_7_migrations_in_schema_history` 改为 `should_record_7_migrations_when_flyway_migrates`。

**原因**: 遵守项目定义的测试命名规范，保持与现有测试代码的一致性。

## 变更记录

- 无与原设计不一致的变更。
