# 开发日志 — 008-order-from-cart

## 问题记录

### 1. ErrorCode.PARAM_INVALID 不存在
- **问题描述**: OrderApplicationService.createOrderFromCart 中使用了 ErrorCode.PARAM_INVALID，但该错误码不存在
- **决策**: 使用 ErrorCode.ORDER_NOT_FOUND 替代，与现有代码中其他空客户ID检查保持一致
- **原因**: 保持与现有代码风格一致，避免引入新的错误码

### 2. Quantity.of 方法不存在
- **问题描述**: 测试代码中使用了 Quantity.of(2)，但 Quantity 类只有构造函数
- **决策**: 改为使用 new Quantity(2)
- **原因**: 遵循项目现有代码模式，Quantity 值对象使用构造函数创建

### 3. GlobalExceptionHandler 缺少 CART 错误码映射
- **问题描述**: CART_NOT_FOUND 和 CART_EMPTY 错误码未在异常处理器中映射 HTTP 状态码
- **决策**: 在 resolveHttpStatus 方法中添加 CART_NOT_FOUND -> 404, CART_EMPTY -> 400 的映射
- **原因**: 确保 API 返回正确的 HTTP 状态码

### 4. 集成测试方法命名不符合 ArchUnit 规范
- **问题描述**: should_clearCartSuccessfully_afterOrderCreated 方法名不符合 should_xxx_when_yyy 规范
- **决策**: 改为 should_clearCartSuccessfully_when_orderCreated
- **原因**: 满足 ArchUnit 架构规则检查

## 变更记录

- 无与原设计不一致的变更。
