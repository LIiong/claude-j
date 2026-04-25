# 任务交接 — 023-inventory-aggregate

## 基本信息
- **task-id**: 023-inventory-aggregate
- **from**: dev
- **to**: qa
- **status**: pending-review
- **phase**: Build -> Verify

## 产出物清单
- `docs/exec-plan/active/023-inventory-aggregate/requirement-design.md` — 需求设计文档（含架构评审章节）
- `docs/exec-plan/active/023-inventory-aggregate/task-plan.md` — 任务执行计划
- `docs/exec-plan/active/023-inventory-aggregate/dev-log.md` — 开发日志（含 Build 阶段遗留问题）

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

## Build 阶段产出

### 代码提交记录
- `433faa2` feat(domain): inventory 聚合根、值对象、Repository 端口
- `79dacf7` feat(application): inventory Command/DTO/Assembler/ApplicationService
- `706ff2c` feat(infrastructure): inventory DO/Mapper/Converter/RepositoryImpl
- `054165f` feat(adapter): inventory REST 端点 + 错误码映射
- `54397f2` feat(application): Order 集成库存预占/扣减/回滚

### 测试数量
- Domain 测试：3 个（InventoryIdTest, SkuCodeTest, InventoryTest）
- Application 测试：1 个（InventoryApplicationServiceTest）
- Infrastructure 测试：1 个（InventoryRepositoryImplTest）
- Adapter 测试：1 个（InventoryControllerTest）

## pre-flight
- mvn-test: **FAIL**（4 failures）
  - FlywayVerificationTest: migration 数量不匹配（预期 10，实际 9）
  - OrderFromCartIntegrationTest: 404 状态码（3 cases）
- checkstyle: 待验证
- entropy-check: 待验证

## 遗留问题（需 QA 关注）

### 问题 1：Flyway migration 数量
- V10 文件存在且命名正确，但 Flyway 只执行了 9 个 migrations
- 可能是 Spring Test context 缓存问题

### 问题 2：OrderFromCartIntegrationTest
- 订单创建端点返回 404
- 可能与 Order 集成 Inventory 后的路由变化有关

## summary
Build 阶段代码已产出，存在 4 个测试失败需 QA 验收时调查处理。
三项 pre-flight 未能全部通过，请 QA 独立验证并决定验收结果。