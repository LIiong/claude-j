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

## 架构评审

**评审人**：@architect
**日期**：2026-04-14
**结论**：✅ 通过

### 评审检查项

**聚合设计**：
- [x] 聚合根边界合理（遵循事务一致性原则，一个事务一个聚合）
- [x] 聚合根封装所有业务不变量（非贫血模型）
- [x] 状态变更仅通过聚合根方法（无公开 setter）

**值对象识别**：
- [x] 金额、标识符、状态等应为值对象
- [x] 值对象不可变（final 字段、equals/hashCode）
- [x] 约束条件在构造函数中校验

**端口设计**：
- [x] Repository 端口在 domain 层（接口）
- [x] 方法粒度合适（不多不少）
- [x] 返回 Domain 对象，不返回 DO

**依赖方向**：
- [x] adapter → application → domain ← infrastructure
- [x] 与已有聚合无循环依赖
- [x] 对象转换链正确（DO ↔ Domain ↔ DTO ↔ Request/Response）

**DDL 设计**：
- [x] 表名 `t_{entity}`，列名 snake_case
- [x] 索引策略合理
- [x] 与领域模型字段一致

**API 设计**：
- [x] RESTful 规范
- [x] 路径命名一致（`/api/v1/{resource}`）
- [x] 响应格式 `ApiResult<T>`

**ADR 需求**：
- [x] 无需要记录的重大架构决策（Money 值对象复用模式已在 order 聚合中确立）

### 评审意见

**设计亮点**：
1. **聚合边界清晰**：Cart 作为聚合根，CartItem 作为聚合内实体，符合事务一致性原则
2. **值对象设计合理**：Quantity 值对象封装数量约束（1~999），Money 值对象保证金额精度
3. **存储策略正确**：采用 t_cart + t_cart_item 两张表，与 order 聚合保持一致的持久化模式
4. **Money 值对象策略**：为避免跨聚合引用，选择聚合内独立 Money 值对象，与 order 的 Money 保持相同逻辑但物理隔离，符合 DDD 最佳实践
5. **API 设计规范**：RESTful 路径设计合理，使用标准 HTTP 方法（POST/PUT/DELETE/GET）

**建议事项**（非阻塞）：
1. **清空购物车接口**：DELETE /api/v1/carts 使用 DELETE 方法符合 RESTful 规范，语义清晰
2. **商品数量上限 999**：合理范围，可根据业务需求后续调整
3. **索引优化**：uk_user_id 和 uk_cart_product 联合唯一索引设计合理

**架构基线检查**：
- 运行 `./scripts/entropy-check.sh` 通过（0 errors, 2 warnings）
- 与已有 order 聚合代码模式一致
- 符合六边形架构依赖方向

### 需要新增的 ADR
无。Money 值对象复用模式已在 order 聚合中确立，无需新增 ADR。

---
评审结论：**设计通过，可以进入 Build 阶段。**
