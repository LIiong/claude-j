package com.claudej.application.order.service;

import com.claudej.application.order.assembler.OrderAssembler;
import com.claudej.application.order.command.CreateOrderCommand;
import com.claudej.application.order.command.CreateOrderFromCartCommand;
import com.claudej.application.order.dto.OrderDTO;
import com.claudej.domain.cart.model.aggregate.Cart;
import com.claudej.domain.cart.model.entity.CartItem;
import com.claudej.domain.cart.model.valobj.Money;
import com.claudej.domain.cart.model.valobj.Quantity;
import com.claudej.domain.cart.repository.CartRepository;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.coupon.model.aggregate.Coupon;
import com.claudej.domain.coupon.model.valobj.CouponId;
import com.claudej.domain.coupon.model.valobj.CouponStatus;
import com.claudej.domain.coupon.model.valobj.DiscountType;
import com.claudej.domain.coupon.repository.CouponRepository;
import com.claudej.domain.order.model.aggregate.Order;
import com.claudej.domain.order.model.valobj.CustomerId;
import com.claudej.domain.order.model.valobj.OrderId;
import com.claudej.domain.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private OrderAssembler orderAssembler;

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

        // 创建有商品的购物车
        mockCart = Cart.create("CUST001");
        mockCart.addItem("PROD001", "iPhone", Money.cny(5999), new Quantity(2));

        // 创建可用优惠券
        mockCoupon = Coupon.create("满100减20", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), new BigDecimal("100"), "CUST001",
                VALID_FROM, VALID_UNTIL);
    }

    @Test
    void should_createOrder_when_validCommandProvided() {
        // Given
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        // When
        OrderDTO result = orderApplicationService.createOrder(createCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo("ORD123456");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void should_throwException_when_createOrderWithNullCommand() {
        // When & Then
        assertThatThrownBy(() -> orderApplicationService.createOrder(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("客户ID不能为空");
    }

    @Test
    void should_throwException_when_createOrderWithEmptyItems() {
        // Given
        createCommand.setItems(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> orderApplicationService.createOrder(createCommand))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单项不能为空");
    }

    @Test
    void should_returnOrder_when_getOrderByIdExists() {
        // Given
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        // When
        OrderDTO result = orderApplicationService.getOrderById("ORD123456");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo("ORD123456");
    }

    @Test
    void should_throwException_when_getOrderByIdNotExists() {
        // Given
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderApplicationService.getOrderById("NONEXISTENT"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单不存在");
    }

    @Test
    void should_payOrder_when_orderExistsAndCreated() {
        // Given
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        // When
        OrderDTO result = orderApplicationService.payOrder("ORD123456");

        // Then
        assertThat(result).isNotNull();
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void should_cancelOrder_when_orderExistsAndCancellable() {
        // Given
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        // When
        OrderDTO result = orderApplicationService.cancelOrder("ORD123456");

        // Then
        assertThat(result).isNotNull();
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void should_createOrderFromCart_when_cartExistsWithItems() {
        // Given
        when(cartRepository.findByUserId("CUST001")).thenReturn(Optional.of(mockCart));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        // When
        OrderDTO result = orderApplicationService.createOrderFromCart(createFromCartCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo("ORD123456");
        verify(cartRepository).findByUserId("CUST001");
        verify(orderRepository).save(any(Order.class));
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void should_throwBusinessException_when_cartNotFound() {
        // Given
        when(cartRepository.findByUserId("CUST001")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderApplicationService.createOrderFromCart(createFromCartCommand))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("购物车不存在");
    }

    @Test
    void should_throwBusinessException_when_cartIsEmpty() {
        // Given
        Cart emptyCart = Cart.create("CUST001");
        when(cartRepository.findByUserId("CUST001")).thenReturn(Optional.of(emptyCart));

        // When & Then
        assertThatThrownBy(() -> orderApplicationService.createOrderFromCart(createFromCartCommand))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("购物车为空");
    }

    @Test
    void should_throwBusinessException_when_customerIdIsBlank() {
        // Given
        CreateOrderFromCartCommand command = new CreateOrderFromCartCommand();
        command.setCustomerId("");

        // When & Then
        assertThatThrownBy(() -> orderApplicationService.createOrderFromCart(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("客户ID不能为空");
    }

    @Test
    void should_clearCartAfterOrderCreation() {
        // Given
        when(cartRepository.findByUserId("CUST001")).thenReturn(Optional.of(mockCart));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        // When
        orderApplicationService.createOrderFromCart(createFromCartCommand);

        // Then
        assertThat(mockCart.isEmpty()).isTrue();
        verify(cartRepository).save(mockCart);
    }

    // --- Coupon integration tests ---

    @Test
    void should_applyCoupon_when_createOrderWithValidCoupon() {
        // Given
        createCommand.setCouponId(mockCoupon.getCouponIdValue());

        when(couponRepository.findByCouponId(any(CouponId.class))).thenReturn(Optional.of(mockCoupon));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        // When
        OrderDTO result = orderApplicationService.createOrder(createCommand);

        // Then
        assertThat(result).isNotNull();
        verify(couponRepository).findByCouponId(any(CouponId.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void should_throwException_when_createOrderWithInvalidCoupon() {
        // Given
        createCommand.setCouponId("INVALID_COUPON");

        when(couponRepository.findByCouponId(any(CouponId.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderApplicationService.createOrder(createCommand))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("优惠券不存在");
    }

    @Test
    void should_throwException_when_createOrderWithOtherUserCoupon() {
        // Given
        createCommand.setCouponId(mockCoupon.getCouponIdValue());

        // Create coupon for different user
        Coupon otherUserCoupon = Coupon.create("满100减20", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), new BigDecimal("100"), "OTHER_USER",
                VALID_FROM, VALID_UNTIL);

        when(couponRepository.findByCouponId(any(CouponId.class))).thenReturn(Optional.of(otherUserCoupon));

        // When & Then
        assertThatThrownBy(() -> orderApplicationService.createOrder(createCommand))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("优惠券不属于该用户");
    }

    @Test
    void should_useCoupon_when_payOrderWithCoupon() {
        // Given - Order with coupon
        Order orderWithCoupon = Order.create(new CustomerId("CUST001"));
        orderWithCoupon.addItem(com.claudej.domain.order.model.entity.OrderItem.create(
                "PROD001", "iPhone", 2,
                com.claudej.domain.order.model.valobj.Money.cny(5999)
        ));
        orderWithCoupon.applyCoupon(new CouponId("COUPON001"), com.claudej.domain.order.model.valobj.Money.cny(20));

        // Create a fresh available coupon for this test
        Coupon availableCoupon = Coupon.create("满100减20", DiscountType.FIXED_AMOUNT,
                new BigDecimal("20"), new BigDecimal("100"), "CUST001",
                VALID_FROM, VALID_UNTIL);

        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(orderWithCoupon));
        when(couponRepository.findByCouponId(any(CouponId.class))).thenReturn(Optional.of(availableCoupon));
        when(orderRepository.save(any(Order.class))).thenReturn(orderWithCoupon);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        // When
        OrderDTO result = orderApplicationService.payOrder("ORD123456");

        // Then
        assertThat(result).isNotNull();
        verify(couponRepository).findByCouponId(any(CouponId.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void should_notQueryCoupon_when_payOrderWithoutCoupon() {
        // Given
        when(orderRepository.findByOrderId(any(OrderId.class))).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        // When
        OrderDTO result = orderApplicationService.payOrder("ORD123456");

        // Then
        assertThat(result).isNotNull();
        verify(couponRepository, never()).findByCouponId(any(CouponId.class));
    }

    @Test
    void should_applyCoupon_when_createOrderFromCartWithValidCoupon() {
        // Given
        createFromCartCommand.setCouponId(mockCoupon.getCouponIdValue());

        when(cartRepository.findByUserId("CUST001")).thenReturn(Optional.of(mockCart));
        when(couponRepository.findByCouponId(any(CouponId.class))).thenReturn(Optional.of(mockCoupon));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(mockOrderDTO);

        // When
        OrderDTO result = orderApplicationService.createOrderFromCart(createFromCartCommand);

        // Then
        assertThat(result).isNotNull();
        verify(couponRepository).findByCouponId(any(CouponId.class));
        verify(orderRepository).save(any(Order.class));
    }
}
