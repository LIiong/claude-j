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

---

## 架构评审

**评审人**：@architect
**日期**：2026-04-25
**结论**：❌ 待修改

### 评审检查项（15 维四类）

**架构合规（7 项）**
- [x] 聚合根边界合理（遵循事务一致性原则）— Product 聚合根封装 SKU 值对象，符合 DDD 原则；SKU 不独立实体化简化模型
- [x] 值对象识别充分（金额、标识符等应为 VO）— ProductId/ProductName/SKU/Money 值对象识别充分；SKU 包含 skuCode + stock，嵌入聚合
- [x] Repository 端口粒度合适（方法不多不少）— 5 个方法（save/findById/findByProductId/findByStatus/findAll）粒度合适
- [x] 与已有聚合无循环依赖 — Product 聚合不依赖 Order；OrderItem 仅持 productId + snapshotPrice，通过 Application 层查询 Product
- [x] DDL 设计与领域模型一致（字段映射、索引合理）— 表名 t_product、列名 snake_case、唯一索引 uk_product_id、索引 idx_status/idx_sku_code 合理
- [x] API 设计符合 RESTful 规范 — POST/GET/PUT 端点符合规范；activate/deactivate 使用 PUT 而非 POST，符合语义
- [x] 对象转换链正确（DO ↔ Domain ↔ DTO ↔ Request/Response）— 转换链完整，MapStruct 转换器设计合理

**需求质量（3 项）**
- [x] 需求无歧义：核心名词、流程、异常分支均有明确定义 — 领域模型定义清晰，状态转换规则完整
- [ ] 验收条件可验证：每条 AC 可转化为 `should_xxx_when_yyy` 测试用例 — **AC-2 含歧义**：「上架后不可调价？需确认」未明确决策
- [ ] 业务规则完备：状态机/不变量/边界值在需求中已列明 — **定价规则边界缺失**：AC-2 未明确上架后是否允许调价；假设「价格 >= 0，无上限校验」未纳入 AC

**计划可执行性（2 项）**
- [x] task-plan 粒度合格：按层任务已分解到原子级（10–15 分钟/步），每步含文件路径 + 验证命令 + 预期输出 — 14 个原子任务，含骨架/验证命令/预期输出/commit 消息
- [x] 依赖顺序正确：domain → application → infrastructure → adapter → start 自下而上，层间依赖无倒置

**可测性保障（3 项 — 010 复盘后新增）**
- [ ] **AC 自动化全覆盖**：`test-case-design.md` 的「AC 自动化覆盖矩阵」每条 AC 都有对应自动化测试方法；任一标「手动」但无替代自动化测试 → **打回** — **test-case-design.md 不存在**（Spec 阶段未产出）
- [x] **可测的注入方式**：若引入新 Spring Bean，使用构造函数注入而非字段注入 — task-plan 已明确使用 `@ExtendWith(MockitoExtension.class)` + Mock Repository
- [x] **配置校验方式合规**：若涉及敏感/跨环境配置校验，使用 `@ConfigurationProperties + @Validated` — 本任务不涉及敏感配置校验，无违规

**心智原则（Karpathy — 动手前自检）**
- [x] **简洁性**：需求未要求的抽象/配置/工厂已移除 — SKU 嵌入 Product（不独立实体化）符合简洁原则；无过度抽象
- [x] **外科性**：设计仅改动任务直接相关的文件 — 新增 Product 聚合，不改动已有聚合；ErrorCode 扩展最小化
- [x] **假设显性**：需求里含糊的字段/边界/异常，requirement-design 已在「假设与待确认」列出 — SKU 单一设计/上架后调价/库存扣减/价格范围均已列出

### 评审意见

#### 必须修改（阻塞通过）

**1. AC-2 定价规则歧义必须明确**
- 当前 AC-2 写法：「价格调整仅允许 DRAFT 状态（上架后不可调价？需确认）」含歧义标记「？需确认」
- **要求**：必须在此 AC 中明确决策并移除歧义标记：
  - 若上架后禁止调价 → AC 改为「价格调整仅允许 DRAFT 状态」
  - 若上架后允许调价 → AC 改为「价格调整允许 DRAFT/ACTIVE/INACTIVE 状态」并补充调价触发重新定价的业务规则
- 参考已有聚合：Coupon 聚合的「懒过期」策略在 design 中明确定义，不留歧义

**2. 补充 test-case-design.md**
- 当前缺失测试设计文档，违反可测性检查项「AC 自动化全覆盖」
- **要求**：@dev 在 Build 阳段前补充 `test-case-design.md`，包含：
  - AC 自动化覆盖矩阵（6 条 AC 对应测试方法）
  - Domain 层测试用例（ProductId/ProductName/SKU/ProductStatus/Product 聚合根）
  - Application 层测试用例（Mock Repository 编排验证）
  - Infrastructure 层集成测试（H2 往返）
  - Adapter 层契约测试（MockMvc HTTP 状态码）
- 参考 `docs/exec-plan/templates/test-case-design.template.md`

#### 建议改进（不阻塞通过）

**1. SKU 单一设计假设需用户确认**
- 「假设与待确认」#1：「若需多 SKU（如颜色/尺寸变体），需重新设计 SKU 为实体 + SKU Repository」
- 建议在正式开工前向用户确认：当前业务场景是否需要多 SKU 变体支持
- 若确认单一 SKU，可在 requirement-design.md 「关键算法/技术方案」章节补充决策理由（已列出，符合 Karpathy #1）

**2. 价格范围校验补充到 AC**
- 当前假设：「价格 >= 0，无上限校验」
- 建议将此边界值规则补充到验收条件：「原价/促销价 >= 0，无上限校验」

**3. 状态机参考 CouponStatus 模式**
- 设计已参考 CouponStatus 模式（canActivate/canDeactivate + toActive/toInactive），符合已有代码风格
- 确认状态转换异常码命名规范：`INVALID_PRODUCT_STATUS_TRANSITION`（参考 `INVALID_COUPON_STATUS_TRANSITION`）

### entropy-check.sh 基线证据

```bash
$ ./scripts/entropy-check.sh
============================================
  claude-j 熵检测 (Entropy Check)
============================================
--- [1/13] Domain 层纯净性 ---
PASS: domain 层零 Spring import
PASS: domain 层零 MyBatis-Plus import
...
--- [检查完成] ---
错误 (FAIL): 0
警告 (WARN): 12
status: pass (exit code 0)
```

### 需要新增的 ADR

无。SKU 嵌入设计已在 requirement-design.md「关键算法/技术方案」章节记录，符合「决策显性化」原则，无需单独 ADR。

### 下一步行动

1. @dev 修改 requirement-design.md AC-2，明确定价规则决策
2. @dev 补充 test-case-design.md（AC 覆盖矩阵 + 分层测试用例）
3. 修改完成后重新提交评审

---

**评审状态**：changes-requested
**修改项**：2 项必须修改（AC-2 歧义 + test-case-design.md 补充）