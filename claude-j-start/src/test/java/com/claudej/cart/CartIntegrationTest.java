package com.claudej.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 购物车服务全链路集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String createAddItemRequest(String userId, String productId, String productName, 
                                         String unitPrice, int quantity) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("userId", userId);
        request.put("productId", productId);
        request.put("productName", productName);
        request.put("unitPrice", unitPrice);
        request.put("quantity", quantity);
        return request.toString();
    }

    private String createUpdateQuantityRequest(String userId, String productId, int quantity) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("userId", userId);
        request.put("productId", productId);
        request.put("quantity", quantity);
        return request.toString();
    }

    @Test
    void should_createCartAndAddItem_when_fullFlow() throws Exception {
        // Given - 添加商品到购物车请求
        String requestJson = createAddItemRequest("USER001", "PROD001", "iPhone 15", "6999.00", 1);

        // When - 添加商品到购物车
        mockMvc.perform(post("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is("USER001")))
                .andExpect(jsonPath("$.data.itemCount", is(1)))
                .andExpect(jsonPath("$.data.totalAmount").exists())
                .andExpect(jsonPath("$.data.items[0].productId", is("PROD001")))
                .andExpect(jsonPath("$.data.items[0].productName", is("iPhone 15")))
                .andExpect(jsonPath("$.data.items[0].quantity", is(1)));

        // When - 查询购物车
        mockMvc.perform(get("/api/v1/carts")
                        .param("userId", "USER001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is("USER001")))
                .andExpect(jsonPath("$.data.itemCount", is(1)))
                .andExpect(jsonPath("$.data.items[0].productId", is("PROD001")));
    }

    @Test
    void should_updateItemQuantity_when_validRequest() throws Exception {
        // Given - 先添加商品
        String addRequest = createAddItemRequest("USER002", "PROD002", "MacBook Pro", "14999.00", 1);

        mockMvc.perform(post("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addRequest))
                .andExpect(status().isOk());

        // When - 更新商品数量
        String updateRequest = createUpdateQuantityRequest("USER002", "PROD002", 3);

        mockMvc.perform(put("/api/v1/carts/items/quantity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is("USER002")))
                .andExpect(jsonPath("$.data.items[0].quantity", is(3)))
                .andExpect(jsonPath("$.data.items[0].subtotal").exists());
    }

    @Test
    void should_removeItem_when_deleteRequest() throws Exception {
        // Given - 先添加商品
        String addRequest = createAddItemRequest("USER003", "PROD003", "AirPods Pro", "1999.00", 2);

        mockMvc.perform(post("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addRequest))
                .andExpect(status().isOk());

        // Verify - 购物车中有商品
        mockMvc.perform(get("/api/v1/carts")
                        .param("userId", "USER003"))
                .andExpect(jsonPath("$.data.itemCount", is(1)));

        // When - 删除商品
        mockMvc.perform(delete("/api/v1/carts/items/{productId}", "PROD003")
                        .param("userId", "USER003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.itemCount", is(0)));
    }

    @Test
    void should_clearCart_when_clearRequest() throws Exception {
        // Given - 添加多个商品
        String request1 = createAddItemRequest("USER004", "PROD004", "iPad Pro", "6999.00", 1);
        String request2 = createAddItemRequest("USER004", "PROD005", "Apple Pencil", "999.00", 1);

        mockMvc.perform(post("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request1))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request2))
                .andExpect(status().isOk());

        // Verify - 购物车中有2个商品
        mockMvc.perform(get("/api/v1/carts")
                        .param("userId", "USER004"))
                .andExpect(jsonPath("$.data.itemCount", is(2)));

        // When - 清空购物车
        mockMvc.perform(delete("/api/v1/carts")
                        .param("userId", "USER004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Then - 购物车为空
        mockMvc.perform(get("/api/v1/carts")
                        .param("userId", "USER004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount", is(0)))
                .andExpect(jsonPath("$.data.totalAmount").exists());
    }

    @Test
    void should_addMultipleItems_when_sameUser() throws Exception {
        // Given - 同一用户添加多个商品
        String request1 = createAddItemRequest("USER005", "PROD006", "Magic Keyboard", "2399.00", 1);
        String request2 = createAddItemRequest("USER005", "PROD007", "Magic Mouse", "699.00", 2);

        // When - 添加第一个商品
        mockMvc.perform(post("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount", is(1)));

        // When - 添加第二个商品
        mockMvc.perform(post("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount", is(2)))
                .andExpect(jsonPath("$.data.items[0].productId", is("PROD006")))
                .andExpect(jsonPath("$.data.items[1].productId", is("PROD007")));

        // Then - 验证购物车总价
        mockMvc.perform(get("/api/v1/carts")
                        .param("userId", "USER005"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount", is(2)))
                .andExpect(jsonPath("$.data.totalAmount").exists());
    }

    @Test
    void should_updateQuantity_when_addSameProduct() throws Exception {
        // Given - 添加商品
        String request = createAddItemRequest("USER006", "PROD008", "USB-C Cable", "149.00", 2);

        mockMvc.perform(post("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantity", is(2)));

        // When - 再次添加相同商品（数量累加）
        String request2 = createAddItemRequest("USER006", "PROD008", "USB-C Cable", "149.00", 3);
        mockMvc.perform(post("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount", is(1)))
                .andExpect(jsonPath("$.data.items[0].quantity", is(5))); // 2 + 3 = 5
    }

    @Test
    void should_return400_when_addWithInvalidData() throws Exception {
        // Given - 无效请求（单价为0）
        String request = createAddItemRequest("USER007", "PROD009", "Test Product", "0", 1);

        mockMvc.perform(post("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_addWithInvalidQuantity() throws Exception {
        // Given - 无效请求（数量为0）
        String request = createAddItemRequest("USER008", "PROD010", "Test Product", "100.00", 0);

        mockMvc.perform(post("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_calculateSubtotalCorrectly_when_multipleItems() throws Exception {
        // Given - 添加商品
        String request = createAddItemRequest("USER009", "PROD011", "Product A", "100.00", 3);

        MvcResult result = mockMvc.perform(post("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].subtotal", is(300.0))) // 100 * 3 = 300
                .andReturn();
    }
}
