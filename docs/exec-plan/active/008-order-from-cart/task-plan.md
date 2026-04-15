# 任务执行计划 — 008-order-from-cart

## 任务状态跟踪

<!-- 状态流转：待办 → 进行中 → 单测通过 → 待验收 → 验收通过 / 待修复 -->

| # | 任务 | 负责人 | 状态 | 复杂度 | 备注 |
|---|------|--------|------|--------|------|
| 1 | ErrorCode: 添加 CART_EMPTY 错误码 | dev | 待办 | S | 仅添加枚举值 |
| 2 | Application: CreateOrderFromCartCommand | dev | 待办 | S | 命令对象 |
| 3 | Application: OrderApplicationService.createOrderFromCart | dev | 待办 | M | 核心逻辑，需注入 CartRepository |
| 4 | Application: OrderApplicationService 单元测试 | dev | 待办 | M | 覆盖正常/异常场景 |
| 5 | Adapter: CreateOrderFromCartRequest | dev | 待办 | S | 请求对象 + @Valid |
| 6 | Adapter: OrderController.createOrderFromCart | dev | 待办 | S | 端点实现 |
| 7 | Adapter: OrderController 单元测试 | dev | 待办 | M | MockMvc 测试 |
| 8 | Start: OrderFromCartIntegrationTest | dev | 待办 | M | 全链路集成测试 |
| 9 | 全量 mvn test | dev | 待办 | - | 含 ArchUnit 架构规则 |
| 10 | QA: 测试用例设计 | qa | 待办 | - | |
| 11 | QA: 验收测试 + 代码审查 | qa | 待办 | - | |
| 12 | QA: 接口集成测试 | qa | 待办 | - | |

## 任务分解详情

### 1. ErrorCode: 添加 CART_EMPTY 错误码
- **文件**: `claude-j-domain/src/main/java/com/claudej/domain/common/exception/ErrorCode.java`
- **内容**: 在 Cart Management 区域添加 `CART_EMPTY("CART_EMPTY", "购物车为空，无法下单")`
- **复杂度**: S

### 2. Application: CreateOrderFromCartCommand
- **文件**: `claude-j-application/src/main/java/com/claudej/application/order/command/CreateOrderFromCartCommand.java`
- **字段**: customerId (String, 必填), couponId (String, 可选)
- **复杂度**: S

### 3. Application: OrderApplicationService.createOrderFromCart
- **文件**: `claude-j-application/src/main/java/com/claudej/application/order/service/OrderApplicationService.java`
- **依赖注入**: 新增 `CartRepository` 构造器注入
- **核心逻辑**:
  ```
  @Transactional
  1. 通过 cartRepository.findByUserId(command.getCustomerId()) 查询购物车
  2. 验证购物车存在，否则抛出 CART_NOT_FOUND
  3. 验证购物车非空，否则抛出 CART_EMPTY
  4. 创建订单: Order.create(customerId)
  5. 遍历 cart.getItems()，转换为 OrderItem 并 addItem 到订单
  6. 保存订单: orderRepository.save(order)
  7. 清空购物车: cart.clear()
  8. 保存购物车: cartRepository.save(cart)
  9. 返回 OrderDTO
  ```
- **复杂度**: M

### 4. Application: 单元测试
- **文件**: `claude-j-application/src/test/java/com/claudej/application/order/service/OrderApplicationServiceTest.java`
- **测试场景**:
  - should_createOrderFromCart_when_cartExistsWithItems
  - should_throwBusinessException_when_cartNotFound
  - should_throwBusinessException_when_cartIsEmpty
  - should_clearCartAfterOrderCreation
  - should_calculateCorrectTotal_when_multipleItems
- **复杂度**: M

### 5. Adapter: CreateOrderFromCartRequest
- **文件**: `claude-j-adapter/src/main/java/com/claudej/adapter/order/web/request/CreateOrderFromCartRequest.java`
- **字段**: customerId (@NotBlank), couponId (可选)
- **复杂度**: S

### 6. Adapter: OrderController.createOrderFromCart
- **文件**: `claude-j-adapter/src/main/java/com/claudej/adapter/order/web/OrderController.java`
- **内容**:
  ```java
  @PostMapping("/from-cart")
  public ApiResult<OrderResponse> createOrderFromCart(@Valid @RequestBody CreateOrderFromCartRequest request) {
      CreateOrderFromCartCommand command = new CreateOrderFromCartCommand();
      command.setCustomerId(request.getCustomerId());
      command.setCouponId(request.getCouponId());
      OrderDTO dto = orderApplicationService.createOrderFromCart(command);
      return ApiResult.ok(convertToResponse(dto));
  }
  ```
- **复杂度**: S

### 7. Adapter: 单元测试
- **文件**: `claude-j-adapter/src/test/java/com/claudej/adapter/order/web/OrderControllerTest.java`
- **测试场景**:
  - should_return200_when_createOrderFromCart_success
  - should_return400_when_customerIdIsBlank
  - should_return404_when_cartNotFound
  - should_return400_when_cartIsEmpty
- **复杂度**: M

### 8. Start: 集成测试
- **文件**: `claude-j-start/src/test/java/com/claudej/start/order/OrderFromCartIntegrationTest.java`
- **场景**: 完整链路测试（准备购物车数据 -> 调用API -> 验证订单创建 -> 验证购物车清空）
- **复杂度**: M

## 执行顺序

```
ErrorCode
  ↓
Application Command → Application Service
  ↓
Application Tests (Mockito)
  ↓
Adapter Request → Adapter Controller
  ↓
Adapter Tests (MockMvc)
  ↓
Start Integration Test
  ↓
全量 mvn test → checkstyle → entropy-check
  ↓
QA 验收
```

## 依赖关系

```
Task 1 (ErrorCode)
  ↓
Task 2 (Command) → Task 3 (Service)
  ↓                 ↓
Task 5 (Request) → Task 6 (Controller) → Task 4 (App Tests)
  ↓                                      ↓
Task 7 (Adapter Tests) ←─────────────────┘
  ↓
Task 8 (Integration Test)
  ↓
Task 9 (全量测试)
```

## 测试策略

### Domain 层
- **无需变更**: 复用现有 Order、Cart 聚合能力，无新增 domain 代码

### Application 层
- **Mockito 测试**: Mock CartRepository + OrderRepository
- **验证点**: 编排顺序、事务边界、异常场景

### Adapter 层
- **MockMvc 测试**: Mock OrderApplicationService
- **验证点**: HTTP 路径、请求校验、响应格式、异常码映射

### Infrastructure 层
- **无需变更**: 复用现有 Repository 实现

### Start 层
- **集成测试**: 使用 H2 数据库，完整链路验证
- **数据准备**: 通过 Repository 直接准备购物车数据
- **验证点**: 订单创建成功、购物车被清空、数据一致性

## 开发完成记录
<!-- dev 完成后填写 -->
- 全量 `mvn clean test`：x/x 用例通过
- 架构合规检查：
- 通知 @qa 时间：

## QA 验收记录
<!-- qa 验收后填写 -->
- 全量测试（含集成测试）：x/x 用例通过
- 代码审查结果：
- 代码风格检查：
- 问题清单：详见 test-report.md
- **最终状态**：
