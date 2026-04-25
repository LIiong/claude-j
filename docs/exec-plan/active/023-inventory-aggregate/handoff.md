# 任务交接 — 023-inventory-aggregate

## 基本信息
- **task-id**: 023-inventory-aggregate
- **from**: qa
- **to**: dev
- **status**: changes-requested
- **phase**: Verify -> Build（返工）

## 产出物清单
- `docs/exec-plan/active/023-inventory-aggregate/test-case-design.md` — 测试用例设计
- `docs/exec-plan/active/023-inventory-aggregate/test-report.md` — 测试报告（含问题清单）
- `docs/exec-plan/active/023-inventory-aggregate/handoff.md` — 交接文件

## QA 验收结果

### 独立验证执行（三项预飞）

```bash
# 2026-04-25 @qa 独立执行
mvn clean test
# 结果：Tests run: 59, Failures: 4, Errors: 0
# 失败详情：
# - FlywayVerificationTest.should_record_10_migrations_when_flyway_migrates: expected 10, was 9
# - OrderFromCartIntegrationTest: 3 tests 返回 404 (INVENTORY_NOT_FOUND)

mvn checkstyle:check -B
# 结果：BUILD SUCCESS, 0 violations

./scripts/entropy-check.sh
# 结果：PASS (0 FAIL, 13 WARN)
```

### 代码审查结果
- ✅ 依赖方向正确
- ✅ Domain 层纯净（无 Spring/框架 import）
- ✅ 聚合根封装不变量
- ✅ 值对象不可变 + equals/hashCode 正确
- ✅ Repository 接口在 domain，实现在 infrastructure
- ✅ 对象转换链正确
- ✅ Controller 无业务逻辑
- ✅ Java 8 兼容

### 测试覆盖统计
- Domain: 35 cases ✅
- Application: 14 cases ✅
- Infrastructure: 7 cases ✅
- Adapter: 12 cases ✅
- Integration: 4 failures ❌

## pre-flight（QA 验证）
- mvn-test: **FAIL**（4 failures）
- checkstyle: **PASS**（0 violations）
- entropy-check: **PASS**（0 FAIL, 13 WARN）

## 问题清单（Critical 阻塞验收）

### 问题 #1：FlywayVerificationTest migration 数量不匹配
- **严重度**: Critical
- **现象**: 预期 10 个 migrations，实际只有 9 个被 Flyway 执行
- **根因**: Spring Test context 缓存问题，其他测试先运行创建 context 时只有 9 个 migrations
- **修复建议**: 在 FlywayVerificationTest 上添加 `@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)` 强制重建 Spring context

### 问题 #2：OrderFromCartIntegrationTest 3 个测试失败
- **严重度**: Critical
- **现象**: POST /api/v1/orders/from-cart 返回 404 + INVENTORY_NOT_FOUND
- **根因**: OrderApplicationService.createOrderFromCart() 现在调用 reserveStockForOrder()，但测试使用的 productId（PROD_CART_001 等）没有对应的库存记录
- **这是 Inventory 集成后遗漏的关键测试数据准备**
- **修复建议**:
  1. 在 OrderFromCartIntegrationTest 中先通过 Inventory API 创建库存记录
  2. 或使用 V8/V10 中已存在的产品 ID

### 问题 #3：Inventory 聚合未在 CLAUDE.md 更新
- **严重度**: Major
- **现象**: entropy-check WARN: 聚合 inventory 在 domain 层存在但未在 CLAUDE.md 聚合列表中记录
- **修复建议**: 更新 CLAUDE.md，新增 `| inventory | com.claudej.*.inventory | 库存服务 |`

## summary
QA 验收发现 2 个 Critical 问题阻塞验收：
1. Flyway migration 数量不匹配（Spring context 缓存问题）
2. Order 从购物车创建失败（缺少库存数据准备）

请 @dev 按 systematic-debugging SKILL 进行根因调查后修复，修复后重新提交验收。

**返工轮次**: 第 1 轮（最多允许 3 轮）