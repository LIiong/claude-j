# 需求拆分设计 — 023-inventory-aggregate

## 需求描述
新建 inventory 聚合，实现库存预占/扣减/回滚机制，与订单聚合集成：创建订单时预占库存、支付成功时扣减库存、取消订单时回滚库存。

## 领域分析

### 聚合根: Inventory
- id (Long) — 数据库自增主键
- inventoryId (InventoryId) — 库存唯一业务标识
- productId (String) — 关联商品ID（跨聚合弱引用，不依赖 Product 聚合）
- skuCode (SkuCode) — SKU编码
- availableStock (int) — 可用库存数
- reservedStock (int) — 预占库存数
- createTime (LocalDateTime) — 创建时间
- updateTime (LocalDateTime) — 更新时间

### 值对象
- **InventoryId**: 库存唯一标识，非空字符串，不可变
- **SkuCode**: SKU编码，长度不超过32，不可变

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

---

## 架构评审

**评审人**：@architect
**日期**：2026-04-25
**结论**：✅ 通过

### 评审检查项（15 维四类）

**架构合规（7 项）**
- [x] 聚合根边界合理（遵循事务一致性原则）— Inventory 作为独立聚合，封装预占/扣减/回滚不变量，事务边界清晰
- [x] 值对象识别充分（金额、标识符等应为 VO）— InventoryId、SkuCode 均为值对象；productId 使用 String 类型（跨聚合弱引用，符合 OrderItem 现有模式）
- [x] Repository 端口粒度合适（方法不多不少）— 5 个方法覆盖核心场景：save/findByInventoryId/findByProductId/findBySkuCode/existsByInventoryId
- [x] 与已有聚合无循环依赖 — Inventory 仅依赖 common 模块（ErrorCode），不依赖 Product/Order 聚合；OrderApplicationService 将注入 InventoryRepository（单向依赖）
- [x] DDL 设计与领域模型一致（字段映射、索引合理）— t_inventory 字段与 Inventory 聚合根一一对应；uk_inventory_id/idx_product_id/idx_sku_code 索引覆盖查询场景
- [x] API 设计符合 RESTful 规范 — POST 创建 / GET 查询 / POST adjust 管理员调库，路径命名符合项目风格（/api/v1/inventory）
- [x] 对象转换链正确（DO ↔ Domain ↔ DTO ↔ Request/Response）— InventoryDO ↔ Inventory ↔ InventoryDTO ↔ InventoryRequest/InventoryResponse，四层转换链完整

**需求质量（3 项）**
- [x] 需求无歧义：核心名词、流程、异常分支均有明确定义 — 预占/扣减/回滚流程清晰；库存不足抛 INVENTORY_INSUFFICIENT；管理员调库有 reason 字段
- [x] 验收条件可验证：每条 AC 可转化为 `should_xxx_when_yyy` 测试用例 — 6 条验收条件均可转化为具体测试方法
- [x] 业务规则完备：状态机/不变量/边界值在需求中已列明 — 不变量公式明确（availableStock >= 0, reservedStock >= 0, 预占 <= 可用）

**计划可执行性（2 项）**
- [x] task-plan 粒度合格：按层任务已分解到原子级（10–15 分钟/步），每步含文件路径 + 验证命令 + 预期输出 — 17 项任务覆盖 domain/application/infrastructure/adapter/start 五层，每项含骨架代码 + 验证命令 + commit 消息
- [x] 依赖顺序正确：domain → application → infrastructure → adapter → start 自下而上，层间依赖无倒置 — task-plan 执行顺序正确

**可测性保障（3 项 — 010 复盘后新增）**
- [x] **AC 自动化全覆盖**：验收条件均可转化为自动化测试，无标「手动」项
- [x] **可测的注入方式**：OrderApplicationService 现有代码已使用构造函数注入；InventoryApplicationService 将沿用此模式
- [x] **配置校验方式合规**：本任务不涉及敏感配置校验，无需引入 @ConfigurationProperties

**心智原则（Karpathy — 动手前自检）**
- [x] **简洁性**：需求未要求的抽象/配置/工厂已移除 — 无 XxxStrategy/XxxFactory；InventoryRepository 端口接口仅 5 方法，无过度抽象
- [x] **外科性**：设计仅改动任务直接相关的文件 — 新增 inventory 聚合 + 修改 OrderApplicationService（集成库存），不涉及其他聚合重构
- [x] **假设显性**：需求里含糊的字段/边界/异常，requirement-design 已在「假设与待确认」列出 — 4 项待确认已全部评审并给出结论

### 评审意见

#### 1. 并发安全策略（待确认项 #1）
**结论**：可接受。当前版本依赖数据库事务（单体应用 + H2/MySQL）。

**建议**：后续演进时考虑以下优化：
- 在 InventoryDO 增加 `version` 字段做乐观锁（@Version 注解）
- 对于高并发场景，引入 Redis 分布式锁或数据库行锁（SELECT FOR UPDATE）

**决策**：当前设计符合项目现状，暂不需要乐观锁。后续需做 ADR 记录演进方向。

#### 2. 库存初始化时机（待确认项 #2）
**结论**：管理员手动创建库存记录。

**理由**：
- 商品上架与库存初始化是两个独立的业务动作
- 部分商品可能上架但不设库存（预售/虚拟商品）
- 管理员手动创建更灵活，可指定初始库存数量

**设计补充**：在 InventoryController 提供 POST /api/v1/inventory 端点，管理员通过 API 创建库存记录。

#### 3. 库存不足时订单处理（待确认项 #3）
**结论**：预占失败 = 订单创建失败。

**理由**：
- 电商标准做法，避免「缺货下单」导致的用户体验问题
- 符合「事务一致性」原则——订单创建与库存预占在同一事务内

**实现细节**：OrderApplicationService.createOrder() 先预占库存，预占失败抛 INVENTORY_INSUFFICIENT，订单不创建。

#### 4. 库存变更流水表（待确认项 #4）
**结论**：当前版本暂不增加。

**理由**：
- 符合 Karpathy 原则②（简洁优先）——需求未要求
- 现有 updateTime + deleted 字段可追溯基本变更

**建议**：后续版本如需完整审计链路，可新增 t_inventory_log 表记录变更流水。

#### 5. 设计修正
**问题**：原设计文档 `productId (ProductId)` 声明引用已有值对象。

**修正**：改为 `productId (String)` — 跨聚合弱引用，不依赖 Product 聚合。

**依据**：
- OrderItem 使用 `String productId` 而非 ProductId 值对象（现有模式）
- ADR-003 原则：跨聚合使用值对象 ID 做弱关联，但 String 类型更解耦
- Inventory 聚合应独立，不依赖 Product 聚合的任何类

#### 6. task-plan 验证命令建议
部分任务骨架代码需补充 import 声明（如 `BusinessException`、`ErrorCode`），但这是 Build 阶段细节，不影响评审通过。

### 架构基线检查
```bash
./scripts/entropy-check.sh
# 结果：PASS（0 FAIL，12 WARN）
# WARN 项目主要为测试缺失（auth 聚合）和 ADR 格式问题，与本任务无关
```

### 需要新增的 ADR
无。当前设计决策已在评审意见中记录，后续演进时再创建 ADR（如乐观锁策略、库存流水表）。

---

**评审通过，可进入 Build 阶段。**