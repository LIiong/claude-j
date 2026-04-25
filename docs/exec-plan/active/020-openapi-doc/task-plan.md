# 任务执行计划 — 020-openapi-doc

## 任务状态跟踪

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Start: 添加 springdoc-openapi-ui 依赖 | dev | 完成 | |
| 2 | Adapter: ShortLinkRedirectController @Tag + @Operation | dev | 完成 | |
| 3 | Adapter: LinkController @Tag + @Operation | dev | 完成 | |
| 4 | Adapter: UserOrderController @Tag + @Operation | dev | 完成 | |
| 5 | Adapter: AuthController 补充其他方法 @Operation | dev | 完成 | |
| 6 | Adapter: CartController 所有方法 @Operation | dev | 完成 | |
| 7 | Adapter: CouponController 所有方法 @Operation | dev | 完成 | |
| 8 | Adapter: OrderController 所有方法 @Operation | dev | 完成 | |
| 9 | Adapter: UserController 所有方法 @Operation | dev | 完成 | |
| 10 | 全量 mvn test + checkstyle + entropy-check | dev | 完成 | |
| 11 | QA: 测试用例设计 | qa | 待办 | |
| 12 | QA: 验收测试 + 代码审查 | qa | 待办 | |

## 执行顺序

start → adapter（按 Controller 逐个） → 全量测试 → QA 验收

## 原子任务分解

> **本任务特点**：纯配置型任务，不涉及 domain/application/infrastructure 层，无需 TDD（OpenAPI 注解无业务逻辑需测试）。
> **验证方式**：启动应用后手工访问 swagger-ui.html + /v3/api-docs，确认注解生效。

### 1.1 Start 添加依赖
- **文件**：`claude-j-start/pom.xml`
- **骨架**：
  ```xml
  <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-ui</artifactId>
  </dependency>
  ```
- **验证命令**：`mvn compile -pl claude-j-start`
- **预期输出**：编译通过，无依赖冲突
- **commit**：`feat(start): add springdoc-openapi-ui dependency`

### 2.1 Adapter ShortLinkRedirectController
- **文件**：`claude-j-adapter/src/main/java/com/claudej/adapter/shortlink/web/ShortLinkRedirectController.java`
- **骨架**：
  ```java
  import io.swagger.v3.oas.annotations.Operation;
  import io.swagger.v3.oas.annotations.tags.Tag;

  @Tag(name = "短链跳转", description = "短链接跳转服务")
  @Controller
  @RequestMapping("/s")
  public class ShortLinkRedirectController {
      @Operation(summary = "短链跳转", description = "根据短码跳转到原始长链接")
      @GetMapping("/{shortCode}")
      public ResponseEntity<Void> redirect(...) { }
  }
  ```
- **验证命令**：`mvn compile -pl claude-j-adapter`
- **预期输出**：编译通过
- **commit**：`feat(adapter): add OpenAPI annotations to ShortLinkRedirectController`

### 3.1 Adapter LinkController
- **文件**：`claude-j-adapter/src/main/java/com/claudej/adapter/link/web/LinkController.java`
- **骨架**：
  ```java
  import io.swagger.v3.oas.annotations.Operation;
  import io.swagger.v3.oas.annotations.tags.Tag;

  @Tag(name = "链接管理", description = "链接的创建、更新、删除、查询")
  @RestController
  @RequestMapping("/api/v1/links")
  public class LinkController {
      @Operation(summary = "创建链接", description = "创建一个新的链接记录")
      @PostMapping
      public ApiResult<LinkResponse> createLink(...) { }

      @Operation(summary = "更新链接", description = "根据ID更新链接信息")
      @PutMapping("/{id}")
      public ApiResult<LinkResponse> updateLink(...) { }

      // 其他方法类似...
  }
  ```
- **验证命令**：`mvn compile -pl claude-j-adapter`
- **预期输出**：编译通过
- **commit**：`feat(adapter): add OpenAPI annotations to LinkController`

### 4.1 Adapter UserOrderController
- **文件**：`claude-j-adapter/src/main/java/com/claudej/adapter/user/web/UserOrderController.java`
- **骨架**：
  ```java
  import io.swagger.v3.oas.annotations.Operation;
  import io.swagger.v3.oas.annotations.tags.Tag;

  @Tag(name = "用户订单", description = "查询用户的订单列表和详情")
  @RestController
  @RequestMapping("/api/v1/users/{userId}/orders")
  public class UserOrderController {
      @Operation(summary = "查询用户订单列表", description = "根据用户ID查询该用户的所有订单")
      @GetMapping
      public ApiResult<List<OrderResponse>> getUserOrders(...) { }

      @Operation(summary = "分页查询用户订单", description = "分页查询用户的订单列表")
      @GetMapping("/paged")
      public ApiResult<PageResponse<OrderResponse>> getUserOrdersPaged(...) { }

      @Operation(summary = "查询用户订单详情", description = "查询指定订单的详细信息")
      @GetMapping("/{orderId}")
      public ApiResult<OrderResponse> getUserOrderDetail(...) { }
  }
  ```
- **验证命令**：`mvn compile -pl claude-j-adapter`
- **预期输出**：编译通过
- **commit**：`feat(adapter): add OpenAPI annotations to UserOrderController`

### 5.1 Adapter AuthController 补充注解
- **文件**：`claude-j-adapter/src/main/java/com/claudej/adapter/auth/web/AuthController.java`
- **骨架**：为以下方法添加 @Operation：
  - login：`@Operation(summary = "用户登录", description = "使用账号密码登录")`
  - loginBySms：`@Operation(summary = "短信验证码登录", description = "使用手机号和短信验证码登录")`
  - logout：`@Operation(summary = "用户登出", description = "退出当前登录会话")`
  - refreshToken：`@Operation(summary = "刷新Token", description = "使用refreshToken获取新的accessToken")`
  - changePassword：`@Operation(summary = "修改密码", description = "修改用户密码")`
  - resetPassword：`@Operation(summary = "重置密码", description = "通过邮箱验证码重置密码")`
  - getAuthUser：`@Operation(summary = "获取认证用户信息", description = "根据用户ID获取认证相关信息")`
- **验证命令**：`mvn compile -pl claude-j-adapter`
- **预期输出**：编译通过
- **commit**：`feat(adapter): add Operation annotations to AuthController methods`

### 6.1 Adapter CartController
- **文件**：`claude-j-adapter/src/main/java/com/claudej/adapter/cart/web/CartController.java`
- **骨架**：为以下方法添加 @Operation：
  - addItem：`@Operation(summary = "添加商品到购物车", description = "将商品添加到用户购物车")`
  - updateItemQuantity：`@Operation(summary = "更新商品数量", description = "修改购物车中商品的数量")`
  - removeItem：`@Operation(summary = "删除购物车商品", description = "从购物车中移除指定商品")`
  - clearCart：`@Operation(summary = "清空购物车", description = "清空用户购物车中的所有商品")`
  - getCart：`@Operation(summary = "查询购物车", description = "获取用户购物车详情")`
- **验证命令**：`mvn compile -pl claude-j-adapter`
- **预期输出**：编译通过
- **commit**：`feat(adapter): add Operation annotations to CartController`

### 7.1 Adapter CouponController
- **文件**：`claude-j-adapter/src/main/java/com/claudej/adapter/coupon/web/CouponController.java`
- **骨架**：为以下方法添加 @Operation：
  - createCoupon：`@Operation(summary = "创建优惠券", description = "为用户创建一张优惠券")`
  - getCouponById：`@Operation(summary = "根据ID查询优惠券", description = "根据优惠券ID获取优惠券详情")`
  - getCouponsByUserId：`@Operation(summary = "查询用户优惠券列表", description = "获取用户的所有优惠券")`
  - getAvailableCouponsByUserId：`@Operation(summary = "查询用户可用优惠券", description = "获取用户当前可使用的优惠券")`
  - getCouponsByUserIdPaged：`@Operation(summary = "分页查询用户优惠券", description = "分页获取用户的优惠券列表")`
  - getAvailableCouponsByUserIdPaged：`@Operation(summary = "分页查询可用优惠券", description = "分页获取用户可用的优惠券")`
  - useCoupon：`@Operation(summary = "使用优惠券", description = "在订单中使用指定优惠券")`
- **验证命令**：`mvn compile -pl claude-j-adapter`
- **预期输出**：编译通过
- **commit**：`feat(adapter): add Operation annotations to CouponController`

### 8.1 Adapter OrderController
- **文件**：`claude-j-adapter/src/main/java/com/claudej/adapter/order/web/OrderController.java`
- **骨架**：为以下方法添加 @Operation：
  - createOrder：`@Operation(summary = "创建订单", description = "创建一个新的订单")`
  - getOrderById：`@Operation(summary = "查询订单详情", description = "根据订单ID查询订单详情")`
  - getOrdersByCustomerId：`@Operation(summary = "查询客户订单列表", description = "根据客户ID查询其所有订单")`
  - getOrdersByCustomerIdPaged：`@Operation(summary = "分页查询客户订单", description = "分页查询客户的订单列表")`
  - payOrder：`@Operation(summary = "支付订单", description = "对订单进行支付操作")`
  - cancelOrder：`@Operation(summary = "取消订单", description = "取消未支付的订单")`
  - createOrderFromCart：`@Operation(summary = "从购物车创建订单", description = "将购物车商品转换为订单")`
  - shipOrder：`@Operation(summary = "发货", description = "订单发货操作")`
  - deliverOrder：`@Operation(summary = "确认送达", description = "确认订单已送达")`
  - refundOrder：`@Operation(summary = "退款", description = "订单退款操作")`
- **验证命令**：`mvn compile -pl claude-j-adapter`
- **预期输出**：编译通过
- **commit**：`feat(adapter): add Operation annotations to OrderController`

### 9.1 Adapter UserController
- **文件**：`claude-j-adapter/src/main/java/com/claudej/adapter/user/web/UserController.java`
- **骨架**：为以下方法添加 @Operation：
  - createUser：`@Operation(summary = "创建用户", description = "创建新用户账号")`
  - getUserById：`@Operation(summary = "根据ID查询用户", description = "根据用户ID获取用户信息")`
  - getUserByUsername：`@Operation(summary = "根据用户名查询", description = "根据用户名获取用户信息")`
  - freezeUser：`@Operation(summary = "冻结用户", description = "冻结指定用户账号")`
  - unfreezeUser：`@Operation(summary = "解冻用户", description = "解冻被冻结的用户账号")`
  - getInvitedUsers：`@Operation(summary = "查询被邀请用户", description = "查询用户邀请的所有用户")`
  - getInvitedUsersPaged：`@Operation(summary = "分页查询被邀请用户", description = "分页查询用户邀请的用户列表")`
  - validateInviteCode：`@Operation(summary = "验证邀请码", description = "验证邀请码是否有效")`
- **验证命令**：`mvn compile -pl claude-j-adapter`
- **预期输出**：编译通过
- **commit**：`feat(adapter): add Operation annotations to UserController`

### 10.1 全量验证
- **验证命令**：
  ```bash
  mvn test
  mvn checkstyle:check
  ./scripts/entropy-check.sh
  ```
- **预期输出**：三项全部通过
- **commit**：`docs: 020-openapi-doc 任务完成`（仅在所有任务完成后）

## 开发完成记录
<!-- dev 完成后填写 -->
- 全量 `mvn clean test`：Tests run: 59, Failures: 0, Errors: 0 - BUILD SUCCESS
- 架构合规检查：mvn checkstyle:check: 0 violations - BUILD SUCCESS; entropy-check: 0 FAIL, 12 WARN - PASS
- 通知 @qa 时间：2026-04-25

## QA 验收记录
<!-- qa 验收后填写 -->
- 全量测试（含集成测试）：待填写
- 代码审查结果：待填写
- 代码风格检查：待填写
- 问题清单：详见 test-report.md
- **最终状态**：待填写