---
description: "六边形架构约束：依赖方向、对象边界、聚合设计。操作 Java 源码、SQL、POM 或 Mapper XML 时强制生效。"
globs:
  - "**/*.java"
  - "**/pom.xml"
  - "**/schema.sql"
  - "**/mapper/*.xml"
alwaysApply: false
---

# 架构约束规则

## 适用范围
- **生效时机**：编辑 Java 源码、SQL、POM 或 Mapper XML 时自动注入。
- **目标**：确保六边形架构边界长期稳定，避免架构漂移。

## MUST（强制）

### 依赖方向
- 只允许：`adapter -> application -> domain <- infrastructure`。
- `start` 仅做装配，不承载业务规则。
- 新增模块时必须先声明其层级，再决定依赖关系。

### 对象边界
- 对象转换链必须完整：`Request/Response <-> DTO <-> Domain <-> DO`。
- DO 只能存在于 `infrastructure` 层，禁止泄漏。
- Request/Response 只能存在于 `adapter` 层，禁止向下传递。
- Domain 对象禁止直接暴露为 REST 响应。

### 聚合与事务
- 聚合根必须封装所有业务不变量，状态变更仅通过聚合方法。
- 跨聚合协作优先使用领域事件，禁止聚合间直接写操作。
- 一个事务只修改一个聚合（跨聚合场景采用最终一致性方案）。

## MUST NOT（禁止）
- 禁止 `domain` 层出现 `org.springframework.*` 或 `com.baomidou.*` 的 import。
- 禁止 `application` 直接访问数据库或基础设施实现。
- 禁止 `adapter` 绕过 `application` 直接调用持久化层。
- 禁止在 `infrastructure` 中堆积领域业务决策逻辑。
- 禁止使用 `var`、records、text blocks、switch 表达式、`List.of()`、`Map.of()`。
- 禁止聚合根/实体使用 `@Setter` 或 `@Data`。

## 执行检查（每次改动后）
1. 检查 import 与模块依赖，确认未出现逆向依赖。
2. 检查对象类型是否跨层泄漏（DO/DTO/Request/Response/Domain）。
3. 运行 `mvn test` 与 `./scripts/entropy-check.sh`，验证架构约束仍成立。
