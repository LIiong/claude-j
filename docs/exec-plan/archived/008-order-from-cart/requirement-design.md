# 008-order-from-cart 需求设计

## 需求描述

支持用户从购物车直接创建订单的完整流程，作为购物车与订单之间的关键桥梁功能。

当前系统已有：
- ✅ 用户管理（注册、查询、冻结/解冻）
- ✅ 购物车（添加商品、更新数量、删除、清空、查询）
- ✅ 订单（创建、查询、支付、取消）
- ✅ 购买记录查询（用户订单列表、订单详情）

**缺失功能：从购物车创建订单的桥梁**

## 功能需求

### 1. 从购物车创建订单 API

**接口**：`POST /api/v1/orders/from-cart`

**功能**：
- 接收用户ID，将购物车商品转为订单
- 支持指定优惠券（可选，预留扩展）
- 下单成功后清空购物车
- 事务保证数据一致性

**请求体**：
```json
{
    "customerId": "USER001",
    "couponId": "COUPON001"  // 可选，预留扩展
}
```

**响应**：
```json
{
    "success": true,
    "data": {
        "orderId": "ORDER20250415001",
        "customerId": "USER001",
        "status": "CREATED",
        "totalAmount": 299.99,
        "currency": "CNY",
        "items": [
            {
                "productId": "PROD001",
                "productName": "iPhone 15",
                "quantity": 1,
                "unitPrice": 299.99,
                "subtotal": 299.99
            }
        ],
        "createTime": "2026-04-15T10:00:00"
    }
}
```

**业务流程**：
```
1. 根据 customerId 查询用户购物车
2. 验证购物车非空
3. 验证用户存在且状态正常（可选，预留扩展）
4. 创建订单，将购物车商品转为订单项
5. (可选) 应用优惠券折扣（预留扩展）
6. 保存订单
7. 清空购物车
8. 返回订单信息
```

**异常场景**：

| 场景 | HTTP状态码 | 错误码 | 错误消息 |
|------|-----------|--------|----------|
| 购物车不存在 | 404 | CART_NOT_FOUND | 购物车不存在 |
| 购物车为空 | 400 | CART_EMPTY | 购物车为空，无法下单 |
| 客户ID为空 | 400 | PARAM_INVALID | 客户ID不能为空 |

## 技术方案

### 分层实现

| 层级 | 变更内容 |
|------|---------|
| adapter | CreateOrderFromCartRequest.java、OrderController 添加 POST /from-cart 端点 |
| application | CreateOrderFromCartCommand.java、OrderApplicationService 添加 createOrderFromCart 方法 |
| domain | 无需变更，使用现有 Order、Cart 聚合能力 |
| infrastructure | 无需变更 |

### 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                        Adapter Layer                        │
│  OrderController.createOrderFromCart()                      │
│    ↓ Request/Response 转换                                  │
│  CreateOrderFromCartRequest                                 │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                     Application Layer                       │
│  OrderApplicationService.createOrderFromCart()              │
│    - 编排 CartRepository + OrderRepository                  │
│    - @Transactional 事务保证                                │
│    ↓ Command/Domain 转换                                    │
│  CreateOrderFromCartCommand                                 │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                           │
│  Cart（聚合根）- clear() 清空方法                           │
│  Order（聚合根）- create() + addItem() 创建订单             │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                   Infrastructure Layer                      │
│  CartRepositoryImpl / OrderRepositoryImpl（无需变更）       │
└─────────────────────────────────────────────────────────────┘
```

### 事务边界

```java
@Transactional
public OrderDTO createOrderFromCart(CreateOrderFromCartCommand command) {
    // 1. 查询购物车
    // 2. 验证购物车非空
    // 3. 创建订单
    // 4. 保存订单
    // 5. 清空购物车
    // 6. 保存购物车（空状态）
    // 以上所有操作在同一个事务中
}
```

### 领域规则复用

**Cart 聚合提供的能力**：
- `findByUserId(userId)` - 查询购物车
- `isEmpty()` - 验证购物车非空
- `getItems()` - 获取购物车项列表
- `clear()` - 清空购物车

**Order 聚合提供的能力**：
- `Order.create(customerId)` - 创建订单
- `addItem(orderItem)` - 添加订单项
- `orderRepository.save(order)` - 保存订单

**购物车项转订单项映射**：
| 购物车项字段 | 订单项字段 |
|-------------|-----------|
| productId | productId |
| productName | productName |
| unitPrice | unitPrice |
| quantity.value | quantity |
| subtotal | subtotal（计算） |

### 约束检查

- ✅ 遵循 DDD 分层架构（adapter → application → domain ← infrastructure）
- ✅ 遵循 Java 8 规范（无 var/List.of/Map.of/text blocks）
- ✅ 使用 MapStruct 进行对象转换（如需要）
- ✅ @Transactional 事务保证数据一致性
- ✅ 应用服务层编排多个聚合（Cart + Order），通过事务保证一致性
- ✅ 复用现有领域能力，不修改 domain 层代码

## 新增错误码

```java
// ErrorCode.java 需要添加
CART_EMPTY("CART_EMPTY", "购物车为空，无法下单")
```

## 变更清单

### 新增文件

| # | 文件路径 | 说明 |
|---|----------|------|
| 1 | `claude-j-adapter/src/main/java/com/claudej/adapter/order/web/request/CreateOrderFromCartRequest.java` | 从购物车创建订单请求 |
| 2 | `claude-j-application/src/main/java/com/claudej/application/order/command/CreateOrderFromCartCommand.java` | 从购物车创建订单命令 |

### 修改文件

| # | 文件路径 | 变更内容 |
|---|----------|----------|
| 3 | `claude-j-adapter/src/main/java/com/claudej/adapter/order/web/OrderController.java` | 添加 POST /from-cart 端点 |
| 4 | `claude-j-application/src/main/java/com/claudej/application/order/service/OrderApplicationService.java` | 添加 createOrderFromCart 方法，注入 CartRepository |
| 5 | `claude-j-domain/src/main/java/com/claudej/domain/common/exception/ErrorCode.java` | 添加 CART_EMPTY 错误码 |

### 测试文件

| # | 文件路径 | 说明 |
|---|----------|------|
| 6 | `claude-j-application/src/test/java/com/claudej/application/order/service/OrderApplicationServiceTest.java` | 添加 createOrderFromCart 单元测试 |
| 7 | `claude-j-adapter/src/test/java/com/claudej/adapter/order/web/OrderControllerTest.java` | 添加 POST /from-cart API 测试 |
| 8 | `claude-j-start/src/test/java/com/claudej/start/order/OrderFromCartIntegrationTest.java` | 添加集成测试 |

## 架构评审

**评审人**：@architect
**日期**：2026-04-15
**结论**：✅ 通过

### 评审检查项

- [x] 聚合根边界合理（遵循事务一致性原则）
- [x] 值对象识别充分（金额、标识符等应为 VO）
- [x] Repository 端口粒度合适（方法不多不少）
- [x] 与已有聚合无循环依赖
- [x] DDL 设计与领域模型一致（字段映射、索引合理）
- [x] API 设计符合 RESTful 规范
- [x] 对象转换链正确（DO ↔ Domain ↔ DTO ↔ Request/Response）

### 评审意见

#### 1. 应用服务层依赖 CartRepository 和 OrderRepository —— 合理 ✅

- OrderApplicationService 是订单用例的编排中心，负责协调购物车与订单两个聚合
- 这是应用服务的核心职责：编排多个聚合完成业务流程
- 依赖方向正确：application → domain（CartRepository 和 OrderRepository 都是 domain 层定义的端口）

#### 2. 事务边界 —— 当前设计在单服务场景下合理 ✅

- `@Transactional` 保证购物车查询、订单创建、购物车清空的原子性
- 两个聚合在同一事务中修改，在单体应用中是可接受的做法
- 若未来拆分为微服务，需要引入 Saga 模式，但当前设计已预留扩展空间
- 无需引入分布式事务（2PC/XA），增加复杂度

#### 3. 用户状态验证 —— 建议作为可选项 ✅

- 设计文档已提及"验证用户存在且状态正常（可选，预留扩展）"
- 当前 M1 阶段可跳过用户验证，简化实现
- 如需添加，建议通过 UserDomainService 或 UserRepository 查询验证

#### 4. 优惠券预留扩展点 —— 设计合理 ✅

- couponId 作为可选字段传入是合适的扩展点
- 优惠券应用逻辑应放在 Order 聚合内（通过 `applyCoupon()` 方法）
- 当前阶段仅预留字段，不实现具体逻辑，符合迭代开发原则

#### 5. DDD 六边形架构合规性 —— 完全合规 ✅

- 依赖方向：adapter → application → domain，符合架构约束
- 无 domain 层 Spring/MyBatis-Plus import
- Repository 是 domain 层定义的端口，infrastructure 层实现
- 符合已有代码模式（参考 OrderApplicationService 现有实现）

#### 6. Java 8 编码规范 —— 合规 ✅

- 无 var、List.of()、Map.of() 使用
- 使用构造器注入模式（符合现有代码风格）
- 使用传统 setter 进行对象转换（符合现有代码模式）

### 建议（非阻塞）

1. **聚合操作顺序优化**：建议先验证用户状态（如果实现），再查询购物车，减少无效查询
2. **幂等性考虑**：未来可考虑为 createOrderFromCart 添加幂等键，防止重复下单
3. **事件发布**：清空购物车后，可考虑发布 CartClearedEvent（用于后续积分、推荐等场景）

### 需要新增的 ADR

无需新增 ADR。本功能遵循已有架构决策，属于在现有框架内的功能实现。

## 接口规范

### 请求参数

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| customerId | String | 是 | 客户ID |
| couponId | String | 否 | 优惠券ID（预留扩展） |

### 响应字段

| 字段名 | 类型 | 说明 |
|--------|------|------|
| success | Boolean | 是否成功 |
| data | Object | 订单信息 |
| data.orderId | String | 订单号 |
| data.customerId | String | 客户ID |
| data.status | String | 订单状态（CREATED） |
| data.totalAmount | BigDecimal | 订单总金额 |
| data.currency | String | 币种 |
| data.items | Array | 订单项列表 |
| data.createTime | String | 创建时间 |

### HTTP 状态码

| 状态码 | 场景 |
|--------|------|
| 200 | 下单成功 |
| 400 | 请求参数错误（客户ID为空、购物车为空） |
| 404 | 购物车不存在 |
| 500 | 服务器内部错误 |
