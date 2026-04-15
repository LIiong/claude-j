package com.claudej.domain.cart.model.aggregate;

import com.claudej.domain.cart.model.entity.CartItem;
import com.claudej.domain.cart.model.valobj.Money;
import com.claudej.domain.cart.model.valobj.Quantity;
import com.claudej.domain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    @Test
    void should_createCart_when_validUserId() {
        // Arrange & Act
        Cart cart = Cart.create("u001");

        // Assert
        assertThat(cart.getUserId()).isEqualTo("u001");
        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getTotalAmount().isZero()).isTrue();
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void should_addItem_when_cartIsEmpty() {
        // Arrange
        Cart cart = Cart.create("u001");

        // Act
        cart.addItem("p001", "商品A", Money.cny(99.99), new Quantity(2));

        // Assert
        assertThat(cart.getItemCount()).isEqualTo(1);
        assertThat(cart.getTotalAmount().getAmount().doubleValue()).isEqualTo(199.98);
        assertThat(cart.isEmpty()).isFalse();
    }

    @Test
    void should_mergeQuantity_when_addingSameProduct() {
        // Arrange
        Cart cart = Cart.create("u001");
        cart.addItem("p001", "商品A", Money.cny(50.00), new Quantity(2));

        // Act
        cart.addItem("p001", "商品A", Money.cny(50.00), new Quantity(3));

        // Assert
        assertThat(cart.getItemCount()).isEqualTo(1);
        assertThat(cart.getItems().get(0).getQuantity().getValue()).isEqualTo(5);
        assertThat(cart.getTotalAmount().getAmount().doubleValue()).isEqualTo(250.00);
    }

    @Test
    void should_addMultipleItems_when_differentProducts() {
        // Arrange
        Cart cart = Cart.create("u001");

        // Act
        cart.addItem("p001", "商品A", Money.cny(50.00), new Quantity(2));
        cart.addItem("p002", "商品B", Money.cny(30.00), new Quantity(1));

        // Assert
        assertThat(cart.getItemCount()).isEqualTo(2);
        assertThat(cart.getTotalAmount().getAmount().doubleValue()).isEqualTo(130.00);
    }

    @Test
    void should_updateItemQuantity_when_itemExists() {
        // Arrange
        Cart cart = Cart.create("u001");
        cart.addItem("p001", "商品A", Money.cny(50.00), new Quantity(2));

        // Act
        cart.updateItemQuantity("p001", new Quantity(5));

        // Assert
        assertThat(cart.getItems().get(0).getQuantity().getValue()).isEqualTo(5);
        assertThat(cart.getTotalAmount().getAmount().doubleValue()).isEqualTo(250.00);
    }

    @Test
    void should_throwException_when_updatingNonExistentItem() {
        // Arrange
        Cart cart = Cart.create("u001");
        cart.addItem("p001", "商品A", Money.cny(50.00), new Quantity(2));

        // Act & Assert
        assertThatThrownBy(() -> cart.updateItemQuantity("p999", new Quantity(5)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("不存在");
    }

    @Test
    void should_removeItem_when_itemExists() {
        // Arrange
        Cart cart = Cart.create("u001");
        cart.addItem("p001", "商品A", Money.cny(50.00), new Quantity(2));
        cart.addItem("p002", "商品B", Money.cny(30.00), new Quantity(1));

        // Act
        cart.removeItem("p001");

        // Assert
        assertThat(cart.getItemCount()).isEqualTo(1);
        assertThat(cart.getTotalAmount().getAmount().doubleValue()).isEqualTo(30.00);
    }

    @Test
    void should_throwException_when_removingNonExistentItem() {
        // Arrange
        Cart cart = Cart.create("u001");
        cart.addItem("p001", "商品A", Money.cny(50.00), new Quantity(2));

        // Act & Assert
        assertThatThrownBy(() -> cart.removeItem("p999"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("不存在");
    }

    @Test
    void should_clearAllItems_when_clearCalled() {
        // Arrange
        Cart cart = Cart.create("u001");
        cart.addItem("p001", "商品A", Money.cny(50.00), new Quantity(2));
        cart.addItem("p002", "商品B", Money.cny(30.00), new Quantity(1));

        // Act
        cart.clear();

        // Assert
        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.getTotalAmount().isZero()).isTrue();
    }

    @Test
    void should_reconstructCart_when_reconstructCalled() {
        // Arrange
        CartItem item = CartItem.create("p001", "商品A", Money.cny(50.00), new Quantity(2));
        java.util.List<CartItem> items = java.util.Collections.singletonList(item);

        // Act
        Cart cart = Cart.reconstruct(1L, "u001", items, Money.cny(100.00), 
            java.time.LocalDateTime.now(), java.time.LocalDateTime.now());

        // Assert
        assertThat(cart.getId()).isEqualTo(1L);
        assertThat(cart.getUserId()).isEqualTo("u001");
        assertThat(cart.getItemCount()).isEqualTo(1);
    }
}
