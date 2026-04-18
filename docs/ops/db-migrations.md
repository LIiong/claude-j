# Database Migration Guidelines

本文档说明 claude-j 项目的数据库迁移规范与操作流程。

## Flyway 配置

本项目使用 Flyway 进行数据库版本管理，版本与 Spring Boot 2.7 内置版本一致。

### 版本策略

- **版本格式**: `V{version}__{description}.sql`
- **版本号**: 线性整数版本（V1, V2, V3...）
- **描述**: 下划线分隔的简短描述

### 配置项

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true    # 新库自动基线化
    validate-on-migrate: true    # 校验 migration 完整性
```

## 命名规范

### 新增聚合模板

新增聚合应在现有最高版本基础上递增：

```
V8__inventory_init.sql
V9__payment_init.sql
```

### 文件命名规则

| 组件 | 规则 | 示例 |
|------|------|------|
| 版本号 | 大写 V + 整数 | `V1`, `V2` |
| 分隔符 | 双下划线 `__` | `V1__` |
| 描述 | 小写，下划线分隔 | `user_init` |
| 后缀 | `.sql` | `.sql` |

## 聚合映射

当前 7 个聚合的 migration 文件：

| 版本 | 聚合 | 表 |
|------|------|-----|
| V1 | user | t_user |
| V2 | order | t_order, t_order_item |
| V3 | shortlink | t_short_link |
| V4 | link | t_link |
| V5 | coupon | t_coupon |
| V6 | cart | t_cart, t_cart_item |
| V7 | auth | t_auth_user, t_user_session, t_login_log |

## Rollback 策略

Flyway 社区版不支持自动回滚。如需回滚：

1. **开发环境**: 删除 H2 内存库重新启动
2. **生产环境**: 手动编写反向 migration（V8__rollback_x.sql）

## 开发流程

### 新增表步骤

1. 创建 migration 文件 `V{N}__{description}.sql`
2. 使用 `CREATE TABLE IF NOT EXISTS` 语法
3. 表名使用 `t_{entity}` 格式
4. 列名使用 snake_case
5. 添加 COMMENT 说明
6. 本地验证：`mvn spring-boot:run -Dspring-boot.run.profiles=dev`
7. 提交代码

### 修改表步骤

1. 新建 migration 文件，不要修改已执行的 migration
2. 使用 `ALTER TABLE` 语法
3. 考虑既有数据兼容性

## 验证命令

```bash
# 查看 migration 历史
mvn flyway:info -pl claude-j-start

# 验证 migration 文件
mvn flyway:validate -pl claude-j-start

# 手动执行 migration
mvn flyway:migrate -pl claude-j-start
```

## H2 与 MySQL 兼容

- 使用 `MODE=MySQL` 启动 H2
- 避免使用 MySQL 特有语法（如 `ON UPDATE CURRENT_TIMESTAMP` 在 H2 部分版本不支持）
- 索引使用 `KEY` 或 `INDEX` 语法
- 字符集使用 `utf8mb4`

## 注意事项

1. **永远不要修改已执行的 migration 文件** - 会导致 checksum 不匹配
2. **测试环境也需要 Flyway** - 确保 `@SpringBootTest` 能正常工作
3. **CI 集成** - 每次构建自动执行 `flyway:validate`
4. **生产部署** - 确保数据库备份后再执行新 migration

