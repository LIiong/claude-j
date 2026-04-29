package com.claudej.infrastructure.cart.persistence.repository;

import com.claudej.domain.cart.model.aggregate.Cart;
import com.claudej.domain.cart.model.valobj.Money;
import com.claudej.domain.cart.model.valobj.Quantity;
import com.claudej.domain.cart.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CartRepositoryImpl SpringBootTest
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
class CartRepositoryImplTest {

    @SpringBootApplication(scanBasePackages = {"com.claudej.infrastructure", "com.claudej.application"})
    @MapperScan("com.claudej.infrastructure.**.mapper")
    static class TestConfig {
    }

    @Autowired
    private CartRepository cartRepository;

    @Test
    void shouldSaveNewCart() {
        // Given
        String userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
        Cart cart = Cart.create(userId);
        cart.addItem("prod_001", "Test Product", Money.cny(new BigDecimal("99.99")), new Quantity(2));

        // When
        Cart savedCart = cartRepository.save(cart);

        // Then
        assertNotNull(savedCart);
        assertNotNull(savedCart.getId());
        assertEquals(userId, savedCart.getUserId());
        assertEquals(1, savedCart.getItemCount());
    }

    @Test
    void shouldFindCartByUserId() {
        // Given
        String userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
        Cart cart = Cart.create(userId);
        cart.addItem("prod_001", "Test Product", Money.cny(new BigDecimal("99.99")), new Quantity(2));
        cartRepository.save(cart);

        // When
        Optional<Cart> found = cartRepository.findByUserId(userId);

        // Then
        assertTrue(found.isPresent());
        assertEquals(userId, found.get().getUserId());
        assertEquals(1, found.get().getItemCount());
    }

    @Test
    void shouldReturnEmptyWhenCartNotFound() {
        // When
        Optional<Cart> found = cartRepository.findByUserId("nonexistent_user");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void shouldUpdateExistingCart() {
        // Given
        String userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
        Cart cart = Cart.create(userId);
        cart.addItem("prod_001", "Test Product 1", Money.cny(new BigDecimal("99.99")), new Quantity(1));
        cartRepository.save(cart);

        // When - add another item
        Cart savedCart = cartRepository.findByUserId(userId).orElse(null);
        savedCart.addItem("prod_002", "Test Product 2", Money.cny(new BigDecimal("49.99")), new Quantity(2));
        Cart updatedCart = cartRepository.save(savedCart);

        // Then
        assertEquals(2, updatedCart.getItemCount());

        // Verify by finding again
        Optional<Cart> found = cartRepository.findByUserId(userId);
        assertTrue(found.isPresent());
        assertEquals(2, found.get().getItemCount());
    }

    @Test
    void shouldDeleteCartByUserId() {
        // Given
        String userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
        Cart cart = Cart.create(userId);
        cart.addItem("prod_001", "Test Product", Money.cny(new BigDecimal("99.99")), new Quantity(1));
        cartRepository.save(cart);

        // Verify cart exists
        assertTrue(cartRepository.findByUserId(userId).isPresent());

        // When
        cartRepository.deleteByUserId(userId);

        // Then
        assertFalse(cartRepository.findByUserId(userId).isPresent());
    }

    @Test
    void shouldCalculateTotalAmountCorrectly() {
        // Given
        String userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
        Cart cart = Cart.create(userId);
        cart.addItem("prod_001", "Product 1", Money.cny(new BigDecimal("100.00")), new Quantity(2));
        cart.addItem("prod_002", "Product 2", Money.cny(new BigDecimal("50.00")), new Quantity(1));

        // When
        Cart savedCart = cartRepository.save(cart);

        // Then
        assertEquals(new BigDecimal("250.00"), savedCart.getTotalAmount().getAmount());

        // Verify by finding again
        Optional<Cart> found = cartRepository.findByUserId(userId);
        assertTrue(found.isPresent());
        assertEquals(new BigDecimal("250.00"), found.get().getTotalAmount().getAmount());
    }

    @Test
    void shouldUpdateItemQuantity() {
        // Given
        String userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
        Cart cart = Cart.create(userId);
        cart.addItem("prod_001", "Test Product", Money.cny(new BigDecimal("100.00")), new Quantity(1));
        cartRepository.save(cart);

        // When
        Cart savedCart = cartRepository.findByUserId(userId).orElse(null);
        savedCart.updateItemQuantity("prod_001", new Quantity(5));
        Cart updatedCart = cartRepository.save(savedCart);

        // Then
        assertEquals(new BigDecimal("500.00"), updatedCart.getTotalAmount().getAmount());
    }

    @Test
    void shouldRemoveItem() {
        // Given
        String userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
        Cart cart = Cart.create(userId);
        cart.addItem("prod_001", "Product 1", Money.cny(new BigDecimal("100.00")), new Quantity(1));
        cart.addItem("prod_002", "Product 2", Money.cny(new BigDecimal("50.00")), new Quantity(1));
        cartRepository.save(cart);

        // When
        Cart savedCart = cartRepository.findByUserId(userId).orElse(null);
        savedCart.removeItem("prod_001");
        Cart updatedCart = cartRepository.save(savedCart);

        // Then
        assertEquals(1, updatedCart.getItemCount());
        assertEquals(new BigDecimal("50.00"), updatedCart.getTotalAmount().getAmount());
    }

    @Test
    void shouldClearCart() {
        // Given
        String userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
        Cart cart = Cart.create(userId);
        cart.addItem("prod_001", "Product 1", Money.cny(new BigDecimal("100.00")), new Quantity(1));
        cart.addItem("prod_002", "Product 2", Money.cny(new BigDecimal("50.00")), new Quantity(1));
        cartRepository.save(cart);

        // When
        Cart savedCart = cartRepository.findByUserId(userId).orElse(null);
        savedCart.clear();
        Cart clearedCart = cartRepository.save(savedCart);

        // Then
        assertTrue(clearedCart.isEmpty());
        assertEquals(0, clearedCart.getItemCount());
        assertEquals(0, clearedCart.getTotalAmount().getAmount().compareTo(BigDecimal.ZERO));
    }
}
