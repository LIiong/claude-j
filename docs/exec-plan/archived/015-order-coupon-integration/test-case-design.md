# 测试用例设计 — 015-order-coupon-integration

## 测试范围

验证 Order 与 Coupon 聚合的集成能力，包括订单生命周期中的优惠券使用、核销与回滚机制。核心功能：下单用券验证、支付时核销、取消时回滚。

## 测试策略

按测试金字塔分层测试 + 代码审查 + 风格检查。

---

## 0. AC 自动化覆盖矩阵（强制，动笔前先填）

| # | 验收条件（AC） | 自动化层 | 对应测试方法 | 手动备注（若有） |
|---|---|---|---|---|
| AC1 | Order 聚合包含 couponId、discountAmount、finalAmount 字段 | Domain | `OrderTest.should_reconstructOrder_withCouponFields` | - |
| AC2 | Coupon.unuse() 方法正确将状态从 USED 回滚到 AVAILABLE | Domain | `CouponTest.should_unuseCoupon_when_used` | - |
| AC3 | OrderApplicationService 验证优惠券归属（COUPON_NOT_BELONG_TO_USER） | Application | `OrderApplicationServiceTest.should_throwException_when_createOrderWithOtherUserCoupon` | - |
| AC4 | OrderApplicationService 验证优惠券有效期（COUPON_NOT_AVAILABLE） | Domain | `CouponTest.should_throwException_when_useExpiredCoupon` | - |
| AC5 | OrderApplicationService 验证最低消费金额（COUPON_MIN_ORDER_AMOUNT_NOT_MET） | Application | `OrderApplicationServiceTest.should_applyCoupon_when_createOrderWithValidCoupon`（隐含验证） | - |
| AC6 | 折扣计算正确（FIXED_AMOUNT 和 PERCENTAGE 类型） | Domain | `CouponTest.should_returnFixedAmount_when_orderAmountIsLarger`、`CouponTest.should_calculatePercentageDiscount_when_percentage` | - |
| AC7 | payOrder() 调用 coupon.use() 并更新 Order 状态 | Application | `OrderApplicationServiceTest.should_useCoupon_when_payOrderWithCoupon` | - |
| AC8 | cancelOrder() 调用 coupon.unuse() 如果优惠券已应用 | Application | `OrderApplicationServiceTest.should_cancelOrder_when_orderExistsAndCancellable`（含优惠券回滚逻辑验证） | - |

---

## 一、Domain 层测试场景

<!-- 纯单元测试，JUnit 5 + AssertJ，禁止 Spring 上下文 -->

### Order 聚合根 - 优惠券相关
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D1 | 应用优惠券成功 | 订单已创建，有订单项 | applyCoupon(CouponId, Money) | couponId、discountAmount、finalAmount 正确设置 |
| D2 | 应用优惠券 - couponId 为空 | 订单已创建 | applyCoupon(null, Money) | 抛 BusinessException(COUPON_ID_EMPTY) |
| D3 | 应用优惠券 - discountAmount 为空 | 订单已创建 | applyCoupon(CouponId, null) | 抛 BusinessException(COUPON_DISCOUNT_VALUE_INVALID) |
| D4 | 应用优惠券 - discountAmount 为零 | 订单已创建 | applyCoupon(CouponId, Money.cny(0)) | 抛 BusinessException(COUPON_DISCOUNT_VALUE_INVALID) |
| D5 | 应用优惠券 - 折扣金额大于订单金额 | 订单金额50元 | applyCoupon(CouponId, Money.cny(60)) | 抛 BusinessException(COUPON_DISCOUNT_VALUE_INVALID) |
| D6 | 移除优惠券成功 | 已应用优惠券 | removeCoupon() | couponId=null, discountAmount=0, finalAmount=totalAmount |
| D7 | 移除优惠券 - 无优惠券 | 未应用优惠券 | removeCoupon() | 无变化，不抛异常 |
| D8 | 重建订单 - 带优惠券字段 | 完整参数 | reconstruct(... couponId, discountAmount, finalAmount ...) | 所有字段正确还原 |
| D9 | 计算最终金额 - 有优惠券 | 订单金额100元，折扣20元 | getFinalAmount() | 返回80元 |
| D10 | 计算最终金额 - 无优惠券 | 订单金额100元，无优惠券 | getFinalAmount() | 返回100元 |
| D11 | hasCoupon() - 有优惠券 | 已应用优惠券 | hasCoupon() | 返回 true |
| D12 | hasCoupon() - 无优惠券 | 未应用优惠券 | hasCoupon() | 返回 false |

### Coupon 聚合根 - unuse() 方法
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D13 | 回滚优惠券成功 | 优惠券状态为 USED | unuse() | 状态变为 AVAILABLE, usedTime=null, usedOrderId=null |
| D14 | 回滚优惠券 - 状态为 AVAILABLE | 优惠券状态为 AVAILABLE | unuse() | 抛 BusinessException(INVALID_COUPON_STATUS_TRANSITION) |
| D15 | 回滚优惠券 - 状态为 EXPIRED | 优惠券状态为 EXPIRED | unuse() | 抛 BusinessException(INVALID_COUPON_STATUS_TRANSITION) |

### CouponStatus 枚举 - 状态转换
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D16 | 转换到 USED 成功 | 状态为 AVAILABLE | toUsed() | 返回 USED |
| D17 | 转换到 USED 失败 | 状态为 USED | toUsed() | 抛 BusinessException |
| D18 | 转换到 AVAILABLE 成功 | 状态为 USED | toAvailable() | 返回 AVAILABLE |
| D19 | 转换到 AVAILABLE 失败 | 状态为 AVAILABLE | toAvailable() | 抛 BusinessException |
| D20 | canUnuse() - USED 状态 | 状态为 USED | canUnuse() | 返回 true |
| D21 | canUnuse() - 非 USED 状态 | 状态为 AVAILABLE | canUnuse() | 返回 false |

### Money 值对象 - subtract() 方法
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D22 | 金额相减成功 | 同币种 | subtract(Money) | 返回正确差值 |
| D23 | 金额相减 - 币种不一致 | 不同币种 | subtract(Money) | 抛 BusinessException(CART_CURRENCY_MISMATCH) |

---

## 二、Application 层测试场景

<!-- Mock 单元测试，JUnit 5 + Mockito，禁止 Spring 上下文 -->

### OrderApplicationService - 优惠券集成
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| A1 | 创建订单 - 应用有效优惠券 | 有效优惠券归属当前用户 | createOrder(command with couponId) | 订单创建成功，优惠券验证通过 |
| A2 | 创建订单 - 优惠券不存在 | couponId 无效 | createOrder(command with invalid couponId) | 抛 BusinessException(COUPON_NOT_FOUND) |
| A3 | 创建订单 - 优惠券不属于当前用户 | 优惠券归属其他用户 | createOrder(command with other user's coupon) | 抛 BusinessException(COUPON_NOT_BELONG_TO_USER) |
| A4 | 创建订单 - 优惠券已过期 | 优惠券有效期已过 | createOrder(command with expired coupon) | 抛 BusinessException(COUPON_NOT_AVAILABLE) |
| A5 | 创建订单 - 订单金额不足最低消费 | 订单金额 < minOrderAmount | createOrder(command) | 抛 BusinessException(COUPON_MIN_ORDER_AMOUNT_NOT_MET) |
| A6 | 支付订单 - 核销优惠券 | 订单已应用优惠券 | payOrder(orderId) | coupon.use() 被调用，状态变为 USED |
| A7 | 支付订单 - 无优惠券 | 订单未应用优惠券 | payOrder(orderId) | 正常支付，不查询优惠券 |
| A8 | 取消订单 - 回滚优惠券 | 订单已支付且有优惠券 | cancelOrder(orderId) | coupon.unuse() 被调用，状态回滚 |
| A9 | 取消订单 - 未支付但有优惠券 | 订单未支付但有优惠券 | cancelOrder(orderId) | removeCoupon() 被调用，优惠券未核销 |
| A10 | 从购物车创建订单 - 应用优惠券 | 有效优惠券 | createOrderFromCart(command with couponId) | 订单创建成功，优惠券验证通过 |

---

## 三、Infrastructure 层测试场景

<!-- 集成测试，@SpringBootTest + H2 -->

### OrderConverter - DO 与 Domain 转换
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| I1 | toDomain - 还原带优惠券字段的订单 | DO 包含 couponId, discountAmount, finalAmount | toDomain(OrderDO, items) | Domain 对象字段正确还原 |
| I2 | toDO - 保存带优惠券字段的订单 | Domain 包含优惠券信息 | toDO(Order) | DO 字段正确设置 |
| I3 | toDomain - 无优惠券的订单 | DO 中 couponId 为 null | toDomain(OrderDO, items) | Domain couponId 为 null |

### OrderRepositoryImpl - 持久化往返
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| I4 | 保存并查询 - 带优惠券的订单 | 订单已应用优惠券 | save() → findByOrderId() | 优惠券字段完整还原 |
| I5 | 更新订单 - 添加优惠券后保存 | 订单已存在，后续应用优惠券 | save() → findByOrderId() | 更新后的优惠券字段正确 |

---

## 四、Adapter 层测试场景

<!-- API 测试，@WebMvcTest + MockMvc -->

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| W1 | 创建订单 - 带优惠券参数 | Mock 返回 OrderDTO | POST /api/v1/orders (含 couponId) | 200 + success=true |
| W2 | 创建订单 - 无效优惠券 | Mock 抛 BusinessException | POST /api/v1/orders | 400 + 对应错误码 |
| W3 | 支付订单 - 触发优惠券核销 | Mock 返回 OrderDTO | POST /api/v1/orders/{id}/pay | 200 + success=true |
| W4 | 取消订单 - 触发优惠券回滚 | Mock 返回 OrderDTO | POST /api/v1/orders/{id}/cancel | 200 + success=true |

---

## 五、集成测试场景（全链路）

<!-- @SpringBootTest + AutoConfigureMockMvc + H2，在 start 模块 -->

| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E1 | 完整流程 - 下单用券→支付核销→取消回滚 | 1. 创建优惠券<br>2. 创建订单（用券）<br>3. 支付订单<br>4. 查询优惠券状态<br>5. 取消订单<br>6. 查询优惠券状态 | 优惠券状态：AVAILABLE → USED → AVAILABLE |
| E2 | 订单金额计算一致性验证 | 创建订单（总价200元，折扣20元） | finalAmount = 180元，数据库记录一致 |
| E3 | 跨聚合事务一致性 | 支付订单（同时更新 Order + Coupon） | 两个聚合状态一致，无部分更新 |

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
- [x] 命名规范：XxxDO, XxxDTO, XxxMapper, XxxRepository, XxxRepositoryImpl
- [x] 包结构符合 com.claudej.{layer}.{aggregate}.{sublayer}
- [x] 测试命名 should_xxx_when_xxx
