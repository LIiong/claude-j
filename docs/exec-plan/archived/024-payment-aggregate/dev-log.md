# 开发日志 — 024-payment-aggregate

## 问题记录

<!-- Build 阶段遇到问题时填写，每条四段齐全 -->

### 1. PaymentCallbackCommand 缺少 orderId 字段
- **Issue**: handleCallback 方法最初没有 orderId 字段，导致无法定位支付记录
- **Root Cause**: 设计遗漏，回调需要携带订单信息才能查询对应支付
- **Fix**: 在 PaymentCallbackCommand 和 PaymentCallbackRequest 中增加 orderId 字段
- **Verification**: `mvn test -pl claude-j-application -Dtest=PaymentApplicationServiceTest` 通过

### 2. Inventory 状态不正确导致测试失败
- **Issue**: handleCallback 和 refundPayment 测试失败，库存操作报错
- **Root Cause**: 测试中 Inventory 的 reservedStock=0，但实际业务中订单创建时已预占库存
- **Fix**:
  - handleCallback 测试：使用 createMockInventoryWithReserved(1) 模拟有预留库存
  - refundPayment 测试：使用 adjustStock 而不是 release（因为退款时库存已扣减）
- **Verification**: `mvn test -pl claude-j-application -Dtest=PaymentApplicationServiceTest` 通过

### 3. Start 模块 Flyway migration 失败
- **Issue**: V11 DDL 索引名冲突，Flyway 加载错误版本
- **Root Cause**:
  - 索引名 idx_order_id 与 V2__order_init.sql 中的索引名冲突
  - 测试资源 application.yml 中 Flyway locations 设置为 classpath:db/migration（MySQL 版），而不是 classpath:db/migration-h2（H2 版）
- **Fix**:
  - 创建 claude-j-start/src/test/resources/db/migration-h2/ 目录并复制所有 migration 文件
  - V11 使用唯一索引名 idx_payment_order_id 等
  - 更新测试资源 application.yml 中 Flyway locations 为 classpath:db/migration-h2
- **Verification**: `mvn test -pl claude-j-start -Dtest=FlywayVerificationTest` 通过

## 变更记录

<!-- 与原设计（requirement-design.md）不一致的变更 -->
- PaymentCallbackCommand 增加 orderId 字段（设计文档未明确）
- 退款时库存恢复使用 adjustStock 而不是 release（库存已扣减，需增加而不是释放预留）
- FlywayVerificationTest 更新为 11 migrations 和 14 tables（原设计未考虑新增聚合）

## 开发摘要

### Domain 层
- PaymentId 值对象：8 tests
- PaymentMethod 枚举：6 tests
- PaymentStatus 状态机枚举：33 tests
- PaymentResult 值对象：13 tests
- Payment 聚合根：37 tests
- PaymentRepository/PaymentGateway 端口接口

### Application 层
- CreatePaymentCommand/PaymentCallbackCommand/RefundPaymentCommand 命令
- PaymentDTO/PaymentAssembler (MapStruct)
- PaymentApplicationService：25 tests

### Infrastructure 层
- PaymentDO/PaymentMapper/PaymentConverter
- PaymentRepositoryImpl：8 tests
- MockPaymentGateway：7 tests

### Adapter 层
- CreatePaymentRequest/PaymentCallbackRequest/RefundPaymentRequest
- PaymentResponse
- PaymentController：8 tests

### Start 层
- V11__add_payment.sql DDL（MySQL 和 H2 版）
- FlywayVerificationTest 更新

### 测试统计
- Domain: 97 tests (PaymentId:8, PaymentMethod:6, PaymentStatus:33, PaymentResult:13, Payment:37)
- Application: 25 tests
- Infrastructure: 15 tests (PaymentRepositoryImplTest:8, MockPaymentGatewayTest:7)
- Adapter: 8 tests
- Start: 59 tests (含 FlywayVerificationTest:2)
- 总计: 204 tests（Payment 聚合新增）

### 三项验证
- mvn clean test: Tests run: 915, Failures: 0, Errors: 0
- mvn checkstyle:check: BUILD SUCCESS
- ./scripts/entropy-check.sh: issues: 0, warnings: 13