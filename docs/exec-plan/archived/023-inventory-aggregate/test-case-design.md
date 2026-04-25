# 测试用例设计 — 023-inventory-aggregate

## 测试范围
Inventory 聚合新增及 Order 集成库存预占/扣减/回滚机制的验收测试。

## 测试策略
按测试金字塔分层测试 + 代码审查 + 风格检查。

---

## 0. AC 自动化覆盖矩阵（强制，动笔前先填）

| # | 验收条件（AC） | 自动化层 | 对应测试方法 | 手动备注 |
|---|---|---|---|---|
| AC1 | 库存预占成功后可用库存减少、预占库存增加 | Domain | `InventoryTest.should_reserve_when_stockAvailable` | - |
| AC2 | 库存不足时预占失败，抛出 INVENTORY_INSUFFICIENT | Domain | `InventoryTest.should_throw_when_reserve_exceedsAvailable` | - |
| AC3 | 支付成功后预占转为扣减（reservedStock -= quantity） | Domain | `InventoryTest.should_deduct_when_reservedEnough` | - |
| AC4 | 订单取消后预占库存回滚（reservedStock -= quantity, availableStock += quantity） | Domain | `InventoryTest.should_release_when_reservedEnough` | - |
| AC5 | 三项预飞全过 | Start | `mvn test` + `mvn checkstyle:check` + `entropy-check.sh` | ❌ **FAIL** |
| AC6 | JaCoCo 阈值不下滑 | Start | mvn test 输出的覆盖率报告 | 待验证 |

---

## 一、Domain 层测试场景

### InventoryId 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D1 | 合法值创建 | - | new InventoryId(valid) | 创建成功 |
| D2 | null 值 | - | new InventoryId(null) | 抛 BusinessException(INVENTORY_ID_EMPTY) |
| D3 | 空字符串 | - | new InventoryId("") | 抛 BusinessException(INVENTORY_ID_EMPTY) |
| D4 | 相等性 | - | 两个相同值 | equals 返回 true |

### SkuCode 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D5 | 合法值创建 | - | new SkuCode(valid) | 创建成功 |
| D6 | null 值 | - | new SkuCode(null) | 抛 BusinessException(SKU_CODE_EMPTY) |
| D7 | 空字符串 | - | new SkuCode("") | 抛 BusinessException(SKU_CODE_EMPTY) |
| D8 | 超长值 | - | new SkuCode(>32 chars) | 抛 BusinessException(SKU_CODE_TOO_LONG) |
| D9 | 相等性 | - | 两个相同值 | equals 返回 true |

### Inventory 聚合根
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D10 | 创建聚合 | - | Inventory.create(productId, skuCode, stock) | 初始状态正确，reservedStock=0 |
| D11 | 预占库存 | availableStock >= quantity | reserve(quantity) | availableStock -= quantity, reservedStock += quantity |
| D12 | 预占超过可用 | availableStock < quantity | reserve(quantity) | 抛 INVENTORY_INSUFFICIENT |
| D13 | 扣减库存 | reservedStock >= quantity | deduct(quantity) | reservedStock -= quantity |
| D14 | 扣减超过预占 | reservedStock < quantity | deduct(quantity) | 抛 DEDUCT_EXCEEDS_RESERVED |
| D15 | 回滚库存 | reservedStock >= quantity | release(quantity) | reservedStock -= quantity, availableStock += quantity |
| D16 | 回滚超过预占 | reservedStock < quantity | release(quantity) | 抛 RELEASE_EXCEEDS_RESERVED |
| D17 | 调整库存 | adjustment 合理 | adjustStock(adjustment) | availableStock += adjustment |
| D18 | 调整后负库存 | adjustment 导致负数 | adjustStock(adjustment) | 抛 STOCK_NEGATIVE |
| D19 | 初始库存为负 | initialStock < 0 | Inventory.create(..., -1) | 抛 STOCK_NEGATIVE |

---

## 二、Application 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| A1 | 创建库存 | command 合法 | createInventory(command) | 返回 InventoryDTO, verify save() |
| A2 | 创建库存参数空 | command null 或字段空 | createInventory(command) | 抛 BusinessException |
| A3 | 查询库存 | inventoryId 存在 | getInventoryById(id) | 返回 InventoryDTO |
| A4 | 查询不存在 | inventoryId 不存在 | getInventoryById(id) | 抛 INVENTORY_NOT_FOUND |
| A5 | 按商品查询 | productId 存在 | getInventoryByProductId(productId) | 返回 InventoryDTO |
| A6 | 调整库存 | command 合法 | adjustStock(command) | 返回更新后 InventoryDTO |
| A7 | 预占库存 | stock 充足 | reserveStock(productId, quantity) | verify save() |
| A8 | 预占库存不足 | stock < quantity | reserveStock(productId, quantity) | 抛 INVENTORY_INSUFFICIENT |
| A9 | 扣减库存 | reservedStock 充足 | deductStock(productId, quantity) | verify save() |
| A10 | 回滚库存 | reservedStock 充足 | releaseStock(productId, quantity) | verify save() |

---

## 三、Infrastructure 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| I1 | 保存并查询 | - | save → findByInventoryId | 返回匹配记录 |
| I2 | 按商品查询 | - | save → findByProductId | 返回匹配记录 |
| I3 | 按SKU查询 | - | save → findBySkuCode | 返回匹配记录 |
| I4 | 查询不存在 | - | findByInventoryId | 返回 empty |
| I5 | DO ↔ Domain 转换 | - | 保存 → 查询 | 字段完整还原 |
| I6 | 更新库存 | 已存在记录 | reserve → save → 查询 | reservedStock 正确更新 |

---

## 四、Adapter 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| W1 | 创建库存成功 | Mock 正常返回 | POST /api/v1/inventory | 200 + success=true |
| W2 | 创建库存参数缺失 | productId 空 | POST /api/v1/inventory | 400 |
| W3 | 查询库存成功 | Mock 正常返回 | GET /api/v1/inventory/{id} | 200 + success=true |
| W4 | 查询不存在 | Mock 抛异常 | GET /api/v1/inventory/{id} | 404 + INVENTORY_NOT_FOUND |
| W5 | 按商品查询成功 | Mock 正常返回 | GET /api/v1/inventory/product/{productId} | 200 |
| W6 | 调整库存成功 | Mock 正常返回 | POST /api/v1/inventory/{id}/adjust | 200 |
| W7 | 调整库存参数缺失 | adjustment 空 | POST /api/v1/inventory/{id}/adjust | 400 |

---

## 五、集成测试场景（全链路）

| # | 场景 | 操作 | 预期结果 | 状态 |
|---|------|------|----------|------|
| E1 | Flyway migration 数量 | 运行 FlywayVerificationTest | 10 个 migrations 全执行 | ❌ FAIL |
| E2 | Order 从购物车创建（需库存） | 创建库存 → 加购物车 → 创建订单 | 200 + 订单创建成功 | ❌ FAIL（缺少库存数据） |
| E3 | 库存预占集成 | 创建库存 → 创建订单 → 验证库存预占 | reservedStock 增加 | 未实现 |

---

## 六、代码审查检查项

- [x] 依赖方向正确（adapter → application → domain ← infrastructure）
- [x] domain 模块无 Spring/框架 import
- [x] 聚合根封装业务不变量（非贫血模型）
- [x] 值对象不可变，equals/hashCode 正确
- [x] Repository 接口在 domain，实现在 infrastructure
- [x] 对象转换链正确：DO ↔ Domain ↔ DTO ↔ Request/Response
- [x] Controller 无业务逻辑
- [x] 异常通过 GlobalExceptionHandler 统一处理

## 七、代码风格检查项

- [x] Java 8 兼容（无 var、records、text blocks、List.of）
- [x] 聚合根用 @Getter，值对象用 @Getter + @EqualsAndHashCode + @ToString
- [x] DO 用 @Data + @TableName，DTO 用 @Data
- [x] 命名规范：XxxDO, XxxDTO, XxxMapper, XxxRepository, XxxRepositoryImpl
- [x] 包结构符合 com.claudej.{layer}.{aggregate}.{sublayer}
- [x] 测试命名 should_xxx_when_xxx