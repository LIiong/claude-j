# 任务执行计划 — 016-order-state-machine

## 任务状态跟踪

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Domain: OrderStatus 新增 REFUNDED + canRefund/toRefunded + 测试 | dev | 验收通过 | commit: 520b73f |
| 2 | Domain: Order 新增 refund()/isRefunded() + 测试 | dev | 验收通过 | commit: 520b73f |
| 3 | Application: OrderApplicationService 新增 shipOrder/deliverOrder/refundOrder + 测试 | dev | 验收通过 | TDD Red-Green 完成 |
| 4 | Adapter: OrderController 新增 ship/deliver/refund 端点 + 测试 | dev | 验收通过 | TDD Red-Green 完成 |
| 5 | 全量 mvn test + checkstyle + entropy-check | dev | 验收通过 | 549 tests, 0 checkstyle, entropy PASS |
| 6 | QA: 测试用例设计 | qa | 完成 | test-case-design.md 已编写 |
| 7 | QA: 验收测试 + 代码审查 | qa | 完成 | test-report.md 已编写，验收通过 |

## 执行顺序
domain -> application -> adapter -> 全量测试 -> QA 验收

## 原子任务分解

### 1.1 Domain OrderStatus 新增 REFUNDED + canRefund() + toRefunded()
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/order/model/valobj/OrderStatus.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/order/model/valobj/OrderStatusTest.java`
- **骨架**（Red 阶段先写测试）：
  ```java
  // OrderStatusTest.java
  @Test
  void should_allowRefund_when_paidOrShippedOrDelivered() {
      assertThat(OrderStatus.PAID.canRefund()).isTrue();
      assertThat(OrderStatus.SHIPPED.canRefund()).isTrue();
      assertThat(OrderStatus.DELIVERED.canRefund()).isTrue();
  }

  @Test
  void should_notAllowRefund_when_createdOrCancelledOrRefunded() {
      assertThat(OrderStatus.CREATED.canRefund()).isFalse();
      assertThat(OrderStatus.CANCELLED.canRefund()).isFalse();
      assertThat(OrderStatus.REFUNDED.canRefund()).isFalse();
  }

  @Test
  void should_transitionToRefunded_when_paid() {
      assertThat(OrderStatus.PAID.toRefunded()).isEqualTo(OrderStatus.REFUNDED);
  }

  @Test
  void should_transitionToRefunded_when_shipped() {
      assertThat(OrderStatus.SHIPPED.toRefunded()).isEqualTo(OrderStatus.REFUNDED);
  }

  @Test
  void should_transitionToRefunded_when_delivered() {
      assertThat(OrderStatus.DELIVERED.toRefunded()).isEqualTo(OrderStatus.REFUNDED);
  }

  @Test
  void should_throwException_when_refundFromCreated() {
      assertThatThrownBy(() -> OrderStatus.CREATED.toRefunded())
              .isInstanceOf(BusinessException.class)
              .hasMessageContaining("不允许退款");
  }

  @Test
  void should_throwException_when_refundFromCancelled() {
      assertThatThrownBy(() -> OrderStatus.CANCELLED.toRefunded())
              .isInstanceOf(BusinessException.class)
              .hasMessageContaining("不允许退款");
  }

  @Test
  void should_throwException_when_refundFromRefunded() {
      assertThatThrownBy(() -> OrderStatus.REFUNDED.toRefunded())
              .isInstanceOf(BusinessException.class)
              .hasMessageContaining("不允许退款");
  }
  ```
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=OrderStatusTest`
- **预期输出**：`Tests run: ~16, Failures: 0, Errors: 0`
- **commit**：`feat(domain): order OrderStatus 新增 REFUNDED + canRefund/toRefunded`

### 1.2 Domain Order 新增 refund() + isRefunded()
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/order/model/aggregate/Order.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/order/model/aggregate/OrderTest.java`
- **骨架**（Red 阶段先写测试）：
  ```java
  // OrderTest.java
  @Test
  void should_refundOrder_when_paid() {
      Order order = Order.create(new CustomerId("CUST001"));
      order.addItem(OrderItem.create("PROD001", "iPhone", 1, Money.cny(5999)));
      order.pay();
      order.refund();
      assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
      assertThat(order.isRefunded()).isTrue();
  }

  @Test
  void should_refundOrder_when_shipped() {
      // ship -> refund
      assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
  }

  @Test
  void should_refundOrder_when_delivered() {
      // pay -> ship -> deliver -> refund
      assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
  }

  @Test
  void should_throwException_when_refundCreatedOrder() {
      // CREATED -> refund should fail
      assertThatThrownBy(() -> order.refund()).hasMessageContaining("不允许退款");
  }

  @Test
  void should_throwException_when_refundCancelledOrder() {
      // cancel -> refund should fail
  }

  @Test
  void should_throwException_when_refundRefundedOrder() {
      // refund -> refund should fail
  }
  ```
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=OrderTest`
- **预期输出**：`Tests run: ~25, Failures: 0, Errors: 0`
- **commit**：`feat(domain): order Order 新增 refund/isRefunded 方法`

### 3.1 Application OrderApplicationService 新增 shipOrder/deliverOrder/refundOrder
- **文件**：`claude-j-application/src/main/java/com/claudej/application/order/service/OrderApplicationService.java`
- **测试**：`claude-j-application/src/test/java/com/claudej/application/order/service/OrderApplicationServiceTest.java`
- **骨架**：
  ```java
  // OrderApplicationServiceTest.java
  @Test
  void should_shipOrder_when_orderPaid() {
      // Given: paid order
      mockOrder.pay();
      when(orderRepository.findByOrderId(any())).thenReturn(Optional.of(mockOrder));
      when(orderRepository.save(any())).thenReturn(mockOrder);
      when(orderAssembler.toDTO(any())).thenReturn(mockOrderDTO);

      OrderDTO result = orderApplicationService.shipOrder("ORD123456");

      assertThat(result).isNotNull();
      verify(orderRepository).save(any(Order.class));
  }

  @Test
  void should_deliverOrder_when_orderShipped() { ... }

  @Test
  void should_refundOrder_when_orderPaidWithNoCoupon() { ... }

  @Test
  void should_refundOrderAndUnuseCoupon_when_orderPaidWithCoupon() {
      // coupon.unuse() called
  }

  @Test
  void should_refundOrderAndUnuseCoupon_when_orderShippedWithCoupon() { ... }

  @Test
  void should_refundOrderAndUnuseCoupon_when_orderDeliveredWithCoupon() { ... }

  @Test
  void should_removeCouponOnOrder_when_refundWithCoupon() { ... }

  @Test
  void should_notTouchCoupon_when_refundWithoutCoupon() {
      verify(couponRepository, never()).findByCouponId(any());
  }
  ```
- **验证命令**：`mvn test -pl claude-j-application -Dtest=OrderApplicationServiceTest`
- **预期输出**：`Tests run: ~20, Failures: 0, Errors: 0`
- **commit**：`feat(application): order 新增 shipOrder/deliverOrder/refundOrder 用例`

### 4.1 Adapter OrderController 新增 ship/deliver/refund 端点
- **文件**：`claude-j-adapter/src/main/java/com/claudej/adapter/order/web/OrderController.java`
- **测试**：`claude-j-adapter/src/test/java/com/claudej/adapter/order/web/OrderControllerTest.java`
- **骨架**：
  ```java
  // OrderControllerTest.java
  @Test
  void should_return200_when_shipOrderSuccess() throws Exception {
      mockOrderDTO.setStatus("SHIPPED");
      when(orderApplicationService.shipOrder("ORD123456")).thenReturn(mockOrderDTO);
      mockMvc.perform(post("/api/v1/orders/ORD123456/ship"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.data.status", is("SHIPPED")));
  }

  @Test
  void should_return400_when_shipInvalidStatusOrder() throws Exception { ... }

  @Test
  void should_return200_when_deliverOrderSuccess() throws Exception { ... }

  @Test
  void should_return400_when_deliverInvalidStatusOrder() throws Exception { ... }

  @Test
  void should_return200_when_refundOrderSuccess() throws Exception { ... }

  @Test
  void should_return400_when_refundInvalidStatusOrder() throws Exception { ... }

  @Test
  void should_return404_when_refundNonExistentOrder() throws Exception { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-adapter -Dtest=OrderControllerTest`
- **预期输出**：`Tests run: ~18, Failures: 0, Errors: 0`
- **commit**：`feat(adapter): order 新增 ship/deliver/refund REST 端点`

## 开发完成记录
- **日期**: 2026-04-22
- **开发者**: @dev
- **测试数量**: 549 tests run, 0 failures, 0 errors
- **新增测试**:
  - OrderStatusTest: 新增 8 个退款相关测试（canRefund/toRefunded 正向 + 逆向）
  - OrderTest: 新增 5 个退款相关测试（refund 正向 + 逆向）
  - OrderApplicationServiceTest: 新增 12 个 ship/deliver/refund 测试
  - OrderControllerTest: 新增 9 个 ship/deliver/refund 端点测试
- **三项验证**:
  - mvn test: BUILD SUCCESS (549 tests, 0 failures)
  - mvn checkstyle:check: 0 Checkstyle violations
  - ./scripts/entropy-check.sh: 0 FAIL, 12 WARN (PASS)

## QA 验收记录
<!-- qa 验收后填写 -->
