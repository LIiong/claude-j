# 任务执行计划 — 015-order-coupon-integration

## 任务状态跟踪

<!-- 状态流转：待办 → 进行中 → 单测通过 → 待验收 → 验收通过 / 待修复 -->

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Domain: Order 聚合添加 coupon 相关字段和方法 | dev | 单测通过 | 含 applyCoupon, removeCoupon |
| 2 | Domain: Coupon 聚合添加 unuse() 方法 | dev | 单测通过 | 状态回滚逻辑 |
| 3 | Domain: Order/Coupon 领域层单元测试 | dev | 单测通过 | JUnit 5 + AssertJ |
| 4 | Application: 扩展 Command 和 DTO | dev | 单测通过 | couponId 字段 |
| 5 | Application: 扩展 OrderAssembler | dev | 单测通过 | 映射新字段 |
| 6 | Application: OrderApplicationService 优惠券集成 | dev | 单测通过 | 验证/计算/核销/回滚 |
| 7 | Application: 应用层单元测试（Mockito） | dev | 单测通过 | Mock CouponRepository |
| 8 | Infrastructure: OrderDO 扩展字段 | dev | 单测通过 | coupon_id, discount_amount, final_amount |
| 9 | Infrastructure: OrderConverter 更新 | dev | 单测通过 | 往返映射 |
| 10 | Infrastructure: Repository 实现更新 | dev | 单测通过 | 保存新字段 |
| 11 | Infrastructure: 持久化层集成测试（H2） | dev | 单测通过 | DO↔Domain 往返验证 |
| 12 | Start: schema.sql DDL 更新 | dev | 单测通过 | t_order 表结构 |
| 13 | 全量 mvn test 验证 | dev | 单测通过 | 含 ArchUnit |
| 14 | 代码风格检查 | dev | 单测通过 | checkstyle |
| 15 | 熵检查 | dev | 单测通过 | entropy-check.sh |
| 16 | QA: 测试用例设计 | qa | 完成 | test-case-design.md 已生成 |
| 17 | QA: 验收测试 + 代码审查 | qa | 完成 | 448 测试通过，0 阻塞性问题 |
| 18 | QA: 集成测试（完整流程） | qa | 完成 | 下单→支付→取消流程验证通过 |

## 执行顺序

domain → application → infrastructure → start → 全量测试 → QA 验收

## 原子任务分解（每项 10–15 分钟，单会话可完成并 commit）

### 1.1 Domain: Order 聚合扩展
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/order/model/aggregate/Order.java`
- **变更**：
  - 新增字段：couponId (CouponId), discountAmount (Money), finalAmount (Money)
  - 新增方法：applyCoupon(CouponId, Money), removeCoupon(), getFinalAmount()
  - 修改 reconstruct() 工厂方法接收新参数
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=OrderTest`
- **预期输出**：测试通过，覆盖新增方法的不变量校验
- **commit**：`feat(domain): Order 聚合添加优惠券相关字段和方法`

### 1.2 Domain: Coupon 聚合添加 unuse() 方法
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/coupon/model/aggregate/Coupon.java`
- **变更**：
  - 新增方法：unuse() — 将状态从 USED 回滚到 AVAILABLE
  - 清除 usedTime 和 usedOrderId
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=CouponTest`
- **预期输出**：测试通过，覆盖状态回滚和异常场景
- **commit**：`feat(domain): Coupon 聚合添加 unuse() 方法支持订单取消回滚`

### 2.1 Application: Command 和 DTO 扩展
- **文件**：
  - `claude-j-application/src/main/java/com/claudej/application/order/command/CreateOrderCommand.java`
  - `claude-j-application/src/main/java/com/claudej/application/order/dto/OrderDTO.java`
- **变更**：
  - CreateOrderCommand: 新增可选字段 couponId
  - OrderDTO: 新增字段 couponId, discountAmount, finalAmount
- **验证命令**：`mvn compile -pl claude-j-application`
- **预期输出**：编译通过
- **commit**：`feat(application): Order Command 和 DTO 添加优惠券字段`

### 2.2 Application: OrderAssembler 更新
- **文件**：`claude-j-application/src/main/java/com/claudej/application/order/assembler/OrderAssembler.java`
- **变更**：
  - 更新 toDTO() 方法，映射 couponId, discountAmount, finalAmount
- **验证命令**：`mvn compile -pl claude-j-application`
- **预期输出**：编译通过
- **commit**：`feat(application): OrderAssembler 映射优惠券字段`

### 2.3 Application: OrderApplicationService 优惠券集成
- **文件**：`claude-j-application/src/main/java/com/claudej/application/order/service/OrderApplicationService.java`
- **变更**：
  - 注入 CouponRepository
  - createOrder(): 添加优惠券验证、计算折扣、应用优惠券
  - createOrderFromCart(): 同上
  - payOrder(): 添加优惠券核销逻辑
  - cancelOrder(): 添加优惠券回滚逻辑
- **验证命令**：`mvn test -pl claude-j-application`
- **预期输出**：Mockito 测试通过
- **commit**：`feat(application): OrderApplicationService 集成优惠券验证与核销`

### 3.1 Infrastructure: OrderDO 扩展
- **文件**：`claude-j-infrastructure/src/main/java/com/claudej/infrastructure/order/persistence/dataobject/OrderDO.java`
- **变更**：
  - 新增字段：couponId, discountAmount, finalAmount
- **验证命令**：`mvn compile -pl claude-j-infrastructure`
- **预期输出**：编译通过
- **commit**：`feat(infrastructure): OrderDO 添加优惠券相关字段`

### 3.2 Infrastructure: OrderConverter 更新
- **文件**：`claude-j-infrastructure/src/main/java/com/claudej/infrastructure/order/persistence/converter/OrderConverter.java`
- **变更**：
  - 更新 toDomain(): 从 DO 重建 couponId, discountAmount, finalAmount
  - 更新 toDO(): 将 Domain 字段写入 DO
- **验证命令**：`mvn compile -pl claude-j-infrastructure`
- **预期输出**：编译通过
- **commit**：`feat(infrastructure): OrderConverter 支持优惠券字段映射`

### 3.3 Infrastructure: Repository 实现更新
- **文件**：`claude-j-infrastructure/src/main/java/com/claudej/infrastructure/order/persistence/repository/OrderRepositoryImpl.java`
- **变更**：
  - 修改 save() 方法，确保新字段正确保存
- **验证命令**：`mvn test -pl claude-j-infrastructure -Dtest=OrderRepositoryImplIT`
- **预期输出**：H2 集成测试通过，DO↔Domain 映射准确
- **commit**：`feat(infrastructure): OrderRepository 保存优惠券字段`

### 4.1 Start: DDL 更新
- **文件**：`claude-j-start/src/main/resources/db/schema.sql`
- **变更**：
  - t_order 表添加：coupon_id, discount_amount, final_amount 字段
- **验证命令**：`mvn test -pl claude-j-infrastructure -Dtest=OrderRepositoryImplIT`
- **预期输出**：H2 测试通过，表结构正确
- **commit**：`feat(start): 更新 t_order 表 DDL 支持优惠券字段`

### 5.1 ErrorCode 扩展
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/common/exception/ErrorCode.java`
- **变更**：
  - 新增：COUPON_NOT_BELONG_TO_USER, COUPON_NOT_AVAILABLE, COUPON_MIN_ORDER_AMOUNT_NOT_MET
- **验证命令**：`mvn compile -pl claude-j-domain`
- **预期输出**：编译通过
- **commit**：`feat(domain): 添加优惠券相关错误码`

### 6.1 全量验证
- **验证命令**：
  ```bash
  mvn clean test && \
  mvn checkstyle:check && \
  ./scripts/entropy-check.sh
  ```
- **预期输出**：
  - mvn test: 所有测试通过
  - checkstyle: Exit 0
  - entropy-check: 12/12 checks passed
- **commit**：`chore: 015-order-coupon-integration 全量验证通过`

## 开发完成记录

- 全量 `mvn clean test`：448/448 用例通过
- 架构合规检查：PASS（entropy-check.sh: 0 FAIL, 12 WARN）
- 通知 @qa 时间：2026-04-19

## QA 验收记录

- 全量测试（含集成测试）：448/448 用例通过
- 代码审查结果：✅ 通过，依赖方向正确，领域模型封装不变量
- 代码风格检查：✅ 通过，0 Checkstyle 违规
- 问题清单：详见 test-report.md（0 个阻塞性问题）
- **最终状态**：✅ 验收通过
