package com.claudej.order;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 从购物车创建订单全链路集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class OrderFromCartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String createInventoryRequest(String productId, String skuCode, int initialStock) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("productId", productId);
        request.put("skuCode", skuCode);
        request.put("initialStock", initialStock);
        return request.toString();
    }

    private void createInventoryForProduct(String productId, int initialStock) throws Exception {
        String skuCode = "SKU_" + productId;
        String inventoryRequest = createInventoryRequest(productId, skuCode, initialStock);
        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inventoryRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

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

    private String createOrderFromCartRequest(String customerId) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("customerId", customerId);
        return request.toString();
    }

    @Test
    void should_createOrderFromCart_when_cartExistsWithItems() throws Exception {
        String userId = "USER_CART_001";
        String productId = "PROD_CART_001";

        // Given - 先创建库存记录
        createInventoryForProduct(productId, 100);

        // Given - 添加商品到购物车
        String addItemRequest = createAddItemRequest(userId, productId, "iPhone 15 Pro", "8999.00", 2);

        mockMvc.perform(post("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Verify - 购物车中有商品
        mockMvc.perform(get("/api/v1/carts")
                        .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount", is(1)))
                .andExpect(jsonPath("$.data.items[0].productId", is(productId)))
                .andExpect(jsonPath("$.data.items[0].quantity", is(2)));

        // When - 从购物车创建订单
        String orderRequest = createOrderFromCartRequest(userId);

        MvcResult result = mockMvc.perform(post("/api/v1/orders/from-cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.customerId", is(userId)))
                .andExpect(jsonPath("$.data.status", is("CREATED")))
                .andExpect(jsonPath("$.data.totalAmount").exists())
                .andExpect(jsonPath("$.data.currency", is("CNY")))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()", is(1)))
                .andExpect(jsonPath("$.data.items[0].productId", is(productId)))
                .andExpect(jsonPath("$.data.items[0].productName", is("iPhone 15 Pro")))
                .andExpect(jsonPath("$.data.items[0].quantity", is(2)))
                .andExpect(jsonPath("$.data.items[0].unitPrice").exists())
                .andExpect(jsonPath("$.data.items[0].subtotal").exists())
                .andReturn();

        // Extract orderId for verification
        String responseContent = result.getResponse().getContentAsString();
        ObjectNode responseJson = (ObjectNode) objectMapper.readTree(responseContent);
        String orderId = responseJson.path("data").path("orderId").asText();

        // Then - 验证购物车已被清空
        mockMvc.perform(get("/api/v1/carts")
                        .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount", is(0)))
                .andExpect(jsonPath("$.data.totalAmount").exists());

        // Then - 验证订单可以查询到
        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.orderId", is(orderId)))
                .andExpect(jsonPath("$.data.status", is("CREATED")));
    }

    @Test
    void should_createOrderWithCorrectTotal_when_multipleItemsInCart() throws Exception {
        String userId = "USER_CART_002";

        // Given - 先创建库存记录
        createInventoryForProduct("PROD_MULTI_001", 100);
        createInventoryForProduct("PROD_MULTI_002", 100);

        // Given - 添加多个商品到购物车
        String addItem1 = createAddItemRequest(userId, "PROD_MULTI_001", "Product A", "100.00", 3);
        String addItem2 = createAddItemRequest(userId, "PROD_MULTI_002", "Product B", "200.00", 2);

        mockMvc.perform(post("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItem1))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItem2))
                .andExpect(status().isOk());

        // Verify - 购物车中有2个商品
        mockMvc.perform(get("/api/v1/carts")
                        .param("userId", userId))
                .andExpect(jsonPath("$.data.itemCount", is(2)));

        // When - 从购物车创建订单
        String orderRequest = createOrderFromCartRequest(userId);

        mockMvc.perform(post("/api/v1/orders/from-cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items.length()", is(2)))
                .andExpect(jsonPath("$.data.totalAmount").exists());
    }

    @Test
    void should_return404_when_cartNotFound() throws Exception {
        // When - 使用不存在的用户ID创建订单
        String orderRequest = createOrderFromCartRequest("NON_EXISTENT_USER_999");

        mockMvc.perform(post("/api/v1/orders/from-cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("CART_NOT_FOUND")));
    }

    @Test
    void should_return400_when_cartIsEmpty() throws Exception {
        String userId = "USER_EMPTY_CART";

        // Given - 先创建库存记录
        createInventoryForProduct("PROD_TEMP", 100);

        // Given - 创建空购物车（查询会创建新购物车）
        // 先添加商品
        String addItem = createAddItemRequest(userId, "PROD_TEMP", "Temp Product", "100.00", 1);
        mockMvc.perform(post("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItem))
                .andExpect(status().isOk());

        // 然后清空购物车
        mockMvc.perform(delete("/api/v1/carts")
                        .param("userId", userId))
                .andExpect(status().isOk());

        // When - 从空购物车创建订单
        String orderRequest = createOrderFromCartRequest(userId);

        mockMvc.perform(post("/api/v1/orders/from-cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("CART_EMPTY")));
    }

    @Test
    void should_return400_when_customerIdIsBlank() throws Exception {
        // Given - 空的 customerId
        String orderRequest = createOrderFromCartRequest("");

        // When & Then
        mockMvc.perform(post("/api/v1/orders/from-cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void should_clearCartSuccessfully_when_orderCreated() throws Exception {
        String userId = "USER_CART_CLEAR_001";

        // Given - 先创建库存记录
        createInventoryForProduct("PROD_CLEAR_001", 100);

        // Given - 添加商品到购物车
        String addItem = createAddItemRequest(userId, "PROD_CLEAR_001", "Clear Test Product", "500.00", 1);
        mockMvc.perform(post("/api/v1/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItem))
                .andExpect(status().isOk());

        // Verify - 购物车中有商品
        mockMvc.perform(get("/api/v1/carts")
                        .param("userId", userId))
                .andExpect(jsonPath("$.data.itemCount", is(1)));

        // When - 从购物车创建订单
        String orderRequest = createOrderFromCartRequest(userId);
        mockMvc.perform(post("/api/v1/orders/from-cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Then - 购物车应该被清空
        mockMvc.perform(get("/api/v1/carts")
                        .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount", is(0)))
                .andExpect(jsonPath("$.data.items.length()", is(0)));
    }
}
