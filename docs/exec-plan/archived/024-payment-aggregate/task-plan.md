# 任务执行计划 — 024-payment-aggregate

## 任务状态跟踪

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Domain: PaymentId + PaymentMethod + PaymentStatus 值对象 + 测试 | dev | 验收通过 | PaymentIdTest: 8 tests |
| 2 | Domain: PaymentResult 值对象 + 测试 | dev | 验收通过 | PaymentResultTest: 13 tests |
| 3 | Domain: Payment 聚合根 + 测试 | dev | 验收通过 | PaymentTest: 37 tests |
| 4 | Domain: PaymentRepository 端口接口 | dev | 验收通过 | |
| 5 | Domain: PaymentGateway PSP 端口接口 | dev | 验收通过 | |
| 6 | Domain: ErrorCode 新增 Payment 相关错误码 | dev | 验收通过 | |
| 7 | Application: CreatePaymentCommand + RefundPaymentCommand | dev | 验收通过 | |
| 8 | Application: PaymentDTO + PaymentAssembler | dev | 验收通过 | MapStruct |
| 9 | Application: PaymentApplicationService + 测试 | dev | 验收通过 | PaymentApplicationServiceTest: 25 tests |
| 10 | Infrastructure: PaymentDO + PaymentMapper | dev | 验收通过 | |
| 11 | Infrastructure: PaymentConverter | dev | 验收通过 | |
| 12 | Infrastructure: PaymentRepositoryImpl + 测试 | dev | 验收通过 | PaymentRepositoryImplTest: 8 tests |
| 13 | Infrastructure: MockPaymentGateway + 测试 | dev | 验收通过 | MockPaymentGatewayTest: 7 tests |
| 14 | Adapter: CreatePaymentRequest + PaymentCallbackRequest + RefundPaymentRequest | dev | 验收通过 | |
| 15 | Adapter: PaymentResponse | dev | 验收通过 | |
| 16 | Adapter: PaymentController + 测试 | dev | 验收通过 | PaymentControllerTest: 8 tests |
| 17 | Start: V11 DDL + FlywayVerificationTest 更新 | dev | 验收通过 | t_payment 表 |
| 18 | 全量 mvn test | dev | 验收通过 | Tests run: 915 |
| 19 | mvn checkstyle:check | dev | 验收通过 | BUILD SUCCESS |
| 20 | ./scripts/entropy-check.sh | dev | 验收通过 | issues: 0 |
| 21 | QA: 测试用例设计 | qa | 完成 | test-case-design.md |
| 22 | QA: 验收测试 + 代码审查 | qa | 完成 | test-report.md: 验收通过 |

## 执行顺序

domain → application → infrastructure → adapter → start → 全量测试 → QA 验收

## 原子任务分解（每项 10–15 分钟，单会话可完成并 commit）

### 1.1 Domain 值对象 PaymentId
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/payment/model/valobj/PaymentId.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/payment/model/valobj/PaymentIdTest.java`
- **骨架**（Red 阶段先写测试）：
  ```java
  // PaymentIdTest.java
  @Test
  void should_throw_when_value_is_null() { ... }
  @Test
  void should_throw_when_value_is_empty() { ... }
  @Test
  void should_equal_when_values_match() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=PaymentIdTest`
- **预期输出**：`Tests run: 8, Failures: 0, Errors: 0`
- **commit**：`feat(domain): payment 值对象 PaymentId` (done)

### 1.2 Domain 值对象 PaymentMethod
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/payment/model/valobj/PaymentMethod.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/payment/model/valobj/PaymentMethodTest.java`
- **骨架**：
  ```java
  // PaymentMethod.java — 枚举
  ALIPAY("支付宝"), WECHAT("微信支付"), BANK_CARD("银行卡");
  ```
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=PaymentMethodTest`
- **预期输出**：`Tests run: 6, Failures: 0, Errors: 0`
- **commit**：`feat(domain): payment 值对象 PaymentMethod` (done)

### 1.3 Domain 值对象 PaymentStatus
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/payment/model/valobj/PaymentStatus.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/payment/model/valobj/PaymentStatusTest.java`
- **骨架**：
  ```java
  // PaymentStatus.java — 状态机枚举
  PENDING, SUCCESS, FAILED, REFUNDED;
  boolean canSuccess() { return this == PENDING; }
  boolean canFail() { return this == PENDING; }
  boolean canRefund() { return this == SUCCESS; }
  PaymentStatus toSuccess() { ... }
  PaymentStatus toFailed() { ... }
  PaymentStatus toRefunded() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=PaymentStatusTest`
- **预期输出**：`Tests run: 33, Failures: 0, Errors: 0`
- **commit**：`feat(domain): payment 值对象 PaymentStatus` (done)

### 2.1 Domain 值对象 PaymentResult
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/payment/model/valobj/PaymentResult.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/payment/model/valobj/PaymentResultTest.java`
- **骨架**：
  ```java
  // PaymentResult.java — PSP 返回结果
  private final boolean success;
  private final String transactionNo;
  private final String message;
  static PaymentResult success(String transactionNo);
  static PaymentResult failed(String message);
  ```
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=PaymentResultTest`
- **预期输出**：`Tests run: 13, Failures: 0, Errors: 0`
- **commit**：`feat(domain): payment 值对象 PaymentResult` (done)

### 3.1 Domain 聚合根 Payment
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/payment/model/aggregate/Payment.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/payment/model/aggregate/PaymentTest.java`
- **骨架**：
  ```java
  // Payment.java
  static Payment create(OrderId orderId, CustomerId customerId, Money amount, PaymentMethod method);
  static Payment reconstruct(...);
  void markAsSuccess(String transactionNo);
  void markAsFailed(String message);
  void refund();
  ```
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=PaymentTest`
- **预期输出**：`Tests run: 37, Failures: 0, Errors: 0`
- **commit**：`feat(domain): payment 聚合根与状态机` (done)

### 4.1 Domain Repository 端口
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/payment/repository/PaymentRepository.java`
- **骨架**：
  ```java
  Payment save(Payment payment);
  Optional<Payment> findByPaymentId(PaymentId paymentId);
  Optional<Payment> findByOrderId(OrderId orderId);
  boolean existsByPaymentId(PaymentId paymentId);
  List<Payment> findByCustomerId(CustomerId customerId);
  ```
- **验证命令**：`mvn compile -pl claude-j-domain`
- **预期输出**：编译通过
- **commit**：`feat(domain): payment Repository 端口` (done)

### 5.1 Domain PaymentGateway 端口
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/payment/service/PaymentGateway.java`
- **骨架**：
  ```java
  PaymentResult createPayment(Payment payment);
  PaymentResult queryPayment(String transactionNo);
  PaymentResult refundPayment(String transactionNo, Money amount);
  ```
- **验证命令**：`mvn compile -pl claude-j-domain`
- **预期输出**：编译通过
- **commit**：`feat(domain): payment PaymentGateway 端口` (done)

### 6.1 Domain ErrorCode
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/common/exception/ErrorCode.java`
- **骨架**：新增 Payment 相关错误码
  ```java
  PAYMENT_NOT_FOUND, PAYMENT_ALREADY_SUCCESS, PAYMENT_ALREADY_FAILED,
  PAYMENT_ALREADY_REFUNDED, PAYMENT_CANNOT_REFUND, PAYMENT_STATUS_INVALID,
  PAYMENT_ORDER_ID_EMPTY, PAYMENT_AMOUNT_INVALID, PAYMENT_METHOD_INVALID,
  INVALID_PAYMENT_STATUS_TRANSITION
  ```
- **验证命令**：`mvn compile -pl claude-j-domain`
- **预期输出**：编译通过
- **commit**：`feat(domain): payment ErrorCode` (done)

### 7.1 Application Command
- **文件**：
  - `claude-j-application/src/main/java/com/claudej/application/payment/command/CreatePaymentCommand.java`
  - `claude-j-application/src/main/java/com/claudej/application/payment/command/RefundPaymentCommand.java`
- **验证命令**：`mvn compile -pl claude-j-application`
- **预期输出**：编译通过
- **commit**：`feat(application): payment 命令` (done)

### 8.1 Application DTO + Assembler
- **文件**：
  - `claude-j-application/src/main/java/com/claudej/application/payment/dto/PaymentDTO.java`
  - `claude-j-application/src/main/java/com/claudej/application/payment/assembler/PaymentAssembler.java`
- **骨架**：MapStruct `@Mapper(componentModel = "spring")`
- **验证命令**：`mvn compile -pl claude-j-application`
- **预期输出**：编译通过，生成 Impl 类
- **commit**：`feat(application): payment DTO 与 Assembler` (done)

### 9.1 Application Service + 测试
- **文件**：`claude-j-application/src/main/java/com/claudej/application/payment/service/PaymentApplicationService.java`
- **测试**：`claude-j-application/src/test/java/com/claudej/application/payment/service/PaymentApplicationServiceTest.java`
- **骨架**：
  ```java
  @Service
  PaymentDTO createPayment(CreatePaymentCommand command);
  PaymentDTO getPaymentById(String paymentId);
  PaymentDTO getPaymentByOrderId(String orderId);
  PaymentDTO handleCallback(PaymentCallbackCommand command);
  PaymentDTO refundPayment(String paymentId, RefundPaymentCommand command);
  ```
- **验证命令**：`mvn test -pl claude-j-application -Dtest=PaymentApplicationServiceTest`
- **预期输出**：`Tests run: 25, Failures: 0, Errors: 0`
- **commit**：`feat(application): payment 应用服务` (done)

### 10.1 Infrastructure DO + Mapper
- **文件**：
  - `claude-j-infrastructure/src/main/java/com/claudej/infrastructure/payment/persistence/dataobject/PaymentDO.java`
  - `claude-j-infrastructure/src/main/java/com/claudej/infrastructure/payment/persistence/mapper/PaymentMapper.java`
- **骨架**：`@TableName("t_payment")` + `BaseMapper<PaymentDO>`
- **验证命令**：`mvn compile -pl claude-j-infrastructure`
- **预期输出**：编译通过
- **commit**：`feat(infrastructure): payment DO 与 Mapper` (done)

### 11.1 Infrastructure Converter
- **文件**：`claude-j-infrastructure/src/main/java/com/claudej/infrastructure/payment/persistence/converter/PaymentConverter.java`
- **骨架**：`toDomain(PaymentDO)` + `toDO(Payment)`
- **验证命令**：`mvn compile -pl claude-j-infrastructure`
- **预期输出**：编译通过
- **commit**：`feat(infrastructure): payment Converter` (done)

### 12.1 Infrastructure Repository 实现 + 测试
- **文件**：`claude-j-infrastructure/src/main/java/com/claudej/infrastructure/payment/persistence/repository/PaymentRepositoryImpl.java`
- **测试**：`claude-j-infrastructure/src/test/java/com/claudej/infrastructure/payment/persistence/repository/PaymentRepositoryImplTest.java`
- **骨架**：`@Repository` + H2 集成测试
- **验证命令**：`mvn test -pl claude-j-infrastructure -Dtest=PaymentRepositoryImplTest`
- **预期输出**：`Tests run: 8, Failures: 0, Errors: 0`
- **commit**：`feat(infrastructure): payment Repository 实现` (done)

### 13.1 Infrastructure MockPaymentGateway
- **文件**：`claude-j-infrastructure/src/main/java/com/claudej/infrastructure/payment/gateway/MockPaymentGateway.java`
- **测试**：`claude-j-infrastructure/src/test/java/com/claudej/infrastructure/payment/gateway/MockPaymentGatewayTest.java`
- **骨架**：可配置 success/failed 返回
- **验证命令**：`mvn test -pl claude-j-infrastructure -Dtest=MockPaymentGatewayTest`
- **预期输出**：`Tests run: 7, Failures: 0, Errors: 0`
- **commit**：`feat(infrastructure): payment MockPaymentGateway` (done)

### 14.1 Adapter Request
- **文件**：
  - `claude-j-adapter/src/main/java/com/claudej/adapter/payment/web/request/CreatePaymentRequest.java`
  - `claude-j-adapter/src/main/java/com/claudej/adapter/payment/web/request/PaymentCallbackRequest.java`
  - `claude-j-adapter/src/main/java/com/claudej/adapter/payment/web/request/RefundPaymentRequest.java`
- **验证命令**：`mvn compile -pl claude-j-adapter`
- **预期输出**：编译通过
- **commit**：`feat(adapter): payment Request` (done)

### 15.1 Adapter Response
- **文件**：`claude-j-adapter/src/main/java/com/claudej/adapter/payment/web/response/PaymentResponse.java`
- **验证命令**：`mvn compile -pl claude-j-adapter`
- **预期输出**：编译通过
- **commit**：`feat(adapter): payment Response` (done)

### 16.1 Adapter Controller + 测试
- **文件**：`claude-j-adapter/src/main/java/com/claudej/adapter/payment/web/PaymentController.java`
- **测试**：`claude-j-adapter/src/test/java/com/claudej/adapter/payment/web/PaymentControllerTest.java`
- **骨架**：
  ```java
  @RestController @RequestMapping("/api/v1/payments")
  POST / — 创建支付
  GET /{paymentId} — 查询支付
  GET /orders/{orderId} — 查询订单支付
  POST /callback — 支付回调
  POST /{paymentId}/refund — 退款（ADMIN）
  ```
- **验证命令**：`mvn test -pl claude-j-adapter -Dtest=PaymentControllerTest`
- **预期输出**：`Tests run: 8, Failures: 0, Errors: 0`
- **commit**：`feat(adapter): payment REST 端点` (done)

### 17.1 Start DDL
- **文件**：`claude-j-start/src/main/resources/db/migration/V11__add_payment.sql`
- **骨架**：新增 t_payment 表
- **验证命令**：FlywayVerificationTest
- **预期输出**：表名 `t_payment`，列名 snake_case
- **commit**：`feat(start): payment DDL` (done)

## 开发完成记录

<!-- dev 完成后填写 -->
- 全量 `mvn clean test`：Tests run: 915, Failures: 0, Errors: 0, Skipped: 0
- 架构合规检查：entropy-check issues: 0, warnings: 13
- 通知 @qa 时间：2026-04-25

## QA 验收记录

<!-- qa 验收后填写 -->
- 全量测试（含集成测试）：Tests run: 915, Failures: 0, Errors: 0
- 代码审查结果：依赖方向正确、聚合根封装不变量、值对象不可变、DO 未泄漏
- 代码风格检查：0 Checkstyle violations
- 问题清单：无问题
- **最终状态**：验收通过