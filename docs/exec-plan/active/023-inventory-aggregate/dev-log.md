# 开发日志 — 023-inventory-aggregate

## 问题记录

<!-- Build 阶段填写，每条必须四段齐全 -->

### QA 第一轮打回修复记录

#### 问题 1：FlywayVerificationTest migration 数量不匹配

**Issue**：
- `should_record_10_migrations_when_flyway_migrates` 失败
- 预期 10 个 migrations，断言检查 results.get(9) 版本号为 "10"

**Root Cause**：
- Flyway 实际执行了 10 个 migrations（通过调试代码打印确认）
- SQL ORDER BY version 是**字符串排序**，导致 "10" 排在 "1" 和 "2" 之间（排序结果：1, 10, 2, 3, ...）
- 原断言 `results.get(9)` 获取的是排序后第 10 个元素，实际是 version 9，不是 version 10

**Fix**：
- 修改断言逻辑，使用 Stream filter 查找特定版本，不依赖排序位置
- 移除不必要的 @DirtiesContext 注解（根因不是 context 缓存问题）

**Verification**：
```bash
mvn test -pl claude-j-start -Dtest=FlywayVerificationTest
# Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
# BUILD SUCCESS
```

#### 问题 2：OrderFromCartIntegrationTest 3 个测试失败

**Issue**：
- POST /api/v1/orders/from-cart 返回 404 + INVENTORY_NOT_FOUND
- 测试使用的 productId（PROD_CART_001 等）没有对应的库存记录

**Root Cause**：
- OrderApplicationService.createOrderFromCart() 现在调用 reserveStockForOrder()
- reserveStockForOrder() 通过 InventoryRepository.findByProductId() 查询库存
- 测试数据中只创建了购物车项，未创建库存记录，导致 INVENTORY_NOT_FOUND

**Fix**：
- 在 OrderFromCartIntegrationTest 中添加 createInventoryForProduct() 方法
- 在每个测试方法中，先调用 Inventory API 创建库存记录（POST /api/v1/inventory）
- 受影响的测试：should_createOrderFromCart_when_cartExistsWithItems、should_createOrderWithCorrectTotal_when_multipleItemsInCart、should_return400_when_cartIsEmpty、should_clearCartSuccessfully_when_orderCreated

**Verification**：
```bash
mvn test -pl claude-j-start -Dtest=OrderFromCartIntegrationTest
# Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
# BUILD SUCCESS
```

#### 问题 3：Inventory 聚合未在 CLAUDE.md 更新

**Issue**：
- entropy-check WARN: 聚合 inventory 在 domain 层存在但未在 CLAUDE.md 聚合列表中记录

**Root Cause**：
- Build 阶段遗漏了 CLAUDE.md 文档更新

**Fix**：
- 更新 CLAUDE.md 聚合列表，新增 `| inventory | com.claudej.*.inventory | 库存服务 |`
- 同时修正 cart 聚合说明（质物车 → 购物车）

**Verification**：
```bash
./scripts/entropy-check.sh
# 错误 (FAIL): 0, 警告 (WARN): 12, status: PASS
# 聚合列表与代码同步: PASS
```

## 变更记录

<!-- 与原设计（requirement-design.md）不一致的变更 -->

### Build 阶段变更

1. **V10 DDL 修改**：从 MySQL 特定语法改为 H2 兼容语法
   - 原设计：`ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`
   - 实际：移除 MySQL 特定语法，使用标准 SQL
   - 原因：确保 H2 测试环境兼容