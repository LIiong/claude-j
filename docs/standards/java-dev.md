# Java 开发规则

## DDD 分层规则

### 依赖方向（绝不违反）
```
adapter -> application -> domain <- infrastructure
```

### 层边界
- **domain**：纯 Java。禁止 `import org.springframework.*`、禁止 `import com.baomidou.*`。聚合根仅允许 Lombok @Getter。
- **application**：可使用 @Service、@Transactional。仅依赖 domain。
- **infrastructure**：实现 domain/application 接口。使用 MyBatis-Plus、Spring Data。
- **adapter**：REST 控制器。仅依赖 application。禁止直接 import domain 类。

## 编码规范

### Java 8 兼容性
- 禁止 `var` 关键字
- 禁止 records
- 禁止 text blocks（"""）
- 禁止 switch 表达式
- 禁止 `List.of()`、`Map.of()` — 使用 `Arrays.asList()`、`Collections.unmodifiableMap()`
- 尽可能使用菱形操作符 `<>`

### Lombok 使用规范
- 领域聚合根/实体：仅 `@Getter`（禁止 @Setter、禁止 @Data）
- 值对象：`@Getter`、`@EqualsAndHashCode`、`@ToString`
- 数据对象（DO）：`@Data`、`@TableName`
- DTO：`@Data`
- 命令：`@Data`
- 请求/响应：`@Data`

### 命名规范
- 包：`com.claudej.{layer}.{aggregate}.{sublayer}`
- 类名：参见 dev agent 命名表
- 方法：camelCase，命令动词开头（`createOrder`），查询用名词（`getOrder`）
- 常量：UPPER_SNAKE_CASE
- 数据库表：`t_{entity}`（如 `t_order`、`t_order_item`）
- 数据库列：snake_case

## 对象转换规则
```
Request/Response（adapter）<-> DTO（application）<-> Domain（domain）<-> DO（infrastructure）
```
- 所有转换使用 MapStruct `@Mapper(componentModel = "spring")`
- DO 对象禁止泄漏到 infrastructure 层之上
- Request/Response 对象禁止泄漏到 adapter 层之下
- Domain 对象禁止直接作为 REST 响应返回

## MyBatis-Plus 规范
- 所有 Mapper 继承 `BaseMapper<XxxDO>`
- 数据对象使用 `@TableName("t_xxx")` 注解
- 非标准列映射使用 `@TableField`
- 主键使用 `@TableId(type = IdType.AUTO)`
- 复杂查询放在 XML mapper 文件（`resources/mapper/`）

## 领域建模规则
- 聚合根封装所有业务不变量
- 状态变更仅通过聚合方法进行（禁止公开 setter）
- 值对象不可变 — 所有字段 final，重写 equals/hashCode
- 领域事件用于跨聚合副作用
- Repository 接口返回领域对象，不返回 DO
- 领域规则违反使用 BusinessException + ErrorCode

## 新功能开发顺序
1. Domain：聚合根、实体、值对象、Repository 端口、领域服务
2. Application：命令/DTO、组装器、应用服务
3. Infrastructure：DO、Mapper、转换器、Repository 实现
4. Adapter：控制器、请求/响应对象
5. Start：schema.sql 中添加 DDL，必要时更新配置
