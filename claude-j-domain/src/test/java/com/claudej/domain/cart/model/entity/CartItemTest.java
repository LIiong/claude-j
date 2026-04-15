package com.claudej.domain.cart.model.entity;

import com.claudej.domain.cart.model.valobj.Money;
import com.claudej.domain.cart.model.valobj.Quantity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CartItemTest {

    @Test
    void should_createCartItem_when_validParams() {
        // Arrange
        String productId = "p001";
        String productName = "商品A";
        Money unitPrice = Money.cny(99.99);
        Quantity quantity = new Quantity(2);

        // Act
        CartItem item = CartItem.create(productId, productName, unitPrice, quantity);

        // Assert
        assertThat(item.getProductId()).isEqualTo(productId);
        assertThat(item.getProductName()).isEqualTo(productName);
        assertThat(item.getUnitPrice()).isEqualTo(unitPrice);
        assertThat(item.getQuantity()).isEqualTo(quantity);
        assertThat(item.getSubtotal().getAmount().doubleValue()).isEqualTo(199.98);
    }

    @Test
    void should_updateQuantity_when_updateQuantityCalled() {
        // Arrange
        CartItem item = CartItem.create("p001", "商品A", Money.cny(50.00), new Quantity(2));

        // Act
        item.updateQuantity(new Quantity(5));

        // Assert
        assertThat(item.getQuantity().getValue()).isEqualTo(5);
        assertThat(item.getSubtotal().getAmount().doubleValue()).isEqualTo(250.00);
    }

    @Test
    void should_increaseQuantity_when_increaseQuantityCalled() {
        // Arrange
        CartItem item = CartItem.create("p001", "商品A", Money.cny(50.00), new Quantity(2));

        // Act
        item.increaseQuantity(new Quantity(3));

        // Assert
        assertThat(item.getQuantity().getValue()).isEqualTo(5);
        assertThat(item.getSubtotal().getAmount().doubleValue()).isEqualTo(250.00);
    }

    @Test
    void should_reconstructCartItem_when_reconstructCalled() {
        // Arrange
        String productId = "p001";
        String productName = "商品A";
        Money unitPrice = Money.cny(99.99);
        Quantity quantity = new Quantity(2);
        Money subtotal = Money.cny(199.98);

        // Act
        CartItem item = CartItem.reconstruct(productId, productName, unitPrice, quantity, subtotal);

        // Assert
        assertThat(item.getProductId()).isEqualTo(productId);
        assertThat(item.getSubtotal()).isEqualTo(subtotal);
    }
}
