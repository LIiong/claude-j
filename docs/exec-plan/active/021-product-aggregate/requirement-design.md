# 需求拆分设计 — 021-product-aggregate

## 需求描述

新建 Product 聚合，实现 SKU / 定价 / 上下架状态机的完整生命周期管理。Product 聚合根封装商品核心业务不变量，与 OrderItem 通过 productId 解耦（OrderItem 仅持 productId + 下单快照价）。

## 领域分析

### 聚合根: Product

- productId (ProductId) — 商品唯一标识（值对象）
- name (ProductName) — 商品名称（值对象）
- description (String) — 商品描述
- sku (SKU) — SKU 信息（值对象，嵌入聚合）
- originalPrice (Money) — 原价（值对象）
- promotionalPrice (Money) — 促销价（值对象，可为空）
- status (ProductStatus) — 商品状态（枚举，封装状态机）
- createTime (LocalDateTime) — 创建时间
- updateTime (LocalDateTime) — 更新时间

### 值对象

- **ProductId**: 商品唯一标识，UUID 格式，不可变
- **ProductName**: 商品名称，长度 2-100 字符，不可变
- **SKU**: SKU 信息（skuCode + stock），嵌入聚合，不可变
  - skuCode: String，SKU 编码，非空，最长 32 字符
  - stock: int，库存数量，>= 0

### 状态枚举: ProductStatus

- **DRAFT** — 草稿（初始状态）
- **ACTIVE** — 已上架（可销售）
- **INACTIVE** — 已下架（不可销售）

**状态转换规则**：
```
DRAFT ──activate()──> ACTIVE
ACTIVE ──deactivate()──> INACTIVE
INACTIVE ──activate()──> ACTIVE
DRAFT 不能直接转到 INACTIVE（必须先上架再下架）
```

### 端口接口

- **ProductRepository**:
  - `save(Product product)` — 保存商品
  - `findById(Long id)` — 按 ID 查询
  - `findByProductId(ProductId productId)` — 按业务 ID 查询
  - `findByStatus(ProductStatus status)` — 按状态查询（列表）
  - `findAll(PageRequest pageRequest)` — 分页查询

## 关键算法/技术方案

### SKU 嵌入 Product（而非独立实体）

**决策理由**：
1. SKU 与 Product 是紧密耦合关系 — 一个 SKU 属于唯一 Product
2. 简化模型 — 不引入 SKU 实体和 SKU Repository
3. 符合 DDD 聚合设计原则 — SKU 是 Product 的内部属性
4. OrderItem 已有 productId + snapshotPrice，SKU 信息不在 Order 聚合中

**备选方案**（排除）：
- SKU 作为独立实体 + skuId 关联 — 过度设计，增加跨聚合复杂度

### 定价设计（原价 + 促销价）

- **originalPrice** — 必填，商品原价
- **promotionalPrice** — 可选，促销价（可为 null）
- **getEffectivePrice()** — 返回有效售价（促销价优先，无促销价返回原价）

### 状态机实现

参考 CouponStatus 模式，状态转换方法封装在枚举中：
- `canActivate()` / `canDeactivate()`
- `toActive()` / `toInactive()` — 转换时校验并抛 BusinessException

## API 设计

| 方法 | 路径 | 描述 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| POST | /api/v1/products | 创建商品 | `{ "name": "...", "skuCode": "...", "stock": 100, "originalPrice": 99.00, "promotionalPrice": 79.00, "description": "..." }` | `{ "success": true, "data": { "productId": "...", ... } }` |
| GET | /api/v1/products/{productId} | 查询商品 | — | `{ "success": true, "data": { "productId": "...", "name": "...", "status": "ACTIVE", ... } }` |
| PUT | /api/v1/products/{productId}/price | 调价 | `{ "originalPrice": 129.00, "promotionalPrice": null }` | `{ "success": true, "data": { ... } }` |
| PUT | /api/v1/products/{productId}/activate | 上架 | — | `{ "success": true, "data": { "status": "ACTIVE" } }` |
| PUT | /api/v1/products/{productId}/deactivate | 下架 | — | `{ "success": true, "data": { "status": "INACTIVE" } }` |
| GET | /api/v1/products | 分页查询 | `?status=ACTIVE&page=0&size=10` | `{ "success": true, "data": { "content": [...], "totalElements": 100, ... } }` |

## 数据库设计

```sql
CREATE TABLE IF NOT EXISTS t_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(64) NOT NULL COMMENT '商品唯一业务标识',
    name VARCHAR(100) NOT NULL COMMENT '商品名称',
    description VARCHAR(500) NULL COMMENT '商品描述',
    sku_code VARCHAR(32) NOT NULL COMMENT 'SKU编码',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    original_price DECIMAL(19, 2) NOT NULL COMMENT '原价',
    promotional_price DECIMAL(19, 2) NULL COMMENT '促销价',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/ACTIVE/INACTIVE',
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    UNIQUE KEY uk_product_id (product_id),
    KEY idx_status (status),
    KEY idx_sku_code (sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';
```

## 与 Order.OrderItem 的关系

**解耦设计**：
- OrderItem 持有 `productId` (String) + `snapshotPrice` (Money)
- OrderItem 不持有 Product 引用，不关心 Product 当前状态
- Order 创建时，snapshotPrice 来自 Product 的 `getEffectivePrice()`
- Product 状态变更不影响已创建的 OrderItem

**边界**：
- Product 聚合不依赖 Order 聚合
- Order 聚合通过 Application 层服务查询 Product（不直接引用）

## 影响范围

- **domain**: Product 聚合根 + ProductId/ProductName/SKU 值对象 + ProductStatus 枚举 + ProductRepository 端口
- **application**: CreateProductCommand/UpdatePriceCommand/ActivateProductCommand/DeactivateProductCommand + ProductDTO + ProductAssembler + ProductApplicationService
- **infrastructure**: ProductDO + ProductMapper + ProductConverter + ProductRepositoryImpl
- **adapter**: ProductController + CreateProductRequest/UpdatePriceRequest/ProductResponse
- **start**: V8__product_init.sql（Flyway 迁移）

## 假设与待确认

1. **SKU 单一设计**：假设一个 Product 只有一个 SKU。若需多 SKU（如颜色/尺寸变体），需重新设计 SKU 为实体 + SKU Repository。
2. **促销价优先**：假设有效售价 = promotionalPrice（如有）否则 originalPrice。
3. **库存扣减**：本任务不涉及库存扣减（下单扣库存），后续任务处理。
4. **价格范围**：假设价格 >= 0，无上限校验。

## 验收条件

1. Product 聚合根封装状态机（DRAFT → ACTIVE → INACTIVE）
2. 价格调整仅允许 DRAFT 状态（上架后不可调价？需确认）
3. 上架/下架操作遵循状态转换规则
4. Repository 保存/查询往返正确
5. REST API 契约符合设计
6. JaCoCo 阈值：domain 90% / application 80%