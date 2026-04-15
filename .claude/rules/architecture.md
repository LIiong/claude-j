# 架构约束规则（Claude Rules）

## 适用范围
- 适用于所有涉及模块依赖、对象流转、聚合边界的代码与设计变更。
- 目标：确保六边形架构边界长期稳定，避免架构漂移。

## MUST（强制）

### 依赖方向
- 只允许：`adapter -> application -> domain <- infrastructure`。
- `start` 仅做装配，不承载业务规则。
- 新增模块时必须先声明其层级，再决定依赖关系。

### 对象边界
- 必须遵循对象转换链：`Request/Response <-> DTO <-> Domain <-> DO`。
- DO 只能存在于 `infrastructure` 层。
- Request/Response 只能存在于 `adapter` 层。
- Domain 对象不能直接暴露为 REST 响应。

### 聚合与事务
- 聚合根必须封装不变量与状态流转。
- 跨聚合协作优先使用领域事件，不进行聚合间直接写操作。
- 一个事务应只修改一个聚合（跨聚合场景使用最终一致性方案）。

## MUST NOT（禁止）
- 禁止 `domain` 依赖任何外层模块。
- 禁止 `application` 直接访问数据库或基础设施实现。
- 禁止 `adapter` 绕过 `application` 直接调用持久化层。
- 禁止在 `infrastructure` 中堆积领域业务决策逻辑。

## 执行检查（每次改动后）
1. 检查 import 与模块依赖，确认未出现逆向依赖。
2. 检查对象类型是否跨层泄漏（DO/DTO/Request/Response/Domain）。
3. 运行 `mvn test` 与 `./scripts/entropy-check.sh`，验证架构约束仍成立。
