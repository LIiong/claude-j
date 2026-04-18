# Spring Profile 配置矩阵

本文档描述 claude-j 在多环境（dev/staging/prod）下的配置差异。

## 快速选择

```bash
# 开发环境（本地）
mvn spring-boot:run -pl claude-j-start -Dspring-boot.run.profiles=dev

# 预发布环境（需设置环境变量）
export MYSQL_HOST=... JWT_SECRET=...
mvn spring-boot:run -pl claude-j-start -Dspring-boot.run.profiles=staging

# 生产环境（Docker）
docker run -e SPRING_PROFILES_ACTIVE=prod -e MYSQL_HOST=... claude-j
```

## 五维配置矩阵

| 维度 | dev | staging | prod |
|-----|-----|---------|------|
| **配置文件** | `application-dev.yml` | `application-staging.yml` | `application-prod.yml` |
| **数据源** | H2 (内存) | MySQL | MySQL |
| **JWT Secret** | hardcoded (dev-only) | `${JWT_SECRET}` (required) | `${JWT_SECRET}` (required) |
| **日志级别** | DEBUG | INFO | WARN |
| **Actuator** | health,info,metrics (全开放) | health,info (只读) | health (最小暴露) |
| **CORS** | `*` (全允许) | `${CORS_ALLOWED_ORIGINS}` | `${CORS_ALLOWED_ORIGINS}` (required) |

## 环境变量清单

### 必配变量（staging/prod）

| 变量名 | 示例值 | 说明 |
|--------|--------|------|
| `MYSQL_HOST` | `mysql.example.com` | MySQL 主机 |
| `MYSQL_PORT` | `3306` | MySQL 端口（默认3306） |
| `MYSQL_DB` | `claude_j` | 数据库名 |
| `MYSQL_USER` | `app_user` | 数据库用户 |
| `MYSQL_PASSWORD` | `***` | 数据库密码 |
| `JWT_SECRET` | `min-32-chars-long-secret-key-here` | JWT 签名密钥（≥32字符） |

### 选配变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `CORS_ALLOWED_ORIGINS` | staging: 空字符串<br>prod: 必填 | 允许跨域的域名列表（逗号分隔） |

## 环境启动失败排查

### staging/prod 启动报错 `Could not resolve placeholder`

**现象**：
```
IllegalArgumentException: Could not resolve placeholder 'MYSQL_HOST' in value "${MYSQL_HOST}"
```

**原因**：环境变量未设置，Spring 启动失败（Fail-Fast）。

**解决**：
```bash
# 检查缺失变量
env | grep MYSQL
env | grep JWT_SECRET

# 设置后重启
export MYSQL_HOST=your-host
export MYSQL_PORT=3306
export MYSQL_DB=claude_j
export MYSQL_USER=your-user
export MYSQL_PASSWORD=your-password
export JWT_SECRET=your-32-char-secret
```

## 各环境详细配置

### dev 环境

**用途**：本地开发、单元测试

**特点**：
- H2 内存数据库，无需外部依赖
- H2 Console 启用 (`/h2-console`)
- Flyway 自动迁移
- JWT 使用硬编码密钥（仅本地安全）
- 全量日志便于调试

### staging 环境

**用途**：预发布测试、集成验证

**特点**：
- 连接真实 MySQL
- Actuator 开放 health + info（内部监控）
- 日志级别 INFO（平衡调试与性能）
- CORS 可配置（默认为空，需显式设置）

### prod 环境

**用途**：生产部署

**特点**：
- 连接生产 MySQL（连接池优化）
- Actuator 仅 health（最小暴露面）
- 日志 WARN 级别，结构化输出
- CORS 必须显式配置（安全要求）
- Flyway 仅验证，不自动 DDL

## 相关文件

- `claude-j-start/src/main/resources/application.yml` - 通用配置
- `claude-j-start/src/main/resources/application-dev.yml` - 开发配置
- `claude-j-start/src/main/resources/application-staging.yml` - 预发布配置
- `claude-j-start/src/main/resources/application-prod.yml` - 生产配置
- `docs/devops/Dockerfile` - 容器镜像（默认 prod profile）
