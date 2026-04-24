# 需求拆分设计 — 018-actuator-health

## 需求描述

配置 Spring Boot Actuator 健康检查端点，开放 `/actuator/health` 和 `/actuator/info` 供 K8s 探针使用，实现 liveness/readiness 分离策略，为容器化部署提供基础的健康监控能力。

## 基础设施分析

### 任务性质

这是一个**纯基础设施配置任务**，不涉及业务聚合。主要工作集中在 `claude-j-start` 模块的配置层，无需创建 domain/application/infrastructure/adapter 业务代码。

### 现有依赖

| 依赖项 | 状态 | 位置 |
|--------|------|------|
| `spring-boot-starter-actuator` | 已存在 | `claude-j-start/pom.xml` |
| Actuator 基础配置 | 已存在（部分） | `application-dev.yml`, `application-staging.yml`, `application-prod.yml` |
| Spring Security | **不存在** | 项目未引入 Spring Security |

### 端点开放策略

| 端点 | 开放范围 | 用途 |
|------|---------|------|
| `/actuator/health` | 所有环境 | K8s 存活/就绪探针 |
| `/actuator/info` | 所有环境 | 应用信息展示 |
| `/actuator/health/liveness` | 所有环境 | K8s Liveness Probe |
| `/actuator/health/readiness` | 所有环境 | K8s Readiness Probe |
| `/actuator/metrics` | 仅 dev 环境 | 开发调试用 |
| `/actuator/prometheus` | 暂不开放 | 后续监控任务引入 |

### 健康检查分离策略

采用 Spring Boot 2.3+ 的 Liveness/Readiness 分离机制：

| 探针类型 | HTTP 路径 | 语义 | 响应状态码 |
|---------|----------|------|-----------|
| **Liveness** | `/actuator/health/liveness` | 应用是否存活（进程是否运行） | 200=存活, 500=异常 |
| **Readiness** | `/actuator/health/readiness` | 应用是否就绪（可接收流量） | 200=就绪, 500=未就绪 |

### 环境差异化配置

| 配置项 | dev | staging | prod |
|--------|-----|---------|------|
| 暴露端点 | health,info,metrics,env,liveness,readiness | health,info,liveness,readiness | health,liveness,readiness |
| health.show-details | always | when-authorized | never |
| management.server.port | 默认(8080) | 默认(8080) | 默认(8080) |

## 技术方案

### 方案 A：无安全框架（当前选择）

项目未引入 Spring Security，采用以下策略：

1. **端点隔离**：仅开放必要的 health/info 端点
2. **最小权限**：show-details 在生产环境设为 `never`
3. **网络层保护**：依赖 K8s Service/Ingress 层面的访问控制

**优点**：零额外依赖，配置简单
**缺点**：无法做到细粒度权限控制

### 方案 B：引入 Spring Security（备选，后续任务）

若后续需要更细粒度的安全控制：

1. 引入 `spring-boot-starter-security`
2. 配置 actuator 端点需要 admin 角色
3. 与现有 auth 聚合的 JWT 集成

**暂不采用原因**：当前需求仅需基础健康检查，避免过度设计（符合 Karpathy 原则 2：简洁优先）。

### 自定义健康指示器（可选扩展）

为关键依赖项实现自定义健康指示器：

| 健康指示器 | 检查内容 | 实现优先级 |
|-----------|---------|-----------|
| `DatabaseHealthIndicator` | H2/MySQL 连接可用性 | 低（Spring 默认已提供） |
| `JwtSecretHealthIndicator` | JWT_SECRET 配置有效性 | 中（与 010-secret-externalize 集成） |

**本次任务范围**：仅配置基础端点，自定义健康指示器作为后续扩展。

## 配置设计

### application.yml（主配置）

```yaml
management:
  endpoints:
    web:
      base-path: /actuator
  endpoint:
    health:
      probes:
        enabled: true  # 启用 liveness/readiness 分离
      show-details: never
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState,db
    info:
      enabled: true

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s  # 优雅关闭超时
```

### application-dev.yml

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env,liveness,readiness
  endpoint:
    health:
      show-details: always
    metrics:
      enabled: true
    env:
      enabled: true
```

### application-staging.yml

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,liveness,readiness
  endpoint:
    health:
      show-details: when-authorized
```

### application-prod.yml

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,liveness,readiness
  endpoint:
    health:
      show-details: never
```

## API 设计

| 方法 | 路径 | 描述 | 响应体 |
|------|------|------|--------|
| GET | `/actuator/health` | 综合健康状态 | `{ "status": "UP", "components": {...} }` |
| GET | `/actuator/health/liveness` | 存活探针 | `{ "status": "UP" }` |
| GET | `/actuator/health/readiness` | 就绪探针 | `{ "status": "UP" }` |
| GET | `/actuator/info` | 应用信息 | `{ "app": {...}, "java": {...} }` |

## 数据库设计

**无数据库变更**。本任务仅涉及配置文件修改。

## 影响范围

### 新增文件

| 模块 | 文件 | 说明 |
|------|------|------|
| start | 无 | 仅修改配置文件 |

### 修改文件

| 模块 | 文件 | 修改内容 |
|------|------|---------|
| start | `application.yml` | 添加 management 基础配置 |
| start | `application-dev.yml` | 扩展 actuator 端点暴露 |
| start | `application-staging.yml` | 调整端点配置 |
| start | `application-prod.yml` | 最小化端点暴露 |

### 无需修改

| 模块 | 原因 |
|------|------|
| domain | 无业务逻辑 |
| application | 无业务逻辑 |
| infrastructure | 无业务逻辑 |
| adapter | 无业务逻辑 |

## 假设与待确认

1. **假设**：项目暂不引入 Spring Security，依赖 K8s 网络层保护 actuator 端点
2. **假设**：生产环境的 actuator 端点仅通过 K8s 内部网络访问，不暴露到公网
3. **待确认**：是否需要自定义 `JwtSecretHealthIndicator` 与 010-secret-externalize 集成（本次暂不实现）

## 验收条件

1. 启动应用后 `/actuator/health` 返回 200 状态码
2. `/actuator/health/liveness` 和 `/actuator/health/readiness` 端点可用
3. `/actuator/info` 返回应用基本信息
4. 生产环境配置 `show-details: never` 生效
5. 各环境差异化配置正确生效