# 任务执行计划 — 023-inventory-aggregate

## 任务状态跟踪

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Domain: InventoryId 值对象 + 测试 | dev | 待办 | |
| 2 | Domain: SkuCode 值对象 + 测试 | dev | 待办 | |
| 3 | Domain: ErrorCode 新增库存相关错误码 | dev | 待办 | |
| 4 | Domain: Inventory 聚合根 + 测试 | dev | 待办 | 含 reserve/deduct/release 方法 |
| 5 | Domain: InventoryRepository 端口接口 | dev | 待办 | |
| 6 | Application: Command + DTO | dev | 待办 | CreateInventoryCommand, InventoryDTO |
| 7 | Application: InventoryAssembler | dev | 待办 | MapStruct |
| 8 | Application: InventoryApplicationService + 测试 | dev | 待办 | Mock Repository |
| 9 | Application: OrderApplicationService 集成库存 | dev | 待办 | 修改现有服务，注入 InventoryRepository |
| 10 | Infrastructure: InventoryDO + Mapper | dev | 待办 | |
| 11 | Infrastructure: InventoryConverter | dev | 待办 | DO ↔ Domain |
| 12 | Infrastructure: InventoryRepositoryImpl + 测试 | dev | 待办 | H2 集成测试 |
| 13 | Adapter: InventoryController + Request/Response + 测试 | dev | 待办 | MockMvc |
| 14 | Start: V10__add_inventory.sql DDL | dev | 待办 | |
| 15 | 全量 mvn test + checkstyle + entropy-check | dev | 待办 | |
| 16 | QA: 测试用例设计 | qa | 待办 | |
| 17 | QA: 验收测试 + 代码审查 | qa | 待办 | |

## 执行顺序
domain → application → infrastructure → adapter → start → 全量测试 → QA 验收

## 原子任务分解

### 1.1 Domain 值对象 `InventoryId`
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/inventory/model/valobj/InventoryId.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/inventory/model/valobj/InventoryIdTest.java`
- **骨架**（Red 阶段先写测试）：
  ```java
  // InventoryIdTest.java
  @Test
  void should_throw_when_value_is_null() { ... }
  @Test
  void should_throw_when_value_is_empty() { ... }
  @Test
  void should_equal_when_values_match() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=InventoryIdTest`
- **预期输出**：`Tests run: 4, Failures: 0, Errors: 0`
- **commit**：`feat(domain): inventory 值对象 InventoryId`

### 1.2 Domain 值对象 `SkuCode`
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/inventory/model/valobj/SkuCode.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/inventory/model/valobj/SkuCodeTest.java`
- **骨架**：
  ```java
  @Test
  void should_throw_when_value_is_null() { ... }
  @Test
  void should_throw_when_value_is_empty() { ... }
  @Test
  void should_throw_when_value_too_long() { ... }
  @Test
  void should_equal_when_values_match() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=SkuCodeTest`
- **预期输出**：`Tests run: 4, Failures: 0, Errors: 0`
- **commit**：`feat(domain): inventory 值对象 SkuCode`

### 1.3 Domain ErrorCode 扩展
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/common/exception/ErrorCode.java`
- **新增错误码**：
  ```java
  // Inventory Management
  INVENTORY_NOT_FOUND("INVENTORY_NOT_FOUND", "库存不存在"),
  INVENTORY_INSUFFICIENT("INVENTORY_INSUFFICIENT", "库存不足"),
  INVENTORY_ID_EMPTY("INVENTORY_ID_EMPTY", "库存ID不能为空"),
  SKU_CODE_EMPTY("SKU_CODE_EMPTY", "SKU编码不能为空"),
  SKU_CODE_TOO_LONG("SKU_CODE_TOO_LONG", "SKU编码长度不能超过32"),
  STOCK_NEGATIVE("STOCK_NEGATIVE", "库存不能为负数"),
  RESERVE_NEGATIVE("RESERVE_NEGATIVE", "预占数量必须大于0"),
  DEDUCT_NEGATIVE("DEDUCT_NEGATIVE", "扣减数量必须大于0"),
  RELEASE_NEGATIVE("RELEASE_NEGATIVE", "回滚数量必须大于0")
  ```
- **验证命令**：`mvn compile -pl claude-j-domain`
- **预期输出**：编译通过
- **commit**：`feat(domain): inventory 错误码定义`

### 1.4 Domain 聚合根 `Inventory`
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/inventory/model/aggregate/Inventory.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/inventory/model/aggregate/InventoryTest.java`
- **骨架**：
  ```java
  // InventoryTest.java — 覆盖创建/预占/扣减/回滚/不变量
  @Test
  void should_create_when_validParameters() { ... }
  @Test
  void should_reserve_when_stockAvailable() { ... }
  @Test
  void should_throw_when_reserveExceedsAvailable() { ... }
  @Test
  void should_deduct_when_reservedEnough() { ... }
  @Test
  void should_throw_when_deductExceedsReserved() { ... }
  @Test
  void should_release_when_reservedEnough() { ... }
  @Test
  void should_throw_when_releaseExceedsReserved() { ... }
  @Test
  void should_throw_when_initialStockNegative() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=InventoryTest`
- **预期输出**：`Tests run: 10+, Failures: 0, Errors: 0`
- **commit**：`feat(domain): inventory 聚合根与不变量`

### 1.5 Domain Repository 端口
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/inventory/repository/InventoryRepository.java`
- **骨架**：
  ```java
  public interface InventoryRepository {
      Inventory save(Inventory inventory);
      Optional<Inventory> findByInventoryId(InventoryId inventoryId);
      Optional<Inventory> findByProductId(String productId);
      Optional<Inventory> findBySkuCode(SkuCode skuCode);
      boolean existsByInventoryId(InventoryId inventoryId);
  }
  ```
- **验证命令**：`mvn compile -pl claude-j-domain`
- **预期输出**：编译通过
- **commit**：`feat(domain): inventory Repository 端口接口`

### 2.1 Application Command + DTO
- **文件**：
  - `claude-j-application/src/main/java/com/claudej/application/inventory/command/CreateInventoryCommand.java`
  - `claude-j-application/src/main/java/com/claudej/application/inventory/command/AdjustStockCommand.java`
  - `claude-j-application/src/main/java/com/claudej/application/inventory/dto/InventoryDTO.java`
- **验证命令**：`mvn compile -pl claude-j-application`
- **预期输出**：编译通过
- **commit**：`feat(application): inventory 命令与 DTO`

### 2.2 Application Assembler
- **文件**：`claude-j-application/src/main/java/com/claudej/application/inventory/assembler/InventoryAssembler.java`
- **骨架**：
  ```java
  @Mapper(componentModel = "spring")
  public interface InventoryAssembler {
      InventoryDTO toDTO(Inventory inventory);
      List<InventoryDTO> toDTOList(List<Inventory> inventories);
  }
  ```
- **验证命令**：`mvn compile -pl claude-j-application`
- **预期输出**：MapStruct 生成 Impl 类
- **commit**：`feat(application): inventory Assembler`

### 2.3 Application Service + 测试
- **文件**：`claude-j-application/src/main/java/com/claudej/application/inventory/service/InventoryApplicationService.java`
- **测试**：`claude-j-application/src/test/java/com/claudej/application/inventory/service/InventoryApplicationServiceTest.java`
- **骨架**：
  ```java
  @ExtendWith(MockitoExtension.class)
  class InventoryApplicationServiceTest {
      @Mock InventoryRepository inventoryRepository;
      @InjectMocks InventoryApplicationService service;

      @Test
      void should_createInventory_when_commandValid() { ... }
      @Test
      void should_getInventory_when_idExists() { ... }
      @Test
      void should_adjustStock_when_commandValid() { ... }
  }
  ```
- **验证命令**：`mvn test -pl claude-j-application`
- **预期输出**：`Tests run: X, Failures: 0, Errors: 0`
- **commit**：`feat(application): inventory 应用服务`

### 2.4 OrderApplicationService 集成库存
- **文件**：`claude-j-application/src/main/java/com/claudej/application/order/service/OrderApplicationService.java`
- **修改**：
  - 构造函数注入 `InventoryRepository`
  - `createOrder()` 方法：先预占库存
  - `payOrder()` 方法：扣减库存
  - `cancelOrder()` 方法：回滚库存
- **测试**：更新 `OrderApplicationServiceTest.java` 添加库存集成测试
- **验证命令**：`mvn test -pl claude-j-application`
- **预期输出**：Mock 测试通过
- **commit**：`feat(application): Order 集成库存预占/扣减/回滚`

### 3.1 Infrastructure DO + Mapper
- **文件**：
  - `claude-j-infrastructure/src/main/java/com/claudej/infrastructure/inventory/persistence/dataobject/InventoryDO.java`
  - `claude-j-infrastructure/src/main/java/com/claudej/infrastructure/inventory/persistence/mapper/InventoryMapper.java`
- **骨架**：
  ```java
  @Data
  @TableName("t_inventory")
  public class InventoryDO {
      @TableId(type = IdType.AUTO)
      private Long id;
      private String inventoryId;
      private String productId;
      private String skuCode;
      private Integer availableStock;
      private Integer reservedStock;
      private LocalDateTime createTime;
      private LocalDateTime updateTime;
      @TableLogic
      private Integer deleted;
  }

  public interface InventoryMapper extends BaseMapper<InventoryDO> {}
  ```
- **验证命令**：`mvn compile -pl claude-j-infrastructure`
- **预期输出**：编译通过，无 DO 泄漏
- **commit**：`feat(infrastructure): inventory DO 与 Mapper`

### 3.2 Infrastructure Converter
- **文件**：`claude-j-infrastructure/src/main/java/com/claudej/infrastructure/inventory/persistence/converter/InventoryConverter.java`
- **骨架**：
  ```java
  @Component
  public class InventoryConverter {
      public Inventory toDomain(InventoryDO inventoryDO) { ... }
      public InventoryDO toDO(Inventory inventory) { ... }
  }
  ```
- **验证命令**：`mvn compile -pl claude-j-infrastructure`
- **预期输出**：编译通过
- **commit**：`feat(infrastructure): inventory Converter`

### 3.3 Infrastructure Repository 实现 + 测试
- **文件**：`claude-j-infrastructure/src/main/java/com/claudej/infrastructure/inventory/persistence/repository/InventoryRepositoryImpl.java`
- **测试**：`claude-j-infrastructure/src/test/java/com/claudej/infrastructure/inventory/persistence/repository/InventoryRepositoryImplTest.java`
- **骨架**：
  ```java
  @SpringBootTest
  class InventoryRepositoryImplTest {
      @Autowired InventoryRepository inventoryRepository;

      @Test
      void should_save_and_findById() { ... }
      @Test
      void should_findByProductId() { ... }
      @Test
      void should_findBySkuCode() { ... }
  }
  ```
- **验证命令**：`mvn test -pl claude-j-infrastructure -Dtest=InventoryRepositoryImplTest`
- **预期输出**：H2 写入回读一致
- **commit**：`feat(infrastructure): inventory Repository 实现`

### 4.1 Adapter Controller + Request/Response + 测试
- **文件**：
  - `claude-j-adapter/src/main/java/com/claudej/adapter/inventory/web/InventoryController.java`
  - `claude-j-adapter/src/main/java/com/claudej/adapter/inventory/web/request/CreateInventoryRequest.java`
  - `claude-j-adapter/src/main/java/com/claudej/adapter/inventory/web/request/AdjustStockRequest.java`
  - `claude-j-adapter/src/main/java/com/claudej/adapter/inventory/web/response/InventoryResponse.java`
- **测试**：`claude-j-adapter/src/test/java/com/claudej/adapter/inventory/web/InventoryControllerTest.java`
- **骨架**：
  ```java
  @WebMvcTest(InventoryController.class)
  class InventoryControllerTest {
      @MockBean InventoryApplicationService service;
      @Autowired MockMvc mockMvc;

      @Test
      void should_return_200_when_createValid() { ... }
      @Test
      void should_return_400_when_fieldMissing() { ... }
      @Test
      void should_return_404_when_notFound() { ... }
  }
  ```
- **验证命令**：`mvn test -pl claude-j-adapter -Dtest=InventoryControllerTest`
- **预期输出**：HTTP 200/400/404 断言全过
- **commit**：`feat(adapter): inventory REST 端点`

### 5.1 Start DDL
- **文件**：`claude-j-start/src/main/resources/db/migration/V10__add_inventory.sql`
- **验证命令**：H2 集成测试覆盖（Infrastructure 层）
- **预期输出**：表名 `t_inventory`，列名 snake_case
- **commit**：`feat(start): inventory DDL`

## 开发完成记录
<!-- dev 完成后填写 -->
- 全量 `mvn clean test`：待执行
- 架构合规检查：待执行
- 通知 @qa 时间：待执行

## QA 验收记录
<!-- qa 验收后填写 -->
- 全量测试（含集成测试）：待执行
- 代码审查结果：待执行
- 代码风格检查：待执行
- 问题清单：详见 test-report.md
- **最终状态**：待执行