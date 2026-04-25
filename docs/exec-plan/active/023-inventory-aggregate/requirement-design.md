# 需求拆分设计 — 023-inventory-aggregate

## 需求描述
新建 inventory 聚合，实现库存预占/扣减/回滚机制，与订单聚合集成：创建订单时预占库存、支付成功时扣减库存、取消订单时回滚库存。

## 领域分析

### 聚合根: Inventory
- id (Long) — 数据库自增主键
- inventoryId (InventoryId) — 库存唯一业务标识
- productId (ProductId) — 关联商品ID
- skuCode (SkuCode) — SKU编码
- availableStock (int) — 可用库存数
- reservedStock (int) — 预占库存数
- createTime (LocalDateTime) — 创建时间
- updateTime (LocalDateTime) — 更新时间

### 值对象
- **InventoryId**: 库存唯一标识，非空字符串，不可变
- **SkuCode**: SKU编码，长度不超过32，不可变
- **ProductId**: 已有值对象（引用 product 模块）

### 领域服务（如有）
无需独立领域服务，库存操作由聚合根封装。

### 端口接口
- **InventoryRepository**:
  - `Inventory save(Inventory inventory)` — 保存库存
  - `Optional<Inventory> findByInventoryId(InventoryId inventoryId)` — 按业务ID查询
  - `Optional<Inventory> findByProductId(String productId)` — 按商品ID查询
  - `Optional<Inventory> findBySkuCode(SkuCode skuCode)` — 按SKU查询
  - `boolean existsByInventoryId(InventoryId inventoryId)` — 存在性检查

## 关键算法/技术方案

### 库存预占机制
采用「预占库存」模式而非直接扣减：
- **预占(reserve)**：创建订单时，`reservedStock += quantity`，`availableStock -= quantity`
- **扣减(deduct)**：支付成功时，`reservedStock -= quantity`（预占转为实际扣减）
- **回滚(release)**：订单取消时，`reservedStock -= quantity`，`availableStock += quantity`

### 不变量校验
```
availableStock >= 0  // 可用库存不能为负
reservedStock >= 0   // 预占库存不能为负
availableStock - reservedStock >= 0  // 实际可用 = 可用 - 预占
预占数量 <= availableStock  // 预占不能超过当前可用
```

### 与 Order 集成方案
在 OrderApplicationService 中注入 InventoryRepository：
- **创建订单前**：先预占库存，预占失败则订单创建失败
- **支付成功后**：扣减库存（预占转扣减）
- **取消订单时**：回滚库存（释放预占）

### 并发安全考量
当前版本暂不引入分布式锁/乐观锁，依赖数据库事务：
- 单体应用 + H2/MySQL，单事务内「订单+库存」操作
- 后续演进可引入版本号字段（version）做乐观锁

## API 设计

| 方法 | 路径 | 描述 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| POST | /api/v1/inventory | 创建库存记录 | `{ "productId": "xxx", "skuCode": "SKU001", "stock": 100 }` | `{ "inventoryId": "xxx", ... }` |
| GET | /api/v1/inventory/{inventoryId} | 查询库存详情 | — | `{ "inventoryId": "xxx", "availableStock": 80, "reservedStock": 20 }` |
| GET | /api/v1/inventory/product/{productId} | 按商品ID查询库存 | — | `{ ... }` |
| GET | /api/v1/inventory/sku/{skuCode} | 按SKU查询库存 | — | `{ ... }` |
| POST | /api/v1/inventory/{inventoryId}/adjust | 管理员调整库存 | `{ "adjustment": 50, "reason": "入库" }` | `{ ... }` |

## 数据库设计

```sql
-- V10__add_inventory.sql
CREATE TABLE IF NOT EXISTS t_inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inventory_id VARCHAR(64) NOT NULL COMMENT '库存唯一业务标识',
    product_id VARCHAR(64) NOT NULL COMMENT '关联商品ID',
    sku_code VARCHAR(32) NOT NULL COMMENT 'SKU编码',
    available_stock INT NOT NULL DEFAULT 0 COMMENT '可用库存数',
    reserved_stock INT NOT NULL DEFAULT 0 COMMENT '预占库存数',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    UNIQUE KEY uk_inventory_id (inventory_id),
    KEY idx_product_id (product_id),
    KEY idx_sku_code (sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';
```

## 影响范围
- **domain**:
  - 新增 `Inventory` 聚合根
  - 新增 `InventoryId` 值对象
  - 新增 `SkuCode` 值对象
  - 新增 `InventoryRepository` 端口接口
  - 新增 ErrorCode: `INVENTORY_NOT_FOUND`, `INVENTORY_INSUFFICIENT`, `INVENTORY_ID_EMPTY`, `SKU_CODE_EMPTY`, `SKU_CODE_TOO_LONG`, `STOCK_NEGATIVE`, `RESERVE_NEGATIVE`
- **application**:
  - 新增 `InventoryApplicationService`
  - 新增 `InventoryDTO`, `InventoryAssembler`
  - 新增 `CreateInventoryCommand`, `AdjustStockCommand`, `ReserveStockCommand`, `DeductStockCommand`, `ReleaseStockCommand`
  - **修改** `OrderApplicationService`（集成库存预占/扣减/回滚）
- **infrastructure**:
  - 新增 `InventoryDO`, `InventoryMapper`, `InventoryConverter`, `InventoryRepositoryImpl`
- **adapter**:
  - 新增 `InventoryController`, `InventoryRequest`, `InventoryResponse`
- **start**:
  - 新增 `V10__add_inventory.sql` DDL

## 假设与待确认

### 已确认假设
1. 库存与商品的关系：一个商品对应一个库存记录（当前需求）
2. SKU 编码唯一性：全局唯一，不同商品不能有相同 SKU
3. 库存预占时机：订单创建后立即预占（而非下单确认后）

### 待确认项（需 @architect 评审）
1. **并发安全策略**：当前版本暂不加乐观锁，是否可接受？
2. **库存初始化时机**：由管理员手动创建库存记录，还是商品上架时自动初始化？
3. **库存不足时的订单处理**：预占失败 = 订单创建失败，还是允许「缺货下单」？
4. **库存调整日志**：是否需要单独的库存变更流水表？

## 验收条件
1. 库存预占成功后可用库存减少、预占库存增加
2. 库存不足时预占失败，抛出 `BusinessException(ErrorCode.INVENTORY_INSUFFICIENT)`
3. 支付成功后预占转为扣减（`reservedStock -= quantity`）
4. 订单取消后预占库存回滚（`reservedStock -= quantity`, `availableStock += quantity`）
5. 三项预飞（`mvn test` / `checkstyle` / `entropy-check`）全过
6. JaCoCo 阈值不下滑（domain 90% / application 80%）