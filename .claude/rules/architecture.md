# 架构约束规则

## 模块依赖方向（绝不违反）

```
adapter ──→ application ──→ domain ←── infrastructure
  │                           ▲              │
  │         start ────────────┼──────────────┘
  │         (assembles all)   │
  └───────────────────────────┘
```

**禁止方向**：
- domain → 任何其他层（domain 是最内层）
- application → infrastructure 或 adapter
- adapter → infrastructure

## 对象转换链

```
Request/Response ←→ DTO ←→ Domain ←→ DO
  (adapter)      (application)  (domain)  (infrastructure)
```

- 所有转换使用 MapStruct `@Mapper(componentModel = "spring")`
- DO 对象禁止泄漏到 infrastructure 层之上
- Request/Response 对象禁止泄漏到 adapter 层之下
- Domain 对象禁止直接作为 REST 响应返回

## 聚合边界规则

- 聚合根封装所有业务不变量（禁止贫血模型）
- 聚合根状态变更仅通过其自身方法（禁止公开 setter）
- 值对象不可变 — 所有字段 final，重写 equals/hashCode
- 跨聚合通信使用领域事件，不直接引用
- 一个事务只修改一个聚合

## 各层允许/禁止

| 层 | 允许 | 禁止 |
|---|------|------|
| domain | 纯 Java、Lombok @Getter | Spring 注解、框架 import、@Setter、@Data |
| application | @Service、@Transactional、MapStruct | 直接 DB 访问、HTTP 相关 |
| infrastructure | @Repository、MyBatis-Plus、MapStruct | 业务逻辑 |
| adapter | @RestController、@Valid、Spring Web | 业务逻辑、直接 DB 访问 |

## 新聚合开发顺序 Checklist

1. [ ] domain：聚合根 + 实体 + 值对象 + Repository 端口 + 领域服务端口
2. [ ] application：命令 + DTO + 组装器 + 应用服务
3. [ ] infrastructure：DO + Mapper + 转换器 + Repository 实现 + 服务实现
4. [ ] adapter：请求/响应 + Controller
5. [ ] start：schema.sql DDL + 配置更新
6. [ ] 各层编写测试
7. [ ] 验证：mvn test + mvn checkstyle:check + ./scripts/entropy-check.sh
