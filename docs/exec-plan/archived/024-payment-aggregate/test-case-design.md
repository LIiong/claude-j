# 测试用例设计 — 024-payment-aggregate

## 测试范围
Payment 聚合验收测试：支付创建、支付回调处理、退款流程，PaymentGateway PSP 抽象接口验证。

## 测试策略
按测试金字塔分层测试 + 代码审查 + 风格检查。

---

## 0. AC 自动化覆盖矩阵（强制，动笔前先填）

| # | 验收条件（AC） | 自动化层 | 对应测试方法 | 手动备注 |
|---|---|---|---|---|
| AC1 | 支付创建成功，状态为 PENDING，返回 paymentId | Domain | `PaymentTest.should_createPayment_when_validParametersProvided` | - |
| AC1 | 支付创建成功，状态为 PENDING，返回 paymentId | Application | `PaymentApplicationServiceTest.should_createPayment_when_validCommand` | - |
| AC1 | 支付创建成功，状态为 PENDING，返回 paymentId | Adapter | `PaymentControllerTest.should_return200_when_createPayment` | - |
| AC2 | 支付回调处理后状态正确流转（SUCCESS/FAILED） | Domain | `PaymentTest.should_markAsSuccess_when_pendingStatus` | - |
| AC2 | 支付回调处理后状态正确流转（SUCCESS/FAILED） | Domain | `PaymentTest.should_markAsFailed_when_pendingStatus` | - |
| AC2 | 支付回调处理后状态正确流转（SUCCESS/FAILED） | Application | `PaymentApplicationServiceTest.should_handleCallback_when_success` | - |
| AC2 | 支付回调处理后状态正确流转（SUCCESS/FAILED） | Application | `PaymentApplicationServiceTest.should_handleCallback_when_failed` | - |
| AC3 | 支付成功后订单状态自动变更为 PAID | Application | `PaymentApplicationServiceTest.should_handleCallback_when_success` | - |
| AC4 | 退款后订单状态变更为 REFUNDED | Application | `PaymentApplicationServiceTest.should_refundPayment_when_successStatus` | - |
| AC5 | MockPaymentGateway 可模拟成功/失败场景 | Infrastructure | `MockPaymentGatewayTest.should_returnSuccess_when_simulateSuccess` | - |
| AC5 | MockPaymentGateway 可模拟成功/失败场景 | Infrastructure | `MockPaymentGatewayTest.should_returnFailed_when_simulateFailed` | - |
| AC6 | 三项预飞（mvn test / checkstyle / entropy-check）全过 | 执行验证 | `mvn clean test && mvn checkstyle:check && ./scripts/entropy-check.sh` | - |
| AC7 | JaCoCo 阈值不下滑（domain 90% / application 80%） | 执行验证 | `mvn jacoco:report` + 阈值检查 | - |

---

## 一、Domain 层测试场景

### PaymentId 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D1 | 合法值创建 | - | new PaymentId(valid) | 创建成功 |
| D2 | null 值 | - | new PaymentId(null) | 抛 BusinessException(PAYMENT_ID_EMPTY) |
| D3 | 空字符串 | - | new PaymentId("") | 抛 BusinessException(PAYMENT_ID_EMPTY) |
| D4 | 相等性 | - | 两个相同值 | equals 返回 true |

### PaymentStatus 状态机枚举
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D5 | PENDING → SUCCESS | PENDING | toSuccess() | 返回 SUCCESS |
| D6 | PENDING → FAILED | PENDING | toFailed() | 返回 FAILED |
| D7 | SUCCESS → REFUNDED | SUCCESS | toRefunded() | 返回 REFUNDED |
| D8 | SUCCESS → SUCCESS 禁止 | SUCCESS | toSuccess() | 抛 BusinessException |
| D9 | FAILED → SUCCESS 禁止 | FAILED | toSuccess() | 抛 BusinessException |
| D10 | REFUNDED → SUCCESS 禁止 | REFUNDED | toSuccess() | 抛 BusinessException |
| D11 | 终态判断 | FAILED/REFUNDED | isTerminal() | 返回 true |

### PaymentMethod 支付方式枚举
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D12 | ALIPAY 枚举值 | - | PaymentMethod.ALIPAY | description = "支付宝" |
| D13 | WECHAT 枚举值 | - | PaymentMethod.WECHAT | description = "微信支付" |
| D14 | BANK_CARD 枚举值 | - | PaymentMethod.BANK_CARD | description = "银行卡" |

### PaymentResult 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D15 | 成功结果 | - | PaymentResult.success(txnNo) | success=true, transactionNo 有值 |
| D16 | 失败结果 | - | PaymentResult.failed(msg) | success=false, message 有值 |
| D17 | 成功结果交易号空 | - | PaymentResult.success(null) | 抛 BusinessException |
| D18 | 失败结果消息空 | - | PaymentResult.failed(null) | 抛 BusinessException |
| D19 | 不可变验证 | - | 检查字段 final | 所有字段 final |
| D20 | 相等性 | - | 两个相同结果 | equals 返回 true |

### Payment 聚合根
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D21 | 创建支付 | - | Payment.create(orderId, customerId, amount, method) | paymentId 生成，status=PENDING |
| D22 | orderId 为空 | - | Payment.create(null, ...) | 抛 BusinessException(PAYMENT_ORDER_ID_EMPTY) |
| D23 | customerId 为空 | - | Payment.create(orderId, null, ...) | 抛 BusinessException(PAYMENT_CUSTOMER_ID_EMPTY) |
| D24 | amount 为空 | - | Payment.create(..., null, ...) | 抛 BusinessException(PAYMENT_AMOUNT_INVALID) |
| D25 | amount 为零 | - | Payment.create(..., Money.cny(0), ...) | 抛 BusinessException(PAYMENT_AMOUNT_INVALID) |
| D26 | method 为空 | - | Payment.create(..., null) | 抛 BusinessException(PAYMENT_METHOD_INVALID) |
| D27 | 支付成功 | PENDING | markAsSuccess(txnNo) | status=SUCCESS, transactionNo 有值 |
| D28 | 支付成功交易号空 | PENDING | markAsSuccess(null) | 抛 BusinessException |
| D29 | 支付失败 | PENDING | markAsFailed(msg) | status=FAILED, transactionNo=null |
| D30 | 支付失败消息空 | PENDING | markAsFailed(null) | 抛 BusinessException |
| D31 | 退款 | SUCCESS | refund() | status=REFUNDED |
| D32 | PENDING 状态退款禁止 | PENDING | refund() | 抛 BusinessException |
| D33 | FAILED 状态退款禁止 | FAILED | refund() | 抛 BusinessException |
| D34 | 重建聚合 | - | Payment.reconstruct(...) | 所有字段正确还原 |
| D35 | setId 持久化回填 | - | payment.setId(1L) | id=1L |
| D36 | 终态判断 | FAILED/REFUNDED | isTerminal() | 返回 true |
| D37 | updateTime 更新 | PENDING | markAsSuccess(txnNo) | updateTime 更新 |

---

## 二、Application 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| A1 | 创建支付正常 | Mock Order 存在，未支付 | createPayment(cmd) | 返回 PaymentDTO，status=PENDING |
| A2 | 创建支付订单不存在 | Mock Order 不存在 | createPayment(cmd) | 抛 BusinessException(ORDER_NOT_FOUND) |
| A3 | 创建支付订单已支付 | Mock Order.isPaid()=true | createPayment(cmd) | 抛 BusinessException(ORDER_ALREADY_PAID) |
| A4 | 创建支付金额无效 | amount<=0 | createPayment(cmd) | 抛 BusinessException(PAYMENT_AMOUNT_INVALID) |
| A5 | 创建支付方式无效 | method 非法 | createPayment(cmd) | 抛 BusinessException(PAYMENT_METHOD_INVALID) |
| A6 | 查询支付正常 | Mock Payment 存在 | getPaymentById(id) | 返回 PaymentDTO |
| A7 | 查询支付不存在 | Mock Payment 不存在 | getPaymentById(id) | 抛 BusinessException(PAYMENT_NOT_FOUND) |
| A8 | 回调处理成功 | Mock Payment PENDING | handleCallback(cmd.success=true) | Payment.status=SUCCESS, Order.pay() 被调用 |
| A9 | 回调处理失败 | Mock Payment PENDING | handleCallback(cmd.success=false) | Payment.status=FAILED |
| A10 | 回调幂等性 | Mock Payment SUCCESS | handleCallback(cmd) | 直接返回当前状态，不重复处理 |
| A11 | 退款正常 | Mock Payment SUCCESS | refundPayment(id, cmd) | Payment.status=REFUNDED, Order.refund() 被调用 |
| A12 | 退款状态不允许 | Mock Payment PENDING | refundPayment(id, cmd) | 抛 BusinessException(PAYMENT_CANNOT_REFUND) |
| A13 | 退款库存回滚 | Mock Order.status=PAID | refundPayment() | Inventory.adjustStock() 被调用 |
| A14 | 退款订单不存在 | Mock Order 不存在 | refundPayment() | 抛 BusinessException(ORDER_NOT_FOUND) |

---

## 三、Infrastructure 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| I1 | 保存并查询 | - | save → findByPaymentId | 返回匹配记录 |
| I2 | 根据 orderId 查询 | - | save → findByOrderId | 返回匹配记录 |
| I3 | 查询不存在 | - | findByPaymentId(notExist) | 返回 empty |
| I4 | existsByPaymentId | - | save → existsByPaymentId | 返回 true |
| I5 | findByCustomerId | - | 保存多条 → findByCustomerId | 返回列表 |
| I6 | DO ↔ Domain 转换 | - | 保存 → 查询 | 所有字段完整还原 |
| I7 | MockPaymentGateway 成功 | simulateSuccess=true | createPayment(payment) | PaymentResult.success=true |
| I8 | MockPaymentGateway 失败 | simulateSuccess=false | createPayment(payment) | PaymentResult.success=false |

---

## 四、Adapter 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| W1 | 创建支付成功 | Mock service 返回 DTO | POST /api/v1/payments | 200 + success=true |
| W2 | 创建支付参数校验 | orderId 为空 | POST /api/v1/payments | 400 + success=false |
| W3 | 查询支付成功 | Mock service 返回 DTO | GET /api/v1/payments/{id} | 200 + success=true |
| W4 | 查询支付不存在 | Mock service 抛异常 | GET /api/v1/payments/{id} | 业务异常 HTTP 状态码 |
| W5 | 支付回调成功 | Mock service 返回 DTO | POST /api/v1/payments/callback | 200 + success=true |
| W6 | 支付回调参数校验 | transactionNo 为空 | POST /api/v1/payments/callback | 400 + success=false |
| W7 | 退款成功 | Mock service 返回 DTO | POST /api/v1/payments/{id}/refund | 200 + success=true |
| W8 | 退款无权限 | 无 ADMIN 角色 | POST /api/v1/payments/{id}/refund | 403 |

---

## 五、集成测试场景（全链路）

| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E1 | Flyway 迁移验证 | 启动应用 + 检查 V11 DDL | t_payment 表存在，索引正确 |
| E2 | Payment API 全链路 | 创建支付 → 回调成功 → 查询 | 状态流转正确，数据一致 |

---

## 六、代码审查检查项

- [x] 依赖方向正确（adapter -> application -> domain <- infrastructure）
- [x] domain 模块无 Spring/框架 import
- [x] 聚合根封装业务不变量（非贫血模型）
- [x] 值对象不可变，equals/hashCode 正确
- [x] Repository 接口在 domain，实现在 infrastructure
- [x] 对象转换链正确：DO <-> Domain <-> DTO <-> Request/Response
- [x] Controller 无业务逻辑
- [x] 异常通过 GlobalExceptionHandler 统一处理

## 七、代码风格检查项

- [x] Java 8 兼容（无 var、records、text blocks、List.of）
- [x] 聚合根用 @Getter，值对象用 @Getter + @EqualsAndHashCode + @ToString
- [x] DO 用 @Data + @TableName，DTO 用 @Data
- [x] 命名规范：PaymentDO, PaymentDTO, PaymentMapper, PaymentRepository, PaymentRepositoryImpl
- [x] 包结构符合 com.claudej.{layer}.payment.{sublayer}
- [x] 测试命名 should_xxx_when_xxx