# 测试用例设计 — 008-order-from-cart

## 测试范围
验证从购物车创建订单功能的完整流程，包括正常流程、异常场景、数据一致性。

## 测试策略
按测试金字塔分层测试 + 代码审查 + 风格检查。

---

## 一、Domain 层测试场景

本功能无新增 Domain 代码，复用现有 Order 和 Cart 聚合能力。

---

## 二、Application 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| A1 | 正常从购物车创建订单 | 购物车存在且有商品 | createOrderFromCart(cmd) | 返回 OrderDTO，订单已保存 |
| A2 | 购物车不存在 | 用户ID不存在 | createOrderFromCart(cmd) | 抛 BusinessException(CART_NOT_FOUND) |
| A3 | 购物车为空 | 购物车存在但无商品 | createOrderFromCart(cmd) | 抛 BusinessException(CART_EMPTY) |
| A4 | 客户ID为空 | customerId 为 null/空 | createOrderFromCart(cmd) | 抛 BusinessException(客户ID不能为空) |
| A5 | 购物车清空验证 | 创建订单成功后 | 检查 cart.isEmpty() | 返回 true，cartRepository.save 被调用 |

---

## 三、Infrastructure 层测试场景

本功能无新增 Infrastructure 代码，复用现有 Repository 实现。

---

## 四、Adapter 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| W1 | 成功从购物车创建订单 | Mock 返回正常订单 | POST /api/v1/orders/from-cart | 200 + success=true + 订单数据 |
| W2 | 客户ID为空 | request.customerId = "" | POST /api/v1/orders/from-cart | 400 Bad Request |
| W3 | 购物车不存在 | Mock 抛 CART_NOT_FOUND | POST /api/v1/orders/from-cart | 404 + errorCode=CART_NOT_FOUND |
| W4 | 购物车为空 | Mock 抛 CART_EMPTY | POST /api/v1/orders/from-cart | 400 + errorCode=CART_EMPTY |

---

## 五、集成测试场景（全链路）

| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E1 | 正常流程：从购物车创建订单 | 1. 添加商品到购物车<br>2. 调用 POST /from-cart<br>3. 查询订单确认创建<br>4. 查询购物车确认清空 | 订单创建成功，购物车已清空 |
| E2 | 多商品订单金额正确 | 添加多个商品到购物车，创建订单 | 订单总金额 = 各商品单价×数量之和 |
| E3 | 异常：购物车不存在 | 使用不存在的用户ID调用 POST /from-cart | 404 + CART_NOT_FOUND |
| E4 | 异常：购物车为空 | 1. 创建购物车<br>2. 清空购物车<br>3. 调用 POST /from-cart | 400 + CART_EMPTY |
| E5 | 参数校验：客户ID为空 | 使用空 customerId 调用 POST /from-cart | 400 Bad Request |
| E6 | 数据一致性：购物车清空 | 创建订单后查询购物车 | itemCount=0，items 为空数组 |

---

## 六、代码审查检查项

- [x] 依赖方向正确（adapter → application → domain ← infrastructure）
- [x] domain 模块无 Spring/框架 import
- [x] 应用服务正确编排领域对象（Cart + Order）
- [x] Repository 接口在 domain，实现在 infrastructure
- [x] 对象转换链正确：Request → Command → Domain → DTO → Response
- [x] Controller 无业务逻辑，仅委托 application service
- [x] 异常通过 GlobalExceptionHandler 统一处理
- [x] @Transactional 保证事务一致性

## 七、代码风格检查项

- [x] Java 8 兼容（无 var、records、text blocks、List.of）
- [x] Command 使用 @Data
- [x] Request 使用 @Data + @Valid 注解
- [x] 命名规范符合约定
- [x] 包结构符合 com.claudej.{layer}.{aggregate}.{sublayer}
- [x] 测试命名 should_xxx_when_xxx
