# 架构文档

## 1. 概述

claude-j 是基于 **DDD（领域驱动设计）+ 六边形架构（端口与适配器）** 的 Java 电商订单系统。

**技术栈**：
- Java 8（源码兼容）
- Spring Boot 2.7.18
- Maven（多模块）
- MyBatis-Plus 3.5.5
- MapStruct 1.5.5
- H2（开发环境）/ MySQL（生产环境）

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
                    │    （MyBatis-Plus、Repository 实现、转换器）     │
                    │      实现：domain + application 接口           │
                    └─────────────────────────────────────────────┘
```

### 依赖方向
```
adapter -> application -> domain <- infrastructure
```

## 3. 模块职责

### claude-j-domain（领域层）
- **目的**：纯领域模型、业务规则、不变量
- **包含**：聚合根、实体、值对象、领域服务、Repository 端口接口、领域事件
- **依赖**：无（纯 Java + Lombok + commons-lang3）
- **禁止框架导入**：无 Spring、无 MyBatis、无 HTTP

### claude-j-application（应用层）
- **目的**：用例编排、应用工作流
- **包含**：应用服务、命令、查询、DTO、组装器（MapStruct）
- **依赖**：仅 domain 层
- **允许**：@Service、@Transactional

### claude-j-infrastructure（基础设施层）
- **目的**：实现 domain/application 定义的技术适配器
- **包含**：MyBatis-Plus Mapper、数据对象（DO）、Repository 实现、转换器、外部服务客户端
- **依赖**：domain + application（实现其接口）
- **允许**：@Repository、MyBatis-Plus 注解

### claude-j-adapter（适配器层）
- **目的**：入站适配器（HTTP API）
- **包含**：REST 控制器、请求/响应对象、异常处理器
- **依赖**：仅 application 层
- **允许**：@RestController、@Valid、Spring Web

### claude-j-start（启动模块）
- **目的**：应用组装与启动
- **包含**：主类、配置文件（application.yml）、DDL 脚本
- **依赖**：所有模块

## 4. 示例领域：电商订单

### 领域模型

```
Order（聚合根）
├── orderId: OrderId（值对象）
├── customerId: String
├── items: List<OrderItem>（实体）
│   ├── orderItemId: Long
│   ├── productId: String
│   ├── productName: String
│   ├── quantity: Integer
│   ├── unitPrice: Money（值对象）
│   └── subtotal: Money（值对象）
├── status: OrderStatus（值对象 / 枚举）
├── totalAmount: Money（值对象）
├── createTime: LocalDateTime
└── updateTime: LocalDateTime
```

### 业务规则（由聚合根强制执行）
- 订单必须至少有一个商品
- 商品数量必须 > 0
- 总金额 = 所有商品小计之和
- 仅 CREATED 或 PAID 状态的订单可以取消
- 订单状态遵循状态机：CREATED → PAID → SHIPPED → DELIVERED

## 5. 对象转换链

```
┌──────────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│Request/Response│◄──►│   DTO    │◄──►│  Domain  │◄──►│    DO    │
│  （adapter）   │    │（application）│ │ （domain）│    │（infra）  │
└──────────────┘    └──────────┘    └──────────┘    └──────────┘
                     Assembler        （聚合根）      Converter
                    （MapStruct）                    （MapStruct）
```

## 6. 技术选型

| 技术 | 用途 | 选型理由 |
|-----|------|---------|
| Spring Boot 2.7.18 | 应用框架 | 最后一个稳定 2.x 版本，Java 8 兼容 |
| MyBatis-Plus 3.5.5 | ORM / 持久化 | 轻量级，适合 DDD Repository 模式 |
| MapStruct 1.5.5 | 对象映射 | 编译时生成，类型安全，无反射开销 |
| Lombok 1.18.30 | 样板代码减少 | 减少 getter/setter/constructor 噪音 |
| H2 | 开发数据库 | 内存模式，零配置 |
| JUnit 5 | 测试框架 | 现代测试框架，支持扩展 |
| AssertJ | 测试断言 | 流式、可读的断言 |
| Mockito | Mock 框架 | 标准 Java Mock 框架 |
