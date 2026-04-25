# 任务交接 — 023-inventory-aggregate

## 基本信息
- **task-id**: 023-inventory-aggregate
- **from**: dev
- **to**: architect
- **status**: pending-review
- **phase**: Spec

## 产出物清单
- `docs/exec-plan/active/023-inventory-aggregate/requirement-design.md` — 需求设计文档
- `docs/exec-plan/active/023-inventory-aggregate/task-plan.md` — 任务执行计划
- `docs/exec-plan/active/023-inventory-aggregate/dev-log.md` — 开发日志（Spec 阶段无问题记录）

## 设计摘要
### 领域模型
- **聚合根**: Inventory（库存）
  - 属性：inventoryId, productId, skuCode, availableStock, reservedStock
  - 方法：reserve(), deduct(), release()

- **值对象**: InventoryId, SkuCode

- **不变量**:
  - availableStock >= 0
  - reservedStock >= 0
  - 预占数量 <= availableStock

### 与 Order 集成
- Order 创建 → Inventory.reserve()
- Order 支付 → Inventory.deduct()
- Order 取消 → Inventory.release()

### API 设计
- POST /api/v1/inventory — 创建库存
- GET /api/v1/inventory/{id} — 查询库存
- GET /api/v1/inventory/product/{productId} — 按商品查询
- POST /api/v1/inventory/{id}/adjust — 管理员调库

### DDL
- V10__add_inventory.sql（t_inventory 表）

## 待评审项
1. **并发安全策略**：当前版本暂不加乐观锁，是否可接受？
2. **库存初始化时机**：管理员手动创建，还是商品上架自动初始化？
3. **库存不足时订单处理**：预占失败 = 订单创建失败，是否正确？
4. **库存变更流水表**：是否需要？

## pre-flight
<!-- Spec 阶段无代码产出，三项预飞待 Build 阶段填写 -->
- mvn-test: 待执行（Build 阶段）
- checkstyle: 待执行（Build 阶段）
- entropy-check: 待执行（Build 阶段）

## summary
Spec 阶段完成：需求设计文档、任务执行计划、领域模型设计已产出。
待 @architect 评审后进入 Build 阶段。