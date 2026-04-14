# 需求拆分设计 — 007-shopping-cart

## 需求描述
购物车功能：支持用户添加商品到购物车、修改商品数量、删除商品、清空购物车、查询购物车列表，计算购物车总金额。购物车以用户维度聚合，每个用户有且仅有一个购物车。

## 领域分析

### 聚合根: Cart（购物车）
- userId (String) — 用户ID，购物车与用户一一对应
- items (List<CartItem>) — 购物车商品项列表
- totalAmount (Money) — 购物车总金额（实时计算）
- createTime (LocalDateTime) — 创建时间
- updateTime (LocalDateTime) — 更新时间

### 实体: CartItem（购物车项）
属于 Cart 聚合内部实体，由 Cart 管理其生命周期。
- productId (String) — 商品ID
- productName (String) — 商品名称
- unitPrice (Money) — 商品单价
- quantity (Quantity) — 数量值对象
- subtotal (Money) — 小计金额（unitPrice * quantity）

### 值对象
- **Quantity**: 封装数量约束，必须大于0且不超过999。不可变，所有字段 final，重写 equals/hashCode。

### 领域服务（无）
购物车所有业务逻辑均封装在聚合根 Cart 中，无需独立领域服务。

### 端口接口
- **CartRepository**:
  - `Cart save(Cart cart)` — 保存购物车（含购物车项）
  - `Optional<Cart> findByUserId(String userId)` — 根据用户ID查找购物车
  - `void deleteByUserId(String userId)` — 根据用户ID删除购物车

### 聚合根核心行为
1. **addItem(productId, productName, unitPrice, quantity)** — 添加商品，若已存在相同商品则累加数量
2. **updateItemQuantity(productId, quantity)** — 修改指定商品数量
3. **removeItem(productId)** — 删除指定商品
4. **clear()** — 清空所有商品
5. **calculateTotalAmount()** — 重新计算总金额（内部方法，每次变更自动触发）

### 业务不变量
- 同一用户只有一个购物车
- 同一商品在购物车中只有一条记录（相同商品累加数量）
- 商品数量必须在 1~999 范围内
- 金额计算使用 Money 值对象保证精度
- 清空购物车后总金额为零

## 关键算法/技术方案

### 购物车存储策略
采用数据库持久化（t_cart + t_cart_item 两张表），与订单聚合保持一致的持久化模式。

### 金额计算
复用 order 聚合中已有的 Money 值对象（位于 `com.claudej.domain.order.model.valobj.Money`）。
**注意**：为避免跨聚合引用，在 cart 聚合下创建独立的 Money 值对象（`com.claudej.domain.cart.model.valobj.Money`），与 order 的 Money 保持相同逻辑但物理隔离。

### 添加商品合并策略
调用 addItem 时，若购物车中已存在相同 productId 的商品，则将数量累加到已有项，而非新增一条记录。

## API 设计

| 方法 | 路径 | 描述 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| POST | /api/v1/carts/items | 添加商品到购物车 | `{ "userId": "u001", "productId": "p001", "productName": "商品A", "unitPrice": 99.90, "quantity": 2 }` | `{ "success": true, "data": { CartResponse } }` |
| PUT | /api/v1/carts/items/{productId} | 修改商品数量 | `{ "userId": "u001", "quantity": 5 }` | `{ "success": true, "data": { CartResponse } }` |
| DELETE | /api/v1/carts/items/{productId} | 删除购物车商品 | Query: `?userId=u001` | `{ "success": true, "data": { CartResponse } }` |
| DELETE | /api/v1/carts | 清空购物车 | Query: `?userId=u001` | `{ "success": true, "data": null }` |
| GET | /api/v1/carts | 查询购物车 | Query: `?userId=u001` | `{ "success": true, "data": { CartResponse } }` |

### CartResponse 结构
```json
{
  "userId": "u001",
  "items": [
    {
      "productId": "p001",
      "productName": "商品A",
      "unitPrice": 99.90,
      "quantity": 2,
      "subtotal": 199.80
    }
  ],
  "totalAmount": 199.80,
  "currency": "CNY",
  "itemCount": 1
}
```

## 数据库设计

```sql
-- Cart table
CREATE TABLE IF NOT EXISTS t_cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '购物车总金额',
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_user_id (user_id)
);

-- Cart item table
CREATE TABLE IF NOT EXISTS t_cart_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL COMMENT '购物车ID',
    product_id VARCHAR(64) NOT NULL COMMENT '商品ID',
    product_name VARCHAR(256) NOT NULL COMMENT '商品名称',
    unit_price DECIMAL(12,2) NOT NULL COMMENT '单价',
    quantity INT NOT NULL COMMENT '数量',
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    subtotal DECIMAL(12,2) NOT NULL COMMENT '小计',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_cart_id (cart_id),
    UNIQUE KEY uk_cart_product (cart_id, product_id)
);
```

## 影响范围

- **domain**:
  - 新增 `com.claudej.domain.cart.model.aggregate.Cart` — 聚合根
  - 新增 `com.claudej.domain.cart.model.entity.CartItem` — 实体
  - 新增 `com.claudej.domain.cart.model.valobj.Quantity` — 值对象
  - 新增 `com.claudej.domain.cart.model.valobj.Money` — 金额值对象（聚合内独立）
  - 新增 `com.claudej.domain.cart.repository.CartRepository` — Repository 端口
  - 修改 `com.claudej.domain.common.exception.ErrorCode` — 新增 CART 相关错误码
- **application**:
  - 新增 `com.claudej.application.cart.command.AddCartItemCommand`
  - 新增 `com.claudej.application.cart.command.UpdateCartItemQuantityCommand`
  - 新增 `com.claudej.application.cart.dto.CartDTO`
  - 新增 `com.claudej.application.cart.dto.CartItemDTO`
  - 新增 `com.claudej.application.cart.assembler.CartAssembler`
  - 新增 `com.claudej.application.cart.service.CartApplicationService`
- **infrastructure**:
  - 新增 `com.claudej.infrastructure.cart.persistence.dataobject.CartDO`
  - 新增 `com.claudej.infrastructure.cart.persistence.dataobject.CartItemDO`
  - 新增 `com.claudej.infrastructure.cart.persistence.mapper.CartMapper`
  - 新增 `com.claudej.infrastructure.cart.persistence.mapper.CartItemMapper`
  - 新增 `com.claudej.infrastructure.cart.persistence.converter.CartConverter`
  - 新增 `com.claudej.infrastructure.cart.persistence.repository.CartRepositoryImpl`
- **adapter**:
  - 新增 `com.claudej.adapter.cart.web.CartController`
  - 新增 `com.claudej.adapter.cart.web.request.AddCartItemRequest`
  - 新增 `com.claudej.adapter.cart.web.request.UpdateCartItemQuantityRequest`
  - 新增 `com.claudej.adapter.cart.web.response.CartResponse`
  - 新增 `com.claudej.adapter.cart.web.response.CartItemResponse`
- **start**:
  - 修改 `schema.sql` — 新增 t_cart 和 t_cart_item 表 DDL
