package com.claudej.adapter.product.web;

import com.claudej.adapter.common.GlobalExceptionHandler;
import com.claudej.adapter.product.web.request.CreateProductRequest;
import com.claudej.adapter.product.web.request.UpdatePriceRequest;
import com.claudej.application.common.dto.PageDTO;
import com.claudej.application.product.dto.ProductDTO;
import com.claudej.application.product.service.ProductApplicationService;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.common.model.valobj.PageRequest;
import com.claudej.domain.product.model.valobj.ProductStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {ProductController.class, GlobalExceptionHandler.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductApplicationService productApplicationService;

    private ProductDTO mockProductDTO;

    @BeforeEach
    void setUp() {
        mockProductDTO = new ProductDTO();
        mockProductDTO.setProductId("PROD123456");
        mockProductDTO.setName("测试商品");
        mockProductDTO.setDescription("测试商品描述");
        mockProductDTO.setSkuCode("SKU001");
        mockProductDTO.setStock(100);
        mockProductDTO.setOriginalPrice(new BigDecimal("99.00"));
        mockProductDTO.setPromotionalPrice(new BigDecimal("79.00"));
        mockProductDTO.setEffectivePrice(new BigDecimal("79.00"));
        mockProductDTO.setCurrency("CNY");
        mockProductDTO.setStatus("DRAFT");
        mockProductDTO.setCreateTime(LocalDateTime.now());
        mockProductDTO.setUpdateTime(LocalDateTime.now());
    }

    @Test
    void should_return200_when_createProductSuccess() throws Exception {
        // Given
        CreateProductRequest request = new CreateProductRequest();
        request.setName("测试商品");
        request.setSkuCode("SKU001");
        request.setStock(100);
        request.setOriginalPrice(new BigDecimal("99.00"));
        request.setPromotionalPrice(new BigDecimal("79.00"));
        request.setDescription("测试商品描述");

        when(productApplicationService.createProduct(any())).thenReturn(mockProductDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.productId", is("PROD123456")))
                .andExpect(jsonPath("$.data.name", is("测试商品")))
                .andExpect(jsonPath("$.data.status", is("DRAFT")));
    }

    @Test
    void should_return400_when_createProductWithInvalidInput() throws Exception {
        // Given
        CreateProductRequest request = new CreateProductRequest();
        request.setName("");  // Invalid: empty name
        request.setSkuCode("SKU001");
        request.setStock(100);
        request.setOriginalPrice(new BigDecimal("99.00"));

        // When & Then
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return200_when_getProductByIdSuccess() throws Exception {
        // Given
        when(productApplicationService.getProduct("PROD123456")).thenReturn(mockProductDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/products/PROD123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.productId", is("PROD123456")))
                .andExpect(jsonPath("$.data.name", is("测试商品")));
    }

    @Test
    void should_return404_when_getNonExistentProduct() throws Exception {
        // Given
        when(productApplicationService.getProduct("NONEXISTENT"))
                .thenThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/api/v1/products/NONEXISTENT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("PRODUCT_NOT_FOUND")));
    }

    @Test
    void should_return200_when_updatePriceSuccess() throws Exception {
        // Given
        UpdatePriceRequest request = new UpdatePriceRequest();
        request.setOriginalPrice(new BigDecimal("129.00"));
        request.setPromotionalPrice(null);

        mockProductDTO.setOriginalPrice(new BigDecimal("129.00"));
        mockProductDTO.setPromotionalPrice(null);
        mockProductDTO.setEffectivePrice(new BigDecimal("129.00"));

        when(productApplicationService.updatePrice(eq("PROD123456"), any())).thenReturn(mockProductDTO);

        // When & Then
        mockMvc.perform(put("/api/v1/products/PROD123456/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.originalPrice", is(129.00)));
    }

    @Test
    void should_return200_when_activateProductSuccess() throws Exception {
        // Given
        mockProductDTO.setStatus("ACTIVE");
        when(productApplicationService.activateProduct("PROD123456")).thenReturn(mockProductDTO);

        // When & Then
        mockMvc.perform(put("/api/v1/products/PROD123456/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));
    }

    @Test
    void should_return400_when_activateProductWithInvalidStatus() throws Exception {
        // Given
        when(productApplicationService.activateProduct("PROD123456"))
                .thenThrow(new BusinessException(ErrorCode.INVALID_PRODUCT_STATUS_TRANSITION));

        // When & Then
        mockMvc.perform(put("/api/v1/products/PROD123456/activate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("INVALID_PRODUCT_STATUS_TRANSITION")));
    }

    @Test
    void should_return200_when_deactivateProductSuccess() throws Exception {
        // Given
        mockProductDTO.setStatus("INACTIVE");
        when(productApplicationService.deactivateProduct("PROD123456")).thenReturn(mockProductDTO);

        // When & Then
        mockMvc.perform(put("/api/v1/products/PROD123456/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("INACTIVE")));
    }

    @Test
    void should_return200_when_listProductsByStatusSuccess() throws Exception {
        // Given
        when(productApplicationService.listProducts(ProductStatus.DRAFT))
                .thenReturn(Arrays.asList(mockProductDTO));

        // When & Then
        mockMvc.perform(get("/api/v1/products")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].status", is("DRAFT")));
    }

    @Test
    void should_return200_when_listProductsPagedSuccess() throws Exception {
        // Given
        PageDTO<ProductDTO> mockPageDTO = new PageDTO<ProductDTO>();
        mockPageDTO.setContent(Arrays.asList(mockProductDTO));
        mockPageDTO.setTotalElements(1L);
        mockPageDTO.setTotalPages(1);
        mockPageDTO.setPage(0);
        mockPageDTO.setSize(10);
        mockPageDTO.setFirst(true);
        mockPageDTO.setLast(true);
        mockPageDTO.setEmpty(false);

        when(productApplicationService.listProducts(any(PageRequest.class))).thenReturn(mockPageDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[0].productId", is("PROD123456")));
    }
}