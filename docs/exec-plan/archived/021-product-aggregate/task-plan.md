# 任务执行计划 — 021-product-aggregate

## 任务状态跟踪

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Domain: ProductId + ProductName + SKU 值对象 + 测试 | dev | 单测通过 | commit: 800792a |
| 2 | Domain: ProductStatus 枚举 + 状态转换测试 | dev | 单测通过 | commit: 8a8edcf |
| 3 | Domain: Product 聚合根 + 测试 | dev | 单测通过 | commit: c71c461 |
| 4 | Domain: ProductRepository 端口 | dev | 单测通过 | commit: 2269aec |
| 5 | Application: Command + DTO + Assembler | dev | 单测通过 | commit: 04f7ec1 |
| 6 | Application: ProductApplicationService + 测试 | dev | 单测通过 | commit: 04f7ec1 |
| 7 | Infrastructure: ProductDO + ProductMapper + ProductConverter | dev | 单测通过 | commit: 92fb798 |
| 8 | Infrastructure: ProductRepositoryImpl + 测试 | dev | 单测通过 | commit: 92fb798 |
| 9 | Adapter: ProductController + Request/Response + 测试 | dev | 单测通过 | commit: 6e09ab2 |
| 10 | Start: V8__product_init.sql Flyway 迁移 | dev | 单测通过 | H2兼容迁移已配置 |
| 11 | ErrorCode 扩展（Product 相关错误码） | dev | 单测通过 | commit: 800792a |
| 12 | 全量 mvn test + checkstyle + entropy-check | dev | 验证通过 | Tests: 133, 0 errors, checkstyle pass, entropy 0 errors |
| 13 | QA: 测试用例设计 | qa | 完成 | test-case-design.md 已存在 |
| 14 | QA: 验收测试 + 代码审查 | qa | 完成 | test-report.md 已提交 |

## 执行顺序

domain → application → infrastructure → adapter → start → 全量测试 → QA 验收

## 原子任务分解

### 1.1 Domain 值对象 ProductId
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/product/model/valobj/ProductId.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/product/model/valobj/ProductIdTest.java`
- **骨架**（Red 阶段先写测试）：
  ```java
  // ProductIdTest.java — 覆盖构造校验 / 相等性
  @Test
  void should_throw_when_value_is_null() { ... }
  @Test
  void should_throw_when_value_is_empty() { ... }
  @Test
  void should_equal_when_values_match() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=ProductIdTest`
- **预期输出**：`Tests run: 3, Failures: 0, Errors: 0`
- **commit**：`feat(domain): product 值对象 ProductId`

### 1.2 Domain 值对象 ProductName
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/product/model/valobj/ProductName.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/product/model/valobj/ProductNameTest.java`
- **骨架**：覆盖长度校验（2-100）/ null 校验 / trim
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=ProductNameTest`
- **预期输出**：`Tests run: 4, Failures: 0, Errors: 0`
- **commit**：`feat(domain): product 值对象 ProductName`

### 1.3 Domain 值对象 SKU
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/product/model/valobj/SKU.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/product/model/valobj/SKUTest.java`
- **骨架**：覆盖 skuCode 长度校验（<=32）/ stock >= 0 / 不可变
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=SKUTest`
- **预期输出**：`Tests run: 4, Failures: 0, Errors: 0`
- **commit**：`feat(domain): product 值对象 SKU`

### 2.1 Domain 枚举 ProductStatus
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/product/model/valobj/ProductStatus.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/product/model/valobj/ProductStatusTest.java`
- **骨架**：覆盖状态转换规则（DRAFT→ACTIVE, ACTIVE→INACTIVE, INACTIVE→ACTIVE, 非法转换抛异常）
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=ProductStatusTest`
- **预期输出**：`Tests run: 6, Failures: 0, Errors: 0`
- **commit**：`feat(domain): product 状态枚举 ProductStatus`

### 3.1 Domain 聚合根 Product
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/product/model/aggregate/Product.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/product/model/aggregate/ProductTest.java`
- **骨架**：
  - 工厂方法 `create(...)` + `reconstruct(...)`
  - 状态转换 `activate()` / `deactivate()`
  - 调价 `updatePrice(...)` — 仅 DRAFT 状态可调价
  - `getEffectivePrice()` — 促销价优先
  - `@Getter` 无 `@Setter`
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=ProductTest`
- **预期输出**：覆盖不变量 / 状态转换 / 调价 / 异常场景，全部绿
- **commit**：`feat(domain): product 聚合根与不变量`

### 4.1 Domain Repository 端口
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/product/repository/ProductRepository.java`
- **骨架**：`save`, `findById`, `findByProductId`, `findByStatus`, `findAll(PageRequest)`
- **验证命令**：`mvn compile -pl claude-j-domain`
- **预期输出**：编译通过
- **commit**：`feat(domain): product Repository 端口`

### 5.1 Application Command + DTO
- **文件**：
  - `claude-j-application/src/main/java/com/claudej/application/product/command/CreateProductCommand.java`
  - `claude-j-application/src/main/java/com/claudej/application/product/command/UpdatePriceCommand.java`
  - `claude-j-application/src/main/java/com/claudej/application/product/command/ActivateProductCommand.java`
  - `claude-j-application/src/main/java/com/claudej/application/product/command/DeactivateProductCommand.java`
  - `claude-j-application/src/main/java/com/claudej/application/product/dto/ProductDTO.java`
- **验证命令**：`mvn compile -pl claude-j-application`
- **预期输出**：编译通过
- **commit**：`feat(application): product 命令与 DTO`

### 5.2 Application Assembler
- **文件**：`claude-j-application/src/main/java/com/claudej/application/product/assembler/ProductAssembler.java`
- **骨架**：MapStruct `@Mapper(componentModel = "spring")` — Domain ↔ DTO
- **验证命令**：`mvn compile -pl claude-j-application`
- **预期输出**：编译通过
- **commit**：`feat(application): product 组装器`

### 6.1 Application Service + 测试
- **文件**：`claude-j-application/src/main/java/com/claudej/application/product/service/ProductApplicationService.java`
- **测试**：`claude-j-application/src/test/java/com/claudej/application/product/service/ProductApplicationServiceTest.java`
- **骨架**：
  - `createProduct(CreateProductCommand)`
  - `getProduct(String productId)`
  - `updatePrice(UpdatePriceCommand)`
  - `activateProduct(ActivateProductCommand)`
  - `deactivateProduct(DeactivateProductCommand)`
  - `listProducts(ProductStatus status, PageRequest pageRequest)`
  - 使用 `@ExtendWith(MockitoExtension.class)` + `@Mock ProductRepository`
- **验证命令**：`mvn test -pl claude-j-application`
- **预期输出**：Mockito verify 通过，编排顺序正确
- **commit**：`feat(application): product 应用服务与编排`

### 7.1 Infrastructure DO + Converter
- **文件**：
  - `claude-j-infrastructure/src/main/java/com/claudej/infrastructure/product/persistence/dataobject/ProductDO.java`
  - `claude-j-infrastructure/src/main/java/com/claudej/infrastructure/product/persistence/converter/ProductConverter.java`
- **骨架**：`@TableName("t_product")` + MapStruct Converter
- **验证命令**：`mvn compile -pl claude-j-infrastructure`
- **预期输出**：无 DO 泄漏到层外（guard-java-layer.sh 不报警）
- **commit**：`feat(infrastructure): product DO 与转换器`

### 7.2 Infrastructure Mapper
- **文件**：`claude-j-infrastructure/src/main/java/com/claudej/infrastructure/product/persistence/mapper/ProductMapper.java`
- **骨架**：`extends BaseMapper<ProductDO>`
- **验证命令**：`mvn compile -pl claude-j-infrastructure`
- **预期输出**：编译通过
- **commit**：`feat(infrastructure): product Mapper`

### 8.1 Infrastructure Repository 实现 + 测试
- **文件**：`claude-j-infrastructure/src/main/java/com/claudej/infrastructure/product/persistence/repository/ProductRepositoryImpl.java`
- **测试**：`claude-j-infrastructure/src/test/java/com/claudej/infrastructure/product/persistence/repository/ProductRepositoryImplTest.java`
- **骨架**：`@SpringBootTest` + H2，覆盖保存→查询往返 / 状态查询 / 分页查询
- **验证命令**：`mvn test -pl claude-j-infrastructure -Dtest=ProductRepositoryImplTest`
- **预期输出**：DO↔Domain 映射准确，H2 写入回读一致
- **commit**：`feat(infrastructure): product 持久化实现`

### 9.1 Adapter Controller + Request/Response
- **文件**：
  - `claude-j-adapter/src/main/java/com/claudej/adapter/product/web/ProductController.java`
  - `claude-j-adapter/src/main/java/com/claudej/adapter/product/web/request/CreateProductRequest.java`
  - `claude-j-adapter/src/main/java/com/claudej/adapter/product/web/request/UpdatePriceRequest.java`
  - `claude-j-adapter/src/main/java/com/claudej/adapter/product/web/response/ProductResponse.java`
- **测试**：`claude-j-adapter/src/test/java/com/claudej/adapter/product/web/ProductControllerTest.java`
- **骨架**：
  - `@WebMvcTest` + MockMvc + `@MockBean ProductApplicationService`
  - 覆盖创建 / 查询 / 调价 / 上架 / 下架 / 分页
  - HTTP 状态码 200/400/404 断言
- **验证命令**：`mvn test -pl claude-j-adapter -Dtest=ProductControllerTest`
- **预期输出**：HTTP 状态码断言全过
- **commit**：`feat(adapter): product REST 端点`

### 10.1 Start DDL Flyway 迁移
- **文件**：`claude-j-start/src/main/resources/db/migration/V8__product_init.sql`
- **验证命令**：`mvn test -pl claude-j-infrastructure -Dtest=ProductRepositoryImplTest`（H2 会执行 Flyway）
- **预期输出**：表名 t_product，列名 snake_case
- **commit**：`feat(start): product Flyway DDL`

### 11.1 ErrorCode 扩展
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/common/exception/ErrorCode.java`
- **骨架**：添加 PRODUCT_NOT_FOUND / PRODUCT_NAME_EMPTY / PRODUCT_SKU_CODE_EMPTY / INVALID_PRODUCT_STATUS_TRANSITION 等
- **验证命令**：`mvn compile -pl claude-j-domain`
- **预期输出**：编译通过
- **commit**：`feat(domain): product ErrorCode`

### 12.1 全量验证
- **验证命令**：`mvn test && mvn checkstyle:check && ./scripts/entropy-check.sh`
- **预期输出**：三项全过
- **commit**：不单独 commit，合并到最后

## 开发完成记录

<!-- dev 完成后填写 -->
- 全量 `mvn clean test`：Tests run: 133, Failures: 0, Errors: 0, Skipped: 0 (2026-04-25 08:46:35)
- 架构合规检查：checkstyle pass, entropy-check 0 errors 13 warnings (2026-04-25 08:47:01)
- 通知 @qa 时间：待 QA 开始验收

## QA 验收记录

<!-- qa 验收后填写 -->
- 全量测试（含集成测试）：Tests run: 133+74+59 = 全量通过，BUILD SUCCESS (2026-04-25 09:01)
- 代码审查结果：依赖方向正确、Domain 纯净、聚合根封装不变量、值对象不可变、状态机规则正确、DO 未泄漏
- 代码风格检查：checkstyle 0 violations, entropy-check 0 errors
- 问题清单：详见 test-report.md（无阻塞性问题）
- **最终状态**：验收通过