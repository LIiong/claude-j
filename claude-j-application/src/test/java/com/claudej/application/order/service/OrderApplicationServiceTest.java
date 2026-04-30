package com.claudej.application.order.service;

import com.claudej.application.common.assembler.PageAssembler;
import com.claudej.application.order.assembler.OrderAssembler;
import com.claudej.application.order.command.CreateOrderCommand;
import com.claudej.application.order.command.CreateOrderFromCartCommand;
import com.claudej.application.order.dto.OrderDTO;
import com.claudej.application.order.port.OrderMetricsPort;
import com.claudej.domain.cart.model.aggregate.Cart;
import com.claudej.domain.cart.model.valobj.Money;
import com.claudej.domain.cart.model.valobj.Quantity;
import com.claudej.domain.cart.repository.CartRepository;
import com.claudej.domain.common.event.DomainEventPublisher;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.coupon.model.aggregate.Coupon;
import com.claudej.domain.coupon.model.valobj.CouponId;
import com.claudej.domain.coupon.model.valobj.DiscountType;
import com.claudej.domain.coupon.repository.CouponRepository;
import com.claudej.domain.order.event.OrderCancelledEvent;
import com.claudej.domain.order.event.OrderCreatedEvent;
import com.claudej.domain.order.model.aggregate.Order;
import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.domain.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private OrderMetricsPort orderMetricsPort;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private OrderAssembler orderAssembler;

    @Mock
    private PageAssembler pageAssembler;

    @InjectMocks
    private OrderApplicationService orderApplicationService;

    private CreateOrderCommand createCommand;
    private CreateOrderFromCartCommand createFromCartCommand;
    private Order mockOrder;
    private OrderDTO mockOrderDTO;
    private Cart mockCart;
    private Coupon mockCoupon;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 4, 19, 12, 0, 0);
    private static final LocalDateTime VALID_FROM = LocalDateTime.of(2026, 4, 1, 0, 0, 0);
    private static final LocalDateTime VALID_UNTIL = LocalDateTime.of(2026, 5, 1, 23, 59, 59);

    @BeforeEach
    void setUp() {
        createCommand = new CreateOrderCommand();
        createCommand.setCustomerId("CUST001");

        CreateOrderCommand.OrderItemCommand item1 = new CreateOrderCommand.OrderItemCommand();
        item1.setProductId("PROD001");
        item1.setProductName("iPhone");
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("5999.00"));

        createCommand.setItems(Arrays.asList(item1));

        createFromCartCommand = new CreateOrderFromCartCommand();
        createFromCartCommand.setCustomerId("CUST001");

        mockOrder = Order.create(new CustomerId("CUST001"));
        mockOrder.addItem(com.claudej.domain.order.model.entity.OrderItem.create(
                "PROD001", "iPhone", 2,
                com.claudej.domain.order.model.valobj.Money.cny(5999)
        ));

        mockOrderDTO = new OrderDTO();
        mockOrderDTO.setOrderId("ORD123456");
        mockOrderDTO.setCustomerId("CUST001");
        mockOrderDTO.setStatus("CREATED");

        mockCart = Cart.create("CUST001");
        mockCart.addItem("PROD001", "iPhone", Money.cny(5999), new Quantity(2));

        mockCoupon = Coupon.create("满100减20", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), new BigDecimal("100"), "CUST001",
                VALID_FROM, VALID_UNTIL);
    }

    @Test
    void should_record_success_metric_when_create_order_succeeds() {
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        OrderDTO result = orderApplicationService.createOrder(createCommand);

        assertThat(result).isNotNull();
        verify(orderMetricsPort).recordCreateOrderSuccess("direct");
    }

    @Test
    void should_record_validation_failure_metric_when_create_order_command_invalid() {
        assertThatThrownBy(() -> orderApplicationService.createOrder(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("客户ID不能为空");

        verify(orderMetricsPort).recordCreateOrderFailure("direct", "validation");
        verify(orderMetricsPort).recordCreateOrderDuration(eq("direct"), eq("business_error"), any(Long.class));
    }

    @Test
    void should_record_failure_metric_when_create_order_from_cart_business_error() {
        when(cartRepository.findByUserId("CUST001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderApplicationService.createOrderFromCart(createFromCartCommand))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("购物车不存在");

        verify(orderMetricsPort).recordCreateOrderFailure("cart", "business");
        verify(orderMetricsPort).recordCreateOrderDuration(eq("cart"), eq("business_error"), any(Long.class));
    }

    @Test
    void should_record_system_failure_metric_when_create_order_from_cart_unexpected_error() {
        when(cartRepository.findByUserId("CUST001")).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> orderApplicationService.createOrderFromCart(createFromCartCommand))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boom");

        verify(orderMetricsPort).recordCreateOrderFailure("cart", "system");
        verify(orderMetricsPort).recordCreateOrderDuration(eq("cart"), eq("system_error"), any(Long.class));
    }

    @Test
    void should_createOrder_when_validCommandProvided() {
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        OrderDTO result = orderApplicationService.createOrder(createCommand);

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo("ORD123456");
        verify(orderRepository).save(any(Order.class));

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        OrderCreatedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getOrderId()).isEqualTo(mockOrder.getOrderIdValue());
        assertThat(publishedEvent.getCustomerId()).isEqualTo("CUST001");
        assertThat(publishedEvent.getItems()).hasSize(1);
    }

    @Test
    void should_throwException_when_createOrderWithEmptyItems() {
        createCommand.setItems(Collections.emptyList());

        assertThatThrownBy(() -> orderApplicationService.createOrder(createCommand))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单项不能为空");
    }

    @Test
    void should_returnOrder_when_getOrderByIdExists() {
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        OrderDTO result = orderApplicationService.getOrderById("ORD123456");

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo("ORD123456");
    }

    @Test
    void should_throwException_when_getOrderByIdNotExists() {
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderApplicationService.getOrderById("NONEXISTENT"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单不存在");
    }

    @Test
    void should_payOrder_when_orderExistsAndCreated() {
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        OrderDTO result = orderApplicationService.payOrder("ORD123456");

        assertThat(result).isNotNull();
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void should_cancelOrder_when_orderExistsAndCancellable() {
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        OrderDTO result = orderApplicationService.cancelOrder("ORD123456");

        assertThat(result).isNotNull();
        verify(orderRepository).save(any(Order.class));

        ArgumentCaptor<OrderCancelledEvent> eventCaptor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        OrderCancelledEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getOrderId()).isEqualTo(mockOrder.getOrderIdValue());
    }

    @Test
    void should_createOrderFromCart_when_cartExistsWithItems() {
        when(cartRepository.findByUserId("CUST001")).thenReturn(Optional.of(mockCart));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        OrderDTO result = orderApplicationService.createOrderFromCart(createFromCartCommand);

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo("ORD123456");
        verify(cartRepository).findByUserId("CUST001");
        verify(orderRepository).save(any(Order.class));
        verify(cartRepository).save(any(Cart.class));

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        OrderCreatedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getOrderId()).isEqualTo(mockOrder.getOrderIdValue());
        assertThat(publishedEvent.getItems()).hasSize(1);
    }

    @Test
    void should_throwBusinessException_when_cartNotFound() {
        when(cartRepository.findByUserId("CUST001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderApplicationService.createOrderFromCart(createFromCartCommand))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("购物车不存在");
    }

    @Test
    void should_throwBusinessException_when_cartIsEmpty() {
        Cart emptyCart = Cart.create("CUST001");
        when(cartRepository.findByUserId("CUST001")).thenReturn(Optional.of(emptyCart));

        assertThatThrownBy(() -> orderApplicationService.createOrderFromCart(createFromCartCommand))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("购物车为空");
    }

    @Test
    void should_throwBusinessException_when_customerIdIsBlank() {
        CreateOrderFromCartCommand command = new CreateOrderFromCartCommand();
        command.setCustomerId("");

        assertThatThrownBy(() -> orderApplicationService.createOrderFromCart(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("客户ID不能为空");
    }

    @Test
    void should_clearCartAfterOrderCreation() {
        when(cartRepository.findByUserId("CUST001")).thenReturn(Optional.of(mockCart));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        orderApplicationService.createOrderFromCart(createFromCartCommand);

        assertThat(mockCart.isEmpty()).isTrue();
        verify(cartRepository).save(mockCart);
    }

    @Test
    void should_applyCoupon_when_createOrderWithValidCoupon() {
        createCommand.setCouponId(mockCoupon.getCouponIdValue());

        when(couponRepository.findByCouponId(any(CouponId.class))).thenReturn(Optional.of(mockCoupon));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        OrderDTO result = orderApplicationService.createOrder(createCommand);

        assertThat(result).isNotNull();
        verify(couponRepository).findByCouponId(any(CouponId.class));
        verify(orderRepository).save(any(Order.class));
        verify(domainEventPublisher).publish(any(OrderCreatedEvent.class));
    }

    @Test
    void should_throwException_when_createOrderWithInvalidCoupon() {
        createCommand.setCouponId("INVALID_COUPON");

        when(couponRepository.findByCouponId(any(CouponId.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderApplicationService.createOrder(createCommand))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("优惠券不存在");
    }

    @Test
    void should_throwException_when_createOrderWithOtherUserCoupon() {
        createCommand.setCouponId(mockCoupon.getCouponIdValue());

        Coupon otherUserCoupon = Coupon.create("满100减20", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), new BigDecimal("100"), "OTHER_USER",
                VALID_FROM, VALID_UNTIL);

        when(couponRepository.findByCouponId(any(CouponId.class))).thenReturn(Optional.of(otherUserCoupon));

        assertThatThrownBy(() -> orderApplicationService.createOrder(createCommand))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("优惠券不属于该用户");
    }

    @Test
    void should_useCoupon_when_payOrderWithCoupon() {
        Order orderWithCoupon = Order.create(new CustomerId("CUST001"));
        orderWithCoupon.addItem(com.claudej.domain.order.model.entity.OrderItem.create(
                "PROD001", "iPhone", 2,
                com.claudej.domain.order.model.valobj.Money.cny(5999)
        ));
        orderWithCoupon.applyCoupon(new CouponId("COUPON001"), com.claudej.domain.order.model.valobj.Money.cny(20));

        Coupon availableCoupon = Coupon.create("满100减20", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), new BigDecimal("100"), "CUST001",
                VALID_FROM, VALID_UNTIL);

        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(orderWithCoupon));
        when(couponRepository.findByCouponId(any(CouponId.class))).thenReturn(Optional.of(availableCoupon));
        when(orderRepository.save(any(Order.class))).thenReturn(orderWithCoupon);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        OrderDTO result = orderApplicationService.payOrder("ORD123456");

        assertThat(result).isNotNull();
        verify(couponRepository).findByCouponId(any(CouponId.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void should_notQueryCoupon_when_payOrderWithoutCoupon() {
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        OrderDTO result = orderApplicationService.payOrder("ORD123456");

        assertThat(result).isNotNull();
        verify(couponRepository, never()).findByCouponId(any(CouponId.class));
    }

    @Test
    void should_applyCoupon_when_createOrderFromCartWithValidCoupon() {
        createFromCartCommand.setCouponId(mockCoupon.getCouponIdValue());

        when(cartRepository.findByUserId("CUST001")).thenReturn(Optional.of(mockCart));
        when(couponRepository.findByCouponId(any(CouponId.class))).thenReturn(Optional.of(mockCoupon));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        OrderDTO result = orderApplicationService.createOrderFromCart(createFromCartCommand);

        assertThat(result).isNotNull();
        verify(couponRepository).findByCouponId(any(CouponId.class));
        verify(orderRepository).save(any(Order.class));
        verify(domainEventPublisher).publish(any(OrderCreatedEvent.class));
    }

    @Test
    void should_shipOrder_when_orderPaid() {
        mockOrder.pay();
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        mockOrderDTO.setStatus("SHIPPED");
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        OrderDTO result = orderApplicationService.shipOrder("ORD123456");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("SHIPPED");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void should_throwException_when_shipOrderNotPaid() {
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));

        assertThatThrownBy(() -> orderApplicationService.shipOrder("ORD123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许发货");
    }

    @Test
    void should_deliverOrder_when_orderShipped() {
        mockOrder.pay();
        mockOrder.ship();
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        mockOrderDTO.setStatus("DELIVERED");
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        OrderDTO result = orderApplicationService.deliverOrder("ORD123456");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("DELIVERED");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void should_throwException_when_deliverOrderNotShipped() {
        mockOrder.pay();
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));

        assertThatThrownBy(() -> orderApplicationService.deliverOrder("ORD123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许确认送达");
    }

    @Test
    void should_refundOrder_when_orderPaidWithoutCoupon() {
        mockOrder.pay();
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        mockOrderDTO.setStatus("REFUNDED");
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        OrderDTO result = orderApplicationService.refundOrder("ORD123456");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("REFUNDED");
        verify(orderRepository).save(any(Order.class));
        verify(couponRepository, never()).findByCouponId(any(CouponId.class));
    }

    @Test
    void should_refundOrderAndUnuseCoupon_when_orderPaidWithCoupon() {
        Order orderWithCoupon = Order.create(new CustomerId("CUST001"));
        orderWithCoupon.addItem(com.claudej.domain.order.model.entity.OrderItem.create(
                "PROD001", "iPhone", 2,
                com.claudej.domain.order.model.valobj.Money.cny(5999)
        ));
        orderWithCoupon.applyCoupon(new CouponId("COUPON001"), com.claudej.domain.order.model.valobj.Money.cny(20));
        orderWithCoupon.pay();

        Coupon usedCoupon = Coupon.create("满100减20", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), new BigDecimal("100"), "CUST001",
                VALID_FROM, VALID_UNTIL);
        usedCoupon.use("ORD123456", NOW);

        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(orderWithCoupon));
        when(couponRepository.findByCouponId(any(CouponId.class))).thenReturn(Optional.of(usedCoupon));
        when(couponRepository.save(any(Coupon.class))).thenReturn(usedCoupon);
        when(orderRepository.save(any(Order.class))).thenReturn(orderWithCoupon);
        mockOrderDTO.setStatus("REFUNDED");
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        OrderDTO result = orderApplicationService.refundOrder("ORD123456");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("REFUNDED");
        verify(couponRepository).findByCouponId(any(CouponId.class));
        verify(couponRepository).save(any(Coupon.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void should_refundOrder_when_orderShippedWithoutCoupon() {
        mockOrder.pay();
        mockOrder.ship();
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        mockOrderDTO.setStatus("REFUNDED");
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        OrderDTO result = orderApplicationService.refundOrder("ORD123456");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("REFUNDED");
        verify(orderRepository).save(any(Order.class));
        verify(couponRepository, never()).findByCouponId(any(CouponId.class));
    }

    @Test
    void should_refundOrder_when_orderDeliveredWithoutCoupon() {
        mockOrder.pay();
        mockOrder.ship();
        mockOrder.deliver();
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        mockOrderDTO.setStatus("REFUNDED");
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        OrderDTO result = orderApplicationService.refundOrder("ORD123456");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("REFUNDED");
        verify(orderRepository).save(any(Order.class));
        verify(couponRepository, never()).findByCouponId(any(CouponId.class));
    }

    @Test
    void should_throwException_when_refundCreatedOrder() {
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));

        assertThatThrownBy(() -> orderApplicationService.refundOrder("ORD123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许退款");
    }

    @Test
    void should_throwException_when_refundCancelledOrder() {
        mockOrder.cancel();
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));

        assertThatThrownBy(() -> orderApplicationService.refundOrder("ORD123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许退款");
    }

    @Test
    void should_throwException_when_refundOrderAndCouponNotFound() {
        Order orderWithCoupon = Order.create(new CustomerId("CUST001"));
        orderWithCoupon.addItem(com.claudej.domain.order.model.entity.OrderItem.create(
                "PROD001", "iPhone", 2,
                com.claudej.domain.order.model.valobj.Money.cny(5999)
        ));
        orderWithCoupon.applyCoupon(new CouponId("COUPON001"), com.claudej.domain.order.model.valobj.Money.cny(20));
        orderWithCoupon.pay();

        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(orderWithCoupon));
        when(couponRepository.findByCouponId(any(CouponId.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderApplicationService.refundOrder("ORD123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("优惠券不存在");
    }
}
