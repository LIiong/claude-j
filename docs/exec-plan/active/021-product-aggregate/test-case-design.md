# 测试用例设计 — 021-product-aggregate

## 测试范围
Product 聚合的领域模型测试（状态机、定价规则、SKU 校验）、应用服务编排测试、持久化往返测试、REST API 契约测试。

## 测试策略
按测试金字塔分层测试 + 代码审查 + 风格检查。

---

## 0. AC 自动化覆盖矩阵（强制，动笔前先填）

| # | 验收条件（AC） | 自动化层 | 对应测试方法 | 手动备注（若有） |
|---|---|---|---|---|
| AC1 | Product 聚合根封装状态机（DRAFT → ACTIVE → INACTIVE） | Domain | `ProductStatusTest.should_convertToActive_when_fromDraft` / `ProductStatusTest.should_convertToInactive_when_fromActive` / `ProductStatusTest.should_convertToActive_when_fromInactive` / `ProductStatusTest.should_throw_when_convertToInactive_fromDraft` | - |
| AC2 | 价格调整允许 DRAFT/ACTIVE/INACTIVE 状态，但禁止调整为负数或零 | Domain | `ProductTest.should_updatePrice_when_anyStatus` / `ProductTest.should_throw_when_updatePrice_withNegativeValue` / `ProductTest.should_throw_when_updatePrice_withZeroValue` | - |
| AC3 | 上架/下架操作遵循状态转换规则 | Domain + Application | `ProductTest.should_activate_when_draftOrInactive` / `ProductTest.should_deactivate_when_active` / `ProductApplicationServiceTest.should_activateProduct_when_validCommand` / `ProductApplicationServiceTest.should_deactivateProduct_when_validCommand` | - |
| AC4 | Repository 保存/查询往返正确 | Infrastructure | `ProductRepositoryImplTest.should_saveAndFindById_when_validProduct` / `ProductRepositoryImplTest.should_findByProductId_when_exists` / `ProductRepositoryImplTest.should_returnEmpty_when_productNotFound` | - |
| AC5 | REST API 契约符合设计 | Adapter | `ProductControllerTest.should_return200_when_createProduct` / `ProductControllerTest.should_return200_when_getProduct` / `ProductControllerTest.should_return200_when_updatePrice` / `ProductControllerTest.should_return200_when_activate` / `ProductControllerTest.should_return200_when_deactivate` / `ProductControllerTest.should_return400_when_invalidRequest` | - |
| AC6 | JaCoCo 阈值：domain 90% / application 80% | Start（CI） | `mvn test` 自动生成覆盖率报告 | CI 自动验证 |

---

## 一、Domain 层测试场景

### ProductId 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D1 | 合法值创建 | - | new ProductId("valid-uuid") | 创建成功 |
| D2 | null 值 | - | new ProductId(null) | 抛 BusinessException(PRODUCT_ID_EMPTY) |
| D3 | 空字符串 | - | new ProductId("") | 抛 BusinessException(PRODUCT_ID_EMPTY) |
| D4 | 相等性 | - | 两个相同 UUID 值 | equals 返回 true, hashCode 相同 |

### ProductName 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D5 | 合法值创建（2-100字符） | - | new ProductName("Valid Name") | 创建成功 |
| D6 | null 值 | - | new ProductName(null) | 抛 BusinessException(PRODUCT_NAME_EMPTY) |
| D7 | 空字符串 | - | new ProductName("") | 抛 BusinessException(PRODUCT_NAME_EMPTY) |
| D8 | 长度不足 2 字符 | - | new ProductName("A") | 抛 BusinessException(PRODUCT_NAME_LENGTH_INVALID) |
| D9 | 长度超过 100 字符 | - | new ProductName("超长字符串...") | 抛 BusinessException(PRODUCT_NAME_LENGTH_INVALID) |
| D10 | trim 处理 | - | new ProductName("  Valid Name  ") | 内部值已 trim |

### SKU 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D11 | 合法值创建 | - | new SKU("SKU001", 100) | 创建成功 |
| D12 | skuCode 为 null | - | new SKU(null, 100) | 抛 BusinessException(PRODUCT_SKU_CODE_EMPTY) |
| D13 | skuCode 为空 | - | new SKU("", 100) | 抛 BusinessException(PRODUCT_SKU_CODE_EMPTY) |
| D14 | skuCode 超长（>32） | - | new SKU("超长SKU编码...", 100) | 抛 BusinessException(PRODUCT_SKU_CODE_LENGTH_INVALID) |
| D15 | stock 为负数 | - | new SKU("SKU001", -1) | 抛 BusinessException(PRODUCT_STOCK_NEGATIVE) |
| D16 | stock 为零 | - | new SKU("SKU001", 0) | 创建成功（允许零库存） |
| D17 | 不可变验证 | SKU 已创建 | 尝试修改字段 | 值对象不可变，无 setter |

### ProductStatus 枚举（状态机）
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D18 | DRAFT → ACTIVE | status = DRAFT | status.toActive() | 返回 ACTIVE |
| D19 | ACTIVE → INACTIVE | status = ACTIVE | status.toInactive() | 返回 INACTIVE |
| D20 | INACTIVE → ACTIVE | status = INACTIVE | status.toActive() | 返回 ACTIVE |
| D21 | DRAFT → INACTIVE（非法） | status = DRAFT | status.toInactive() | 抛 BusinessException(INVALID_PRODUCT_STATUS_TRANSITION) |
| D22 | ACTIVE → ACTIVE（重复上架） | status = ACTIVE | status.toActive() | 抛 BusinessException(INVALID_PRODUCT_STATUS_TRANSITION) |
| D23 | INACTIVE → INACTIVE（重复下架） | status = INACTIVE | status.toInactive() | 抛 BusinessException(INVALID_PRODUCT_STATUS_TRANSITION) |

### Product 聚合根
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D24 | 创建商品（DRAFT 状态） | - | Product.create(name, sku, originalPrice, promotionalPrice, description) | 返回 Product，status=DRAFT |
| D25 | 创建商品（促销价可选） | - | promotionalPrice=null | 创建成功，getEffectivePrice()=originalPrice |
| D26 | 创建商品（原价负数） | - | originalPrice=-10 | 抛 BusinessException(PRODUCT_PRICE_NEGATIVE) |
| D27 | 创建商品（原价为零） | - | originalPrice=0 | 抛 BusinessException(PRODUCT_PRICE_ZERO) |
| D28 | 创建商品（促销价负数） | - | promotionalPrice=-10 | 抛 BusinessException(PRODUCT_PRICE_NEGATIVE) |
| D29 | 创建商品（促销价为零） | - | promotionalPrice=0 | 抛 BusinessException(PRODUCT_PRICE_ZERO) |
| D30 | 上架操作 | status=DRAFT | product.activate() | status=ACTIVE |
| D31 | 下架操作 | status=ACTIVE | product.deactivate() | status=INACTIVE |
| D32 | 重新上架 | status=INACTIVE | product.activate() | status=ACTIVE |
| D33 | 调价（DRAFT 状态） | status=DRAFT | product.updatePrice(originalPrice, promotionalPrice) | 价格更新成功 |
| D34 | 调价（ACTIVE 状态） | status=ACTIVE | product.updatePrice(originalPrice, promotionalPrice) | 价格更新成功（上架后允许调价） |
| D35 | 调价（INACTIVE 状态） | status=INACTIVE | product.updatePrice(originalPrice, promotionalPrice) | 价格更新成功 |
| D36 | 调价为负数 | status=任意 | product.updatePrice(-10, null) | 抛 BusinessException(PRODUCT_PRICE_NEGATIVE) |
| D37 | 调价为零 | status=任意 | product.updatePrice(0, null) | 抛 BusinessException(PRODUCT_PRICE_ZERO) |
| D38 | getEffectivePrice（有促销价） | promotionalPrice=79 | product.getEffectivePrice() | 返回 79（促销价优先） |
| D39 | getEffectivePrice（无促销价） | promotionalPrice=null | product.getEffectivePrice() | 返回 originalPrice |
| D40 | 重建聚合（从 DO） | - | Product.reconstruct(...) | 重建成功，所有字段还原 |

---

## 二、Application 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| A1 | 创建商品成功 | Mock repo.save() | service.createProduct(command) | 返回 ProductDTO，verify repo.save() |
| A2 | 查询商品成功 | Mock repo.findByProductId() 返回 Product | service.getProduct(productId) | 返回 ProductDTO |
| A3 | 查询商品不存在 | Mock repo.findByProductId() 返回 null | service.getProduct(productId) | 抛 BusinessException(PRODUCT_NOT_FOUND) |
| A4 | 调价成功 | Mock repo.findByProductId() + save() | service.updatePrice(command) | verify repo.save() 被调用 |
| A5 | 调价商品不存在 | Mock repo.findByProductId() 返回 null | service.updatePrice(command) | 抛 BusinessException(PRODUCT_NOT_FOUND) |
| A6 | 调价为负数 | command.originalPrice=-10 | service.updatePrice(command) | 抛 BusinessException（Domain 层抛出） |
| A7 | 上架成功 | Mock repo.findByProductId() + save() | service.activateProduct(command) | verify repo.save()，status=ACTIVE |
| A8 | 上架商品不存在 | Mock repo.findByProductId() 返回 null | service.activateProduct(command) | 抛 BusinessException(PRODUCT_NOT_FOUND) |
| A9 | 下架成功 | Mock repo.findByProductId() + save() | service.deactivateProduct(command) | verify repo.save()，status=INACTIVE |
| A10 | 下架商品不存在 | Mock repo.findByProductId() 返回 null | service.deactivateProduct(command) | 抛 BusinessException(PRODUCT_NOT_FOUND) |
| A11 | 分页查询成功 | Mock repo.findAll(PageRequest) | service.listProducts(status, pageRequest) | 返回 Page<ProductDTO> |
| A12 | 按状态查询成功 | Mock repo.findByStatus(status) | service.listProducts(ACTIVE, null) | 返回 List<ProductDTO> |

---

## 三、Infrastructure 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| I1 | 保存并按 ID 查询 | - | save(product) → findById(id) | 返回匹配记录，所有字段还原 |
| I2 | 保存并按 productId 查询 | - | save(product) → findByProductId(productId) | 返回匹配记录 |
| I3 | 查询不存在 | - | findByProductId("nonexistent") | 返回 null |
| I4 | 按状态查询 | 已保存 3 条不同状态 | findByStatus(ACTIVE) | 返回 ACTIVE 状态记录 |
| I5 | 分页查询 | 已保存 5 条 | findAll(PageRequest) | 返回分页结果，totalElements=5 |
| I6 | DO ↔ Domain 转换 | - | 保存 → 查询 | productId/skuCode/stock/价格/status 全部还原 |
| I7 | 逻辑删除 | - | deleted=1 的记录 | 查询不返回（MyBatis-Plus 自动过滤） |

---

## 四、Adapter 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| W1 | 创建商品成功（200） | Mock service 返回 DTO | POST /api/v1/products | HTTP 200，success=true |
| W2 | 创建商品参数校验失败（400） | name=null | POST /api/v1/products | HTTP 400，success=false |
| W3 | 创建商品参数校验失败（400） | originalPrice=null | POST /api/v1/products | HTTP 400，success=false |
| W4 | 查询商品成功（200） | Mock service 返回 DTO | GET /api/v1/products/{productId} | HTTP 200，success=true |
| W5 | 查询商品不存在（404） | Mock service 抛 PRODUCT_NOT_FOUND | GET /api/v1/products/{productId} | HTTP 404，success=false |
| W6 | 调价成功（200） | Mock service 返回 DTO | PUT /api/v1/products/{productId}/price | HTTP 200，success=true |
| W7 | 调价参数校验失败（400） | originalPrice=null | PUT /api/v1/products/{productId}/price | HTTP 400，success=false |
| W8 | 调价负数（400） | originalPrice=-10 | PUT /api/v1/products/{productId}/price | HTTP 400（Domain 层异常） |
| W9 | 上架成功（200） | Mock service 返回 DTO | PUT /api/v1/products/{productId}/activate | HTTP 200，success=true |
| W10 | 下架成功（200） | Mock service 返回 DTO | PUT /api/v1/products/{productId}/deactivate | HTTP 200，success=true |
| W11 | 分页查询成功（200） | Mock service 返回 Page | GET /api/v1/products?status=ACTIVE&page=0&size=10 | HTTP 200，success=true |
| W12 | 分页查询参数校验（400） | page=-1 | GET /api/v1/products?page=-1 | HTTP 400，success=false |

---

## 五、集成测试场景（全链路）

| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E1 | 商品完整生命周期 | 创建（DRAFT）→ 上架（ACTIVE）→ 调价 → 下架（INACTIVE）→ 查询 | 每步状态正确，价格更新成功 |
| E2 | 上架后调价验证 | 创建 → 上架 → 调价 → 查询 | 调价成功，不影响历史订单（仅验证 Product 聚合） |
| E3 | 促销价生效验证 | 创建（促销价=79）→ 查询 effectivePrice | 返回促销价 79 |

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