# 测试用例设计 — 016-order-state-machine

## 测试范围
Order 状态机发货（ship）、送达（deliver）、退款（refund）完整链路验收，包括 Domain/Application/Adapter 三层新增代码和测试。

## 测试策略
按测试金字塔分层测试 + 代码审查 + 风格检查。

---

## 0. AC 自动化覆盖矩阵（强制，动笔前先填）

| # | 验收条件（AC） | 自动化层 | 对应测试方法 | 手动备注 |
|---|---|---|---|---|
| AC1 | PAID 状态可退款 | Domain | `OrderStatusTest.should_allowRefund_when_paidOrShippedOrDelivered` | - |
| AC2 | SHIPPED 状态可退款 | Domain | `OrderStatusTest.should_allowRefund_when_paidOrShippedOrDelivered` | - |
| AC3 | DELIVERED 状态可退款 | Domain | `OrderStatusTest.should_allowRefund_when_paidOrShippedOrDelivered` | - |
| AC4 | CREATED 状态不可退款 | Domain | `OrderStatusTest.should_notAllowRefund_when_createdOrCancelledOrRefunded` | - |
| AC5 | CANCELLED 状态不可退款 | Domain | `OrderStatusTest.should_notAllowRefund_when_createdOrCancelledOrRefunded` | - |
| AC6 | REFUNDED 状态不可退款 | Domain | `OrderStatusTest.should_notAllowRefund_when_createdOrCancelledOrRefunded` | - |
| AC7 | PAID → REFUNDED 转换成功 | Domain | `OrderStatusTest.should_transitionToRefunded_when_paid` | - |
| AC8 | SHIPPED → REFUNDED 转换成功 | Domain | `OrderStatusTest.should_transitionToRefunded_when_shipped` | - |
| AC9 | DELIVERED → REFUNDED 转换成功 | Domain | `OrderStatusTest.should_transitionToRefunded_when_delivered` | - |
| AC10 | CREATED → REFUNDED 转换失败 | Domain | `OrderStatusTest.should_throwException_when_refundFromCreated` | - |
| AC11 | CANCELLED → REFUNDED 转换失败 | Domain | `OrderStatusTest.should_throwException_when_refundFromCancelled` | - |
| AC12 | REFUNDED → REFUNDED 转换失败 | Domain | `OrderStatusTest.should_throwException_when_refundFromRefunded` | - |
| AC13 | Order.refund() 正确调用状态转换 | Domain | `OrderTest.should_refundOrder_when_paid` 等 | - |
| AC14 | Order.isRefunded() 正确判断 | Domain | `OrderTest.should_refundOrder_when_paid` 断言 isRefunded() | - |
| AC15 | shipOrder 用例编排正确 | Application | `OrderApplicationServiceTest.should_shipOrder_when_orderPaid` | - |
| AC16 | deliverOrder 用例编排正确 | Application | `OrderApplicationServiceTest.should_deliverOrder_when_orderShipped` | - |
| AC17 | refundOrder 无优惠券场景 | Application | `OrderApplicationServiceTest.should_refundOrder_when_orderPaidWithoutCoupon` | - |
| AC18 | refundOrder 有优惠券回滚场景 | Application | `OrderApplicationServiceTest.should_refundOrderAndUnuseCoupon_when_orderPaidWithCoupon` | - |
| AC19 | refundOrder 移除订单优惠券关联 | Application | `OrderApplicationServiceTest.should_refundOrderAndUnuseCoupon_when_orderPaidWithCoupon` 验证 order.removeCoupon | - |
| AC20 | POST /orders/{id}/ship 端点 | Adapter | `OrderControllerTest.should_return200_when_shipOrderSuccess` | - |
| AC21 | POST /orders/{id}/deliver 端点 | Adapter | `OrderControllerTest.should_return200_when_deliverOrderSuccess` | - |
| AC22 | POST /orders/{id}/refund 端点 | Adapter | `OrderControllerTest.should_return200_when_refundOrderSuccess` | - |
| AC23 | 状态转换异常返回 400 | Adapter | `OrderControllerTest.should_return400_when_shipInvalidStatusOrder` 等 | - |
| AC24 | 订单不存在返回 404 | Adapter | `OrderControllerTest.should_return404_when_shipNonExistentOrder` 等 | - |

---

## 一、Domain 层测试场景

### OrderStatus 值对象（退款相关）
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D1 | PAID 可退款 | PAID 状态 | canRefund() | 返回 true |
| D2 | SHIPPED 可退款 | SHIPPED 状态 | canRefund() | 返回 true |
| D3 | DELIVERED 可退款 | DELIVERED 状态 | canRefund() | 返回 true |
| D4 | CREATED 不可退款 | CREATED 状态 | canRefund() | 返回 false |
| D5 | CANCELLED 不可退款 | CANCELLED 状态 | canRefund() | 返回 false |
| D6 | REFUNDED 不可退款 | REFUNDED 状态 | canRefund() | 返回 false |
| D7 | PAID → REFUNDED | PAID 状态 | toRefunded() | 返回 REFUNDED |
| D8 | SHIPPED → REFUNDED | SHIPPED 状态 | toRefunded() | 返回 REFUNDED |
| D9 | DELIVERED → REFUNDED | DELIVERED 状态 | toRefunded() | 返回 REFUNDED |
| D10 | CREATED → REFUNDED 失败 | CREATED 状态 | toRefunded() | 抛 BusinessException |
| D11 | CANCELLED → REFUNDED 失败 | CANCELLED 状态 | toRefunded() | 抛 BusinessException |
| D12 | REFUNDED → REFUNDED 失败 | REFUNDED 状态 | toRefunded() | 抛 BusinessException |

### Order 聚合根（退款相关）
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D13 | PAID 订单退款 | 已支付订单 | refund() | status = REFUNDED, isRefunded() = true |
| D14 | SHIPPED 订单退款 | 已发货订单 | refund() | status = REFUNDED, isRefunded() = true |
| D15 | DELIVERED 订单退款 | 已送达订单 | refund() | status = REFUNDED, isRefunded() = true |
| D16 | CREATED 订单退款失败 | 已创建订单 | refund() | 抛 BusinessException |
| D17 | CANCELLED 订单退款失败 | 已取消订单 | refund() | 抛 BusinessException |

---

## 二、Application 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| A1 | 发货成功 | 已支付订单 | shipOrder(orderId) | 返回 SHIPPED 状态 DTO |
| A2 | 发货失败（状态不对） | 未支付订单 | shipOrder(orderId) | 抛 BusinessException |
| A3 | 确认送达成功 | 已发货订单 | deliverOrder(orderId) | 返回 DELIVERED 状态 DTO |
| A4 | 确认送达失败（状态不对） | 未发货订单 | deliverOrder(orderId) | 抛 BusinessException |
| A5 | 退款成功（无优惠券） | 已支付无优惠券订单 | refundOrder(orderId) | 返回 REFUNDED 状态 DTO |
| A6 | 退款成功（有优惠券） | 已支付有优惠券订单 | refundOrder(orderId) | coupon.unuse() + order.removeCoupon() + 返回 REFUNDED |
| A7 | 退款成功（SHIPPED 状态） | 已发货订单 | refundOrder(orderId) | 返回 REFUNDED 状态 DTO |
| A8 | 退款成功（DELIVERED 状态） | 已送达订单 | refundOrder(orderId) | 返回 REFUNDED 状态 DTO |
| A9 | 退款失败（CREATED 状态） | 已创建订单 | refundOrder(orderId) | 抛 BusinessException |
| A10 | 退款失败（CANCELLED 状态） | 已取消订单 | refundOrder(orderId) | 抛 BusinessException |
| A11 | 退款失败（优惠券不存在） | 有优惠券但优惠券丢失 | refundOrder(orderId) | 抛 COUPON_NOT_FOUND |
| A12 | 无优惠券时不查询优惠券 | 无优惠券订单 | refundOrder(orderId) | couponRepository.findByCouponId never called |

---

## 三、Infrastructure 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| I1 | REFUNDED 状态持久化 | 无变更 | — | OrderConverter 自动支持 REFUNDED |

> 本任务无 Infrastructure 层新增代码，依赖 OrderConverter.valueOf() 自动支持新枚举值。

---

## 四、Adapter 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| W1 | 发货成功 | Mock shipOrder 返回 SHIPPED | POST /orders/{id}/ship | 200 + status=SHIPPED |
| W2 | 发货失败（状态不对） | Mock 抛 INVALID_ORDER_STATUS_TRANSITION | POST /orders/{id}/ship | 400 + errorCode |
| W3 | 发货失败（订单不存在） | Mock 抛 ORDER_NOT_FOUND | POST /orders/{id}/ship | 404 + errorCode |
| W4 | 确认送达成功 | Mock deliverOrder 返回 DELIVERED | POST /orders/{id}/deliver | 200 + status=DELIVERED |
| W5 | 确认送达失败（状态不对） | Mock 抛 INVALID_ORDER_STATUS_TRANSITION | POST /orders/{id}/deliver | 400 + errorCode |
| W6 | 退款成功 | Mock refundOrder 返回 REFUNDED | POST /orders/{id}/refund | 200 + status=REFUNDED |
| W7 | 退款失败（状态不对） | Mock 抛 INVALID_ORDER_STATUS_TRANSITION | POST /orders/{id}/refund | 400 + errorCode |
| W8 | 退款失败（订单不存在） | Mock 抛 ORDER_NOT_FOUND | POST /orders/{id}/refund | 404 + errorCode |
| W9 | 退款失败（优惠券不存在） | Mock 抛 COUPON_NOT_FOUND | POST /orders/{id}/refund | 404 + errorCode |

---

## 五、集成测试场景（全链路）

| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E1 | 订单完整生命周期 | 创建→支付→发货→送达→退款 | 状态正确转换，金额正确 |
| E2 | 退款后优惠券状态回滚 | 创建带优惠券订单→支付→退款 | 优惠券状态恢复为 AVAILABLE |

> 本任务未新增集成测试（遵循"集成测试适量"规则，现有集成测试已覆盖订单创建/支付流程）。

---

## 六、代码审查检查项

- [x] 依赖方向正确（adapter → application → domain ← infrastructure）
- [x] domain 模块无 Spring/框架 import
- [x] 聚合根封装业务不变量（refund() 封装在 Order，状态转换规则在 OrderStatus）
- [x] 值对象不可变，equals/hashCode 正确（OrderStatus 枚举天然不可变）
- [x] Repository 接口在 domain，实现在 infrastructure（无新增）
- [x] 对象转换链正确：DO ↔ Domain ↔ DTO ↔ Response（REFUNDED 自动支持）
- [x] Controller 无业务逻辑（仅调用 service + 转换 DTO → Response）
- [x] 异常通过 GlobalExceptionHandler 统一处理

## 七、代码风格检查项

- [x] Java 8 兼容（无 var、records、text blocks、List.of）
- [x] 聚合根用 @Getter（Order 使用 @Getter）
- [x] 值对象 @Getter + @EqualsAndHashCode + @ToString（OrderStatus 枚举天然满足）
- [x] DO 用 @Data + @TableName，DTO 用 @Data（无新增）
- [x] 命名规范：XxxDO, XxxDTO, XxxMapper, XxxRepository, XxxRepositoryImpl（遵循既有规范）
- [x] 包结构符合 com.claudej.{layer}.{aggregate}.{sublayer}
- [x] 测试命名 should_xxx_when_xxx（所有新增测试符合）