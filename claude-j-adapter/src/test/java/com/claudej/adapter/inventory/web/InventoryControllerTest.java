package com.claudej.adapter.inventory.web;

import com.claudej.adapter.common.GlobalExceptionHandler;
import com.claudej.adapter.inventory.web.request.AdjustStockRequest;
import com.claudej.adapter.inventory.web.request.CreateInventoryRequest;
import com.claudej.application.inventory.dto.InventoryDTO;
import com.claudej.application.inventory.service.InventoryApplicationService;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * InventoryController 测试
 */
@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {InventoryController.class, GlobalExceptionHandler.class})
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryApplicationService inventoryApplicationService;

    private InventoryDTO mockInventoryDTO;

    @BeforeEach
    void setUp() {
        mockInventoryDTO = new InventoryDTO();
        mockInventoryDTO.setInventoryId("INV123456");
        mockInventoryDTO.setProductId("PROD001");
        mockInventoryDTO.setSkuCode("SKU001");
        mockInventoryDTO.setAvailableStock(100);
        mockInventoryDTO.setReservedStock(0);
        mockInventoryDTO.setCreateTime(LocalDateTime.now());
        mockInventoryDTO.setUpdateTime(LocalDateTime.now());
    }

    @Test
    void should_return_200_when_createInventory_success() throws Exception {
        // Given
        CreateInventoryRequest request = new CreateInventoryRequest();
        request.setProductId("PROD001");
        request.setSkuCode("SKU001");
        request.setInitialStock(100);

        when(inventoryApplicationService.createInventory(any())).thenReturn(mockInventoryDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.inventoryId", is("INV123456")))
                .andExpect(jsonPath("$.data.productId", is("PROD001")))
                .andExpect(jsonPath("$.data.skuCode", is("SKU001")))
                .andExpect(jsonPath("$.data.availableStock", is(100)));
    }

    @Test
    void should_return_400_when_createInventory_withMissingProductId() throws Exception {
        // Given
        CreateInventoryRequest request = new CreateInventoryRequest();
        request.setProductId("");  // Invalid: empty productId
        request.setSkuCode("SKU001");
        request.setInitialStock(100);

        // When & Then
        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_createInventory_withMissingSkuCode() throws Exception {
        // Given
        CreateInventoryRequest request = new CreateInventoryRequest();
        request.setProductId("PROD001");
        request.setSkuCode("");  // Invalid: empty skuCode
        request.setInitialStock(100);

        // When & Then
        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_when_getInventoryById_success() throws Exception {
        // Given
        when(inventoryApplicationService.getInventoryById("INV123456")).thenReturn(mockInventoryDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/inventory/INV123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.inventoryId", is("INV123456")))
                .andExpect(jsonPath("$.data.productId", is("PROD001")));
    }

    @Test
    void should_return_404_when_getInventoryById_notFound() throws Exception {
        // Given
        when(inventoryApplicationService.getInventoryById("NOT-EXIST"))
                .thenThrow(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/api/v1/inventory/NOT-EXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("INVENTORY_NOT_FOUND")));
    }

    @Test
    void should_return_200_when_getInventoryByProductId_success() throws Exception {
        // Given
        when(inventoryApplicationService.getInventoryByProductId("PROD001")).thenReturn(mockInventoryDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/inventory/product/PROD001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.productId", is("PROD001")));
    }

    @Test
    void should_return_404_when_getInventoryByProductId_notFound() throws Exception {
        // Given
        when(inventoryApplicationService.getInventoryByProductId("PROD-NOT-EXIST"))
                .thenThrow(new BusinessException(ErrorCode.INVENTORY_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/api/v1/inventory/product/PROD-NOT-EXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("INVENTORY_NOT_FOUND")));
    }

    @Test
    void should_return_200_when_getInventoryBySkuCode_success() throws Exception {
        // Given
        when(inventoryApplicationService.getInventoryBySkuCode("SKU001")).thenReturn(mockInventoryDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/inventory/sku/SKU001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.skuCode", is("SKU001")));
    }

    @Test
    void should_return_200_when_adjustStock_success() throws Exception {
        // Given
        AdjustStockRequest request = new AdjustStockRequest();
        request.setAdjustment(50);
        request.setReason("入库");

        mockInventoryDTO.setAvailableStock(150);
        when(inventoryApplicationService.adjustStock(any())).thenReturn(mockInventoryDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/inventory/INV123456/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.availableStock", is(150)));
    }

    @Test
    void should_return_400_when_adjustStock_withMissingAdjustment() throws Exception {
        // Given
        AdjustStockRequest request = new AdjustStockRequest();
        // Missing: adjustment

        // When & Then
        mockMvc.perform(post("/api/v1/inventory/INV123456/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_adjustStock_resultNegative() throws Exception {
        // Given
        AdjustStockRequest request = new AdjustStockRequest();
        request.setAdjustment(-200);
        request.setReason("错误调整");

        when(inventoryApplicationService.adjustStock(any()))
                .thenThrow(new BusinessException(ErrorCode.STOCK_NEGATIVE));

        // When & Then
        mockMvc.perform(post("/api/v1/inventory/INV123456/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("STOCK_NEGATIVE")));
    }
}