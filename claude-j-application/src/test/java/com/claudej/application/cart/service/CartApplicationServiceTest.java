package com.claudej.application.cart.service;

import com.claudej.application.cart.assembler.CartAssembler;
import com.claudej.application.cart.command.AddCartItemCommand;
import com.claudej.application.cart.command.UpdateCartItemQuantityCommand;
import com.claudej.application.cart.dto.CartDTO;
import com.claudej.domain.cart.model.aggregate.Cart;
import com.claudej.domain.cart.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartApplicationServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartAssembler cartAssembler;

    @InjectMocks
    private CartApplicationService cartApplicationService;

    private AddCartItemCommand addCommand;
    private Cart cart;
    private CartDTO cartDTO;

    @BeforeEach
    void setUp() {
        addCommand = new AddCartItemCommand();
        addCommand.setUserId("u001");
        addCommand.setProductId("p001");
        addCommand.setProductName("商品A");
        addCommand.setUnitPrice(BigDecimal.valueOf(99.99));
        addCommand.setQuantity(2);

        cart = Cart.create("u001");
        cart.addItem("p001", "商品A", com.claudej.domain.cart.model.valobj.Money.cny(99.99), 
            new com.claudej.domain.cart.model.valobj.Quantity(2));

        cartDTO = new CartDTO();
        cartDTO.setUserId("u001");
        cartDTO.setTotalAmount(BigDecimal.valueOf(199.98));
        cartDTO.setItemCount(1);
    }

    @Test
    void should_createCartAndAddItem_when_cartNotExists() {
        // Arrange
        when(cartRepository.findByUserId("u001")).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartAssembler.toDTO(any(Cart.class))).thenReturn(cartDTO);

        // Act
        CartDTO result = cartApplicationService.addItem(addCommand);

        // Assert
        assertThat(result.getUserId()).isEqualTo("u001");
        assertThat(result.getItemCount()).isEqualTo(1);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void should_addItemToExistingCart_when_cartExists() {
        // Arrange
        Cart existingCart = Cart.create("u001");
        existingCart.addItem("p002", "商品B", com.claudej.domain.cart.model.valobj.Money.cny(50.00), 
            new com.claudej.domain.cart.model.valobj.Quantity(1));

        when(cartRepository.findByUserId("u001")).thenReturn(Optional.of(existingCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(existingCart);
        when(cartAssembler.toDTO(any(Cart.class))).thenReturn(cartDTO);

        // Act
        CartDTO result = cartApplicationService.addItem(addCommand);

        // Assert
        assertThat(result.getUserId()).isEqualTo("u001");
        verify(cartRepository).findByUserId("u001");
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void should_updateItemQuantity_when_itemExists() {
        // Arrange
        UpdateCartItemQuantityCommand updateCommand = new UpdateCartItemQuantityCommand();
        updateCommand.setUserId("u001");
        updateCommand.setProductId("p001");
        updateCommand.setQuantity(5);

        when(cartRepository.findByUserId("u001")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartAssembler.toDTO(any(Cart.class))).thenReturn(cartDTO);

        // Act
        CartDTO result = cartApplicationService.updateItemQuantity(updateCommand);

        // Assert
        assertThat(result.getUserId()).isEqualTo("u001");
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void should_removeItem_when_itemExists() {
        // Arrange
        when(cartRepository.findByUserId("u001")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartAssembler.toDTO(any(Cart.class))).thenReturn(cartDTO);

        // Act
        CartDTO result = cartApplicationService.removeItem("u001", "p001");

        // Assert
        assertThat(result.getUserId()).isEqualTo("u001");
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void should_clearCart_when_cartExists() {
        // Arrange
        when(cartRepository.findByUserId("u001")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        cartApplicationService.clearCart("u001");

        // Assert
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void should_returnCart_when_getCartCalled() {
        // Arrange
        when(cartRepository.findByUserId("u001")).thenReturn(Optional.of(cart));
        when(cartAssembler.toDTO(any(Cart.class))).thenReturn(cartDTO);

        // Act
        CartDTO result = cartApplicationService.getCart("u001");

        // Assert
        assertThat(result.getUserId()).isEqualTo("u001");
        verify(cartRepository).findByUserId("u001");
    }

    @Test
    void should_returnEmptyCart_when_cartNotExists() {
        // Arrange
        when(cartRepository.findByUserId("u001")).thenReturn(Optional.empty());
        when(cartAssembler.toDTO(any(Cart.class))).thenReturn(cartDTO);

        // Act
        CartDTO result = cartApplicationService.getCart("u001");

        // Assert
        assertThat(result.getUserId()).isEqualTo("u001");
    }
}
