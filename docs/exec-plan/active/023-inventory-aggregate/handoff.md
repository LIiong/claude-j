# 任务交接 — 023-inventory-aggregate

## 基本信息
- **task-id**: 023-inventory-aggregate
- **from**: architect
- **to**: dev
- **status**: approved
- **phase**: Review -> Build

## 产出物清单
- `docs/exec-plan/active/023-inventory-aggregate/requirement-design.md` — 需求设计文档（含架构评审章节）
- `docs/exec-plan/active/023-inventory-aggregate/task-plan.md` — 任务执行计划
- `docs/exec-plan/active/023-inventory-aggregate/dev-log.md` — 开发日志（Spec 阶段无问题记录）

## 设计摘要
### 领域模型
- **聚合根**: Inventory（库存）
  - 属性：inventoryId, productId(String), skuCode, availableStock, reservedStock
  - 方法：reserve(), deduct(), release()

- **值对象**: InventoryId, SkuCode

- **不变量**:
  - availableStock >= 0
  - reservedStock >= 0
  - 预占数量 <= availableStock

### 与 Order 集成
- Order 创建 -> Inventory.reserve()
- Order 支付 -> Inventory.deduct()
- Order 取消 -> Inventory.release()

### API 设计
- POST /api/v1/inventory — 创建库存
- GET /api/v1/inventory/{id} — 查询库存
- GET /api/v1/inventory/product/{productId} — 按商品查询
- POST /api/v1/inventory/{id}/adjust — 管理员调库

### DDL
- V10__add_inventory.sql（t_inventory 表）

## 评审结论
**通过**

### 待确认项裁决
1. **并发安全策略**：暂不加乐观锁，依赖数据库事务（后续演进考虑 version 字段）
2. **库存初始化时机**：管理员手动创建库存记录（通过 API）
3. **库存不足时订单处理**：预占失败 = 订单创建失败（符合电商标准做法）
4. **库存变更流水表**：暂不增加（后续版本可新增 t_inventory_log）

### 设计修正
- `productId` 类型从 `ProductId` 值对象改为 `String`（跨聚合弱引用，不依赖 Product 聚合）

## pre-flight
- entropy-check: PASS（退出码 0，0 FAIL / 12 WARN）

## summary
架构评审通过。设计符合六边形架构、DDD 聚合原则、Java 8 约束。
待 @dev 进入 Build 阶段，按 task-plan 执行 TDD 开发。