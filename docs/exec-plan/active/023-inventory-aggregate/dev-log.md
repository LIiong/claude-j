# 开发日志 — 023-inventory-aggregate

## 问题记录

<!-- Build 阶段填写，每条必须四段齐全 -->

### Build 阶段遗留问题

#### 问题 1：FlywayVerificationTest migration 数量不匹配

**现象**：
- `should_record_10_migrations_when_flyway_migrates` 失败
- 预期 10 个 migrations，实际只有 9 个被 Flyway 执行

**复现命令**：
```bash
mvn test -pl claude-j-start -Dtest=FlywayVerificationTest
```

**根因分析**（初步）：
- Spring Test context 缓存问题：其他测试先运行创建 context 时只有 9 个 migrations
- V10 文件在 jar 和 target/classes 中都存在，命名格式正确
- Flyway 日志在 Spring Boot Test 中被过滤，无法看到执行过程

**待处理**：
- 需要深入调查 Spring Test context 缓存与 Flyway 的交互
- 可能需要在 FlywayVerificationTest 上添加特殊配置强制重建 context

#### 问题 2：OrderFromCartIntegrationTest 404 状态码

**现象**：
- 3 个测试返回 404 状态码而不是 200

**复现命令**：
```bash
mvn test -pl claude-j-start -Dtest=OrderFromCartIntegrationTest
```

**根因分析**（初步）：
- 可能与 Order 集成 Inventory 后的 API 路径变化有关
- 需要检查 OrderController 的路由配置

**待处理**：
- 需要检查 Order 从购物车创建的端点是否受 Inventory 集成影响

## 变更记录

<!-- 与原设计（requirement-design.md）不一致的变更 -->

### Build 阶段变更

1. **V10 DDL 修改**：从 MySQL 特定语法改为 H2 兼容语法
   - 原设计：`ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`
   - 实际：移除 MySQL 特定语法，使用标准 SQL
   - 原因：确保 H2 测试环境兼容