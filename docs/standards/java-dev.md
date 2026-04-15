# Java 开发规则（Claude Rules）

## 适用范围
- 适用于所有 `*.java`、`*.xml`（MyBatis Mapper）及相关 DDL 变更。
- 目标：确保代码符合 DDD + 六边形架构、Java 8 约束和对象边界约束。

## MUST（强制）

### 分层与依赖
- 依赖方向必须始终为：`adapter -> application -> domain <- infrastructure`。
- `domain` 必须保持纯净，不得依赖 Spring / MyBatis-Plus。
- `adapter` 只能依赖 `application`，不得直接访问数据库或 `infrastructure`。
- `application` 负责用例编排，不直接操作持久化实现细节。

### Java 8 与编码约定
- 必须使用 Java 8 语法：显式类型、传统 switch、常规字符串字面量。
- 集合工厂方法必须使用 Java 8 兼容方案（如 `Arrays.asList`）。
- 包命名必须使用 `com.claudej.{layer}.{aggregate}.{sublayer}`。
- 表名必须为 `t_{entity}`，列名必须为 `snake_case`。

### 领域建模与对象边界
- 聚合根必须封装业务不变量，状态变更只能通过聚合方法。
- 值对象必须不可变，并具备正确的相等语义。
- Repository 端口返回领域对象，不返回 DO。
- 转换链必须保持：`Request/Response <-> DTO <-> Domain <-> DO`。
- 对象转换应使用 MapStruct：`@Mapper(componentModel = "spring")`。

### 持久化与实现约束
- Mapper 必须继承 `BaseMapper<XxxDO>`。
- DO 必须标注 `@TableName`，主键按规范使用 `@TableId`。
- 复杂查询必须放在 `resources/mapper/` 的 XML 文件中。

## MUST NOT（禁止）
- 禁止在 `domain` 中出现 `org.springframework.*` 或 `com.baomidou.*` import。
- 禁止使用 `var`、records、text blocks、switch 表达式、`List.of()`、`Map.of()`。
- 禁止 DO 泄漏到 `infrastructure` 之外。
- 禁止 Request/Response 泄漏到 `adapter` 之下。
- 禁止将 Domain 对象直接作为 REST 响应输出。
- 禁止在聚合根/实体上使用 `@Setter` 或 `@Data`。

## 执行检查（每次改动后）
1. 按顺序实现：Domain -> Application -> Infrastructure -> Adapter -> Start。
2. 运行 `mvn test`，确保架构规则与单测通过。
3. 运行 `mvn checkstyle:check`，确保风格合规。
4. 运行 `./scripts/entropy-check.sh`，确保无架构漂移。
