# 架构文档

> 此文件为架构概览模板。移植时请替换所有 `${VARIABLE}` 占位符。
> 变量定义详见 `docs/templates/project/variables.md`。

## 1. 概述

${PROJECT_NAME} 是基于 **DDD（领域驱动设计）+ 六边形架构（端口与适配器）** 的 ${PROJECT_DESCRIPTION}。

**技术栈**：
- ${LANGUAGE_VERSION}（源码兼容）
- ${FRAMEWORK}
- ${BUILD_TOOL}
- ${ORM}
- ${MAPPER_TOOL}
- ${DB_DEV}（开发环境）/ ${DB_PROD}（生产环境）

## 2. 架构图

```
                    ┌─────────────────────────────────────────────┐
                    │                  Adapter                     │
                    │       （REST 控制器、请求/响应对象）             │
                    │            仅依赖：application                │
                    └──────────────────┬──────────────────────────┘
                                       │
                                       ▼
                    ┌─────────────────────────────────────────────┐
                    │               Application                    │
                    │     （用例编排、应用服务、命令、DTO）             │
                    │            仅依赖：domain                     │
                    └──────────────────┬──────────────────────────┘
                                       │
                                       ▼
                    ┌─────────────────────────────────────────────┐
                    │                  Domain                      │
                    │   （聚合根、实体、值对象、端口接口）               │
                    │            不依赖任何模块                      │
                    └──────────────────▲──────────────────────────┘
                                       │
                    ┌──────────────────┴──────────────────────────┐
                    │              Infrastructure                   │
                    │    （${ORM}、Repository 实现、转换器）          │
                    │      实现：domain + application 接口           │
                    └─────────────────────────────────────────────┘
```

### 依赖方向
```
adapter -> application -> domain <- infrastructure
```

## 3. 模块职责

### ${MODULE_DOMAIN}（领域层）
- **目的**：纯领域模型、业务规则、不变量
- **包含**：聚合根、实体、值对象、领域服务、Repository 端口接口、领域事件
- **依赖**：无（仅语言标准库）
- **禁止框架导入**：无框架、无 ORM、无 HTTP

### ${MODULE_APPLICATION}（应用层）
- **目的**：用例编排、应用工作流
- **包含**：应用服务、命令、查询、DTO、组装器（${MAPPER_TOOL}）
- **依赖**：仅 domain 层

### ${MODULE_INFRASTRUCTURE}（基础设施层）
- **目的**：实现 domain/application 定义的技术适配器
- **包含**：${ORM} Mapper、数据对象（DO）、Repository 实现、转换器、外部服务客户端
- **依赖**：domain + application（实现其接口）

### ${MODULE_ADAPTER}（适配器层）
- **目的**：入站适配器（HTTP API）
- **包含**：REST 控制器、请求/响应对象、异常处理器
- **依赖**：仅 application 层

### ${MODULE_START}（启动模块）
- **目的**：应用组装与启动
- **包含**：主类、配置文件、DDL 脚本
- **依赖**：所有模块

## 4. 对象转换链

```
┌──────────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│Request/Response│◄──►│   DTO    │◄──►│  Domain  │◄──►│    DO    │
│  （adapter）   │    │（application）│ │ （domain）│    │（infra）  │
└──────────────┘    └──────────┘    └──────────┘    └──────────┘
                     Assembler        （聚合根）      Converter
                    （${MAPPER_TOOL}）                （${MAPPER_TOOL}）
```

- 所有转换使用 ${MAPPER_TOOL}
- DO 禁止泄漏到 infrastructure 层之上
- Request/Response 禁止泄漏到 adapter 层之下
- Domain 禁止直接作为 REST 响应返回

## 5. 技术选型表

| 技术 | 用途 | 选型理由 |
|-----|------|---------|
| ${FRAMEWORK} | 应用框架 | {填写选型理由} |
| ${ORM} | 持久化 | {填写选型理由} |
| ${MAPPER_TOOL} | 对象映射 | {填写选型理由} |
| ${TEST_FRAMEWORK} | 测试 | {填写选型理由} |
| ${DB_DEV} | 开发数据库 | {填写选型理由} |
| ${DB_PROD} | 生产数据库 | {填写选型理由} |

## 6. 命名约定

| 类型 | 规范 | 示例 |
|------|------|------|
| 聚合根 | 无后缀 | `Order` |
| 实体 | 无后缀 | `OrderItem` |
| 值对象 | 无后缀 | `Money`、`OrderId` |
| 数据对象 | `DO` 后缀 | `OrderDO` |
| DTO | `DTO` 后缀 | `OrderDTO` |
| 命令 | `Command` 后缀 | `CreateOrderCommand` |
| Repository 端口 | `Repository` 后缀 | `OrderRepository`（domain） |
| Repository 实现 | `RepositoryImpl` 后缀 | `OrderRepositoryImpl`（infra） |
| 应用服务 | `ApplicationService` 后缀 | `OrderApplicationService` |
| 控制器 | `Controller` 后缀 | `OrderController` |
| 转换器 | `Converter` 后缀 | `OrderConverter`（DO↔Domain） |
| 组装器 | `Assembler` 后缀 | `OrderAssembler`（Domain↔DTO） |

## 7. 三层守护

| 层 | 工具 | 触发时机 | 覆盖 |
|---|------|---------|------|
| L1 | `guard-layer.sh` | 编辑期 | 分层依赖 / Domain 纯净 / DO 泄漏 / 语法版本 |
| L2 | 架构测试（${ARCHUNIT_RULE_COUNT} 条） | ${CMD_RUN_TESTS} | 架构规则 + 测试命名 |
| L3 | ${CMD_ENTROPY_CHECK}（${ENTROPY_CHECK_COUNT} 项） | 交付前 | 全局一致性 |

## 8. 参考示例

本项目的示例业务聚合已在 `docs/exec-plan/archived/` 中归档，可作为移植时的参考。推荐先阅读 `001-*` 任务的完整交付产物（设计→评审→开发→验收）以理解工作流。
