# 测试报告 — 023-inventory-aggregate

**测试日期**：2026-04-25
**测试人员**：@qa
**版本状态**：待修复
**任务类型**：业务聚合

---

## 一、测试执行结果

### 分层测试：`mvn clean test` ❌ 失败

| 模块 | 测试类 | 用例数 | 通过 | 失败 | 耗时 |
|------|--------|--------|------|------|------|
| domain | InventoryIdTest | 4 | 4 | 0 | ~0.1s |
| domain | SkuCodeTest | 4 | 4 | 0 | ~0.1s |
| domain | InventoryTest | 27 | 27 | 0 | ~0.1s |
| application | InventoryApplicationServiceTest | 14 | 14 | 0 | ~0.1s |
| infrastructure | InventoryRepositoryImplTest | 7 | 7 | 0 | ~1.5s |
| adapter | InventoryControllerTest | 12 | 12 | 0 | ~0.5s |
| start | FlywayVerificationTest | 2 | 1 | 1 | ~1.3s |
| start | OrderFromCartIntegrationTest | 6 | 3 | 3 | ~10s |
| **分层合计** | **8 个测试类** | **76** | **72** | **4** | **~15s** |

**失败详情**：
```
[ERROR] FlywayVerificationTest.should_record_10_migrations_when_flyway_migrates:29
expected: "10" but was: "9"

[ERROR] OrderFromCartIntegrationTest.should_createOrderFromCart_when_cartExistsWithItems:80
Status expected:<200> but was:<404>
Body: {"success":false,"errorCode":"INVENTORY_NOT_FOUND","message":"商品库存不存在: PROD_CART_001"}

[ERROR] OrderFromCartIntegrationTest.should_createOrderWithCorrectTotal_when_multipleItemsInCart:144
Status expected:<200> but was:<404>

[ERROR] OrderFromCartIntegrationTest.should_clearCartSuccessfully_when_orderCreated:225
Status expected:<200> but was:<404>
```

### 代码风格检查：`mvn checkstyle:check` ✅ 通过

```
[INFO] You have 0 Checkstyle violations.
[INFO] BUILD SUCCESS
```

### 架构合规检查：`./scripts/entropy-check.sh` ✅ PASS

```
错误 (FAIL): 0
警告 (WARN): 13
status: PASS
```

**关键警告**：
- WARN: 聚合 inventory 在 domain 层存在但未在 CLAUDE.md 聚合列表中记录
- WARN: auth 聚合缺少 6 个测试文件（与本任务无关，但需后续处理）

### 测试用例覆盖映射

| 设计用例 | 对应测试方法 | 状态 |
|----------|-------------|------|
| D1-D19 | InventoryIdTest + SkuCodeTest + InventoryTest (35 cases) | ✅ |
| A1-A10 | InventoryApplicationServiceTest (14 cases) | ✅ |
| I1-I6 | InventoryRepositoryImplTest (7 cases) | ✅ |
| W1-W7 | InventoryControllerTest (12 cases) | ✅ |
| E1-E3 | FlywayVerificationTest + OrderFromCartIntegrationTest | ❌ |

---

## 二、代码审查结果

### 依赖方向检查

| 检查项 | 结果 | 说明 |
|--------|------|------|
| adapter → application（不依赖 domain/infrastructure） | ✅ | InventoryController 仅依赖 InventoryApplicationService |
| application → domain（不依赖其他层） | ✅ | InventoryApplicationService 仅依赖 InventoryRepository(domain) |
| domain 无外部依赖 | ✅ | Inventory/InventoryId/SkuCode 无 Spring/MyBatis import |
| infrastructure → domain + application | ✅ | InventoryRepositoryImpl 仅依赖 InventoryConverter + Mapper |

### 领域模型检查

| 检查项 | 结果 |
|--------|------|
| domain 模块零 Spring/框架 import | ✅ |
| 聚合根封装业务不变量（非贫血模型） | ✅ reserve/deduct/release/adjustStock 方法封装不变量 |
| 值对象不可变，字段 final | ✅ InventoryId/SkuCode 使用 final 字段 |
| 值对象 equals/hashCode 正确 | ✅ @EqualsAndHashCode 注解 |
| Repository 接口在 domain，实现在 infrastructure | ✅ |

### 对象转换链检查

| 转换 | 方式 | 结果 |
|------|------|------|
| Request → Command | 手动赋值 | ✅ Controller 中手动赋值 |
| Domain → DTO | MapStruct | ✅ InventoryAssembler |
| Domain ↔ DO | 静态方法 + 手动 | ✅ InventoryConverter |
| DTO → Response | 手动赋值 | ✅ Controller.convertToResponse |
| DO 未泄漏到 infrastructure 之上 | ✅ | 仅 infrastructure 层可见 InventoryDO |

### Controller 检查

| 检查项 | 结果 |
|--------|------|
| Controller 无业务逻辑，仅委托 application service | ✅ |
| 异常通过 GlobalExceptionHandler 统一处理 | ✅ |
| HTTP 状态码正确 | ✅ 200/400/404 测试通过 |

---

## 三、代码风格检查结果

| 检查项 | 结果 |
|--------|------|
| Java 8 兼容（无 var、records、text blocks、List.of） | ✅ |
| 聚合根仅 @Getter | ✅ Inventory 使用 @Getter |
| 值对象 @Getter + @EqualsAndHashCode + @ToString | ✅ InventoryId/SkuCode |
| DO 用 @Data + @TableName | ✅ InventoryDO |
| DTO 用 @Data | ✅ InventoryDTO |
| 命名规范：XxxDO, XxxDTO, XxxMapper, XxxRepository, XxxRepositoryImpl | ✅ |
| 包结构 com.claudej.{layer}.{aggregate}.{sublayer} | ✅ |
| 测试命名 should_xxx_when_xxx | ✅ |

---

## 四、测试金字塔合规

| 层 | 测试类型 | 框架 | Spring 上下文 | 结果 |
|---|---------|------|-------------|------|
| Domain | 纯单元测试 | JUnit 5 + AssertJ | 无 | ✅ 35 cases |
| Application | Mock 单元测试 | JUnit 5 + Mockito | 无 | ✅ 14 cases |
| Infrastructure | 集成测试 | @SpringBootTest + H2 | 有 | ✅ 7 cases |
| Adapter | API 测试 | @WebMvcTest + MockMvc | 部分（Web 层） | ✅ 12 cases |
| **全链路** | **接口集成测试** | **@SpringBootTest + AutoConfigureMockMvc + H2** | **完整** | ❌ 4 failures |

---

## 五、问题清单

| # | 严重度 | 描述 | 处理 |
|---|--------|------|------|
| 1 | **Critical** | FlywayVerificationTest migration 数量不匹配：预期 10，实际 9。V10 文件存在且命名正确，但 Flyway 只执行了 9 个 migrations。疑似 Spring Test context 缓存问题。 | **@dev 修复** |
| 2 | **Critical** | OrderFromCartIntegrationTest 3 个测试失败（404 状态码）。根因：OrderApplicationService.createOrderFromCart() 现在调用 reserveStockForOrder()，但测试使用的 productId（PROD_CART_001 等）没有对应的库存记录。抛出 INVENTORY_NOT_FOUND。**这是 Inventory 集成后遗漏的测试数据准备**。 | **@dev 修复**：在测试中先创建库存记录，或使用已存在的产品 |
| 3 | **Major** | Inventory 聚合未在 CLAUDE.md 聚合列表中更新。entropy-check 已报警。 | **@dev 修复**：更新 CLAUDE.md |
| 4 | **Minor** | auth 聚合缺少测试文件（与本任务无关）。 | 后续任务处理 |

**2 个 Critical 问题阻塞验收，1 个 Major 问题需修复。**

---

## 六、验收结论

| 维度 | 结论 |
|------|------|
| 功能完整性 | ❌ Inventory 聚合实现完整，但 Order 集成后破坏了现有测试 |
| 测试覆盖 | ❌ 72/76 测试通过，4 个失败阻塞验收 |
| 架构合规 | ✅ ArchUnit 13 条规则通过，依赖方向正确 |
| 代码风格 | ✅ checkstyle 0 violations |
| 数据库设计 | ✅ V10 DDL 格式正确，H2 兼容 |

### 最终状态：❌ 待修复 — 见问题清单

**根因分析**：
- 问题 #1 和 #2 疑似共同根因：**Inventory 集成 Order 后，@dev 未同步更新测试数据准备**。
- OrderApplicationService 现在依赖 InventoryRepository，从购物车创建订单需要先有库存记录。
- Flyway 问题可能是 Spring Test context 缓存导致的副作用。

**建议 @dev 修复方案**：
1. 问题 #2：修改 OrderFromCartIntegrationTest，在测试中先调用 Inventory API 创建库存记录，或使用 V8/V10 中已存在的产品 ID。
2. 问题 #1：在 FlywayVerificationTest 上添加 `@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)` 强制重建 Spring context。
3. 问题 #3：更新 CLAUDE.md 聚合列表，新增 `| inventory | com.claudej.*.inventory | 库存服务 |`。

**打回 @dev 处理，修复后重新提交验收。**