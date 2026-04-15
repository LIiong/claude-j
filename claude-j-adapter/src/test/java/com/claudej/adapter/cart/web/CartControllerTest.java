package com.claudej.adapter.cart.web;

import com.claudej.adapter.cart.web.request.AddCartItemRequest;
import com.claudej.adapter.cart.web.request.UpdateCartItemQuantityRequest;
import com.claudej.adapter.common.ApiResult;
import com.claudej.adapter.common.GlobalExceptionHandler;
import com.claudej.application.cart.dto.CartDTO;
import com.claudej.application.cart.dto.CartItemDTO;
import com.claudej.application.cart.service.CartApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CartController WebMvcTest
 */
@WebMvcTest(CartController.class)
@ContextConfiguration(classes = {CartController.class, GlobalExceptionHandler.class})
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartApplicationService cartApplicationService;

    @Test
    public void shouldAddItemToCart() throws Exception {
        // Given
        AddCartItemRequest request = new AddCartItemRequest();
        request.setUserId("user_001");
        request.setProductId("prod_001");
        request.setProductName("Test Product");
        request.setUnitPrice(new BigDecimal("99.99"));
        request.setQuantity(2);

        CartDTO cartDTO = createCartDTO("user_001", 1, new BigDecimal("199.98"));

        when(cartApplicationService.addItem(any())).thenReturn(cartDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/carts/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("user_001"))
                .andExpect(jsonPath("$.data.totalAmount").value(199.98))
                .andExpect(jsonPath("$.data.itemCount").value(1));
    }

    @Test
    public void shouldReturnBadRequestWhenAddItemWithInvalidData() throws Exception {
        // Given - invalid request (missing required fields)
        AddCartItemRequest request = new AddCartItemRequest();
        request.setUserId("");  // empty userId
        request.setProductId("prod_001");
        request.setProductName("Test Product");
        request.setUnitPrice(new BigDecimal("99.99"));
        request.setQuantity(2);

        // When & Then
        mockMvc.perform(post("/api/v1/carts/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldGetCart() throws Exception {
        // Given
        String userId = "user_001";
        CartDTO cartDTO = createCartDTO(userId, 2, new BigDecimal("299.97"));

        when(cartApplicationService.getCart(userId)).thenReturn(cartDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/carts")
                .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.itemCount").value(2))
                .andExpect(jsonPath("$.data.totalAmount").value(299.97));
    }

    @Test
    public void shouldUpdateItemQuantity() throws Exception {
        // Given
        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest();
        request.setUserId("user_001");
        request.setProductId("prod_001");
        request.setQuantity(5);

        CartDTO cartDTO = createCartDTO("user_001", 1, new BigDecimal("499.95"));

        when(cartApplicationService.updateItemQuantity(any())).thenReturn(cartDTO);

        // When & Then
        mockMvc.perform(put("/api/v1/carts/items/quantity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("user_001"));
    }

    @Test
    public void shouldRemoveItem() throws Exception {
        // Given
        String userId = "user_001";
        String productId = "prod_001";
        CartDTO cartDTO = createCartDTO(userId, 0, new BigDecimal("0"));

        when(cartApplicationService.removeItem(userId, productId)).thenReturn(cartDTO);

        // When & Then
        mockMvc.perform(delete("/api/v1/carts/items/{productId}", productId)
                .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void shouldClearCart() throws Exception {
        // Given
        String userId = "user_001";

        // When & Then
        mockMvc.perform(delete("/api/v1/carts")
                .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private CartDTO createCartDTO(String userId, int itemCount, BigDecimal totalAmount) {
        CartDTO dto = new CartDTO();
        dto.setUserId(userId);
        dto.setItemCount(itemCount);
        dto.setTotalAmount(totalAmount);
        dto.setCurrency("CNY");
        dto.setUpdateTime(LocalDateTime.now());
        dto.setItems(new ArrayList<CartItemDTO>());

        if (itemCount > 0) {
            CartItemDTO item1 = new CartItemDTO();
            item1.setProductId("prod_001");
            item1.setProductName("Test Product 1");
            item1.setUnitPrice(new BigDecimal("99.99"));
            item1.setQuantity(2);
            item1.setSubtotal(new BigDecimal("199.98"));
            dto.getItems().add(item1);
        }

        if (itemCount > 1) {
            CartItemDTO item2 = new CartItemDTO();
            item2.setProductId("prod_002");
            item2.setProductName("Test Product 2");
            item2.setUnitPrice(new BigDecimal("50.00"));
            item2.setQuantity(2);
            item2.setSubtotal(new BigDecimal("100.00"));
            dto.getItems().add(item2);
        }

        return dto;
    }
}
