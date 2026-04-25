package com.claudej.application.product.service;

import com.claudej.application.common.assembler.PageAssembler;
import com.claudej.application.common.dto.PageDTO;
import com.claudej.application.product.assembler.ProductAssembler;
import com.claudej.application.product.command.CreateProductCommand;
import com.claudej.application.product.command.UpdatePriceCommand;
import com.claudej.application.product.dto.ProductDTO;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.model.valobj.Page;
import com.claudej.domain.common.model.valobj.PageRequest;
import com.claudej.domain.common.model.valobj.SortDirection;
import com.claudej.domain.product.model.aggregate.Product;
import com.claudej.domain.product.model.valobj.ProductId;
import com.claudej.domain.product.model.valobj.ProductStatus;
import com.claudej.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductApplicationServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductAssembler productAssembler;

    @Mock
    private PageAssembler pageAssembler;

    @InjectMocks
    private ProductApplicationService productApplicationService;

    private CreateProductCommand createCommand;
    private UpdatePriceCommand updatePriceCommand;
    private Product mockProduct;
    private ProductDTO mockProductDTO;

    @BeforeEach
    void setUp() {
        createCommand = new CreateProductCommand();
        createCommand.setName("测试商品");
        createCommand.setSkuCode("SKU001");
        createCommand.setStock(100);
        createCommand.setOriginalPrice(new BigDecimal("99.00"));
        createCommand.setPromotionalPrice(new BigDecimal("79.00"));
        createCommand.setDescription("测试商品描述");

        updatePriceCommand = new UpdatePriceCommand();
        updatePriceCommand.setOriginalPrice(new BigDecimal("129.00"));
        updatePriceCommand.setPromotionalPrice(null);

        mockProduct = Product.create(
                new com.claudej.domain.product.model.valobj.ProductName("测试商品"),
                new com.claudej.domain.product.model.valobj.SKU("SKU001", 100),
                new BigDecimal("99.00"),
                new BigDecimal("79.00"),
                "测试商品描述"
        );

        mockProductDTO = new ProductDTO();
        mockProductDTO.setProductId(mockProduct.getProductIdValue());
        mockProductDTO.setName("测试商品");
        mockProductDTO.setSkuCode("SKU001");
        mockProductDTO.setStock(100);
        mockProductDTO.setOriginalPrice(new BigDecimal("99.00"));
        mockProductDTO.setPromotionalPrice(new BigDecimal("79.00"));
        mockProductDTO.setStatus("DRAFT");
        mockProductDTO.setDescription("测试商品描述");
    }

    @Test
    void should_createProduct_when_validCommandProvided() {
        // Given
        when(productRepository.save(any(Product.class))).thenReturn(mockProduct);
        when(productAssembler.toDTO(any(Product.class))).thenReturn(mockProductDTO);

        // When
        ProductDTO result = productApplicationService.createProduct(createCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("测试商品");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void should_throwException_when_createProductWithNullCommand() {
        // When & Then
        assertThatThrownBy(() -> productApplicationService.createProduct(null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_returnProduct_when_getProductByIdExists() {
        // Given
        when(productRepository.findByProductId(any(ProductId.class))).thenReturn(Optional.of(mockProduct));
        when(productAssembler.toDTO(any(Product.class))).thenReturn(mockProductDTO);

        // When
        ProductDTO result = productApplicationService.getProduct(mockProduct.getProductIdValue());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(mockProduct.getProductIdValue());
    }

    @Test
    void should_throwException_when_getProductByIdNotExists() {
        // Given
        when(productRepository.findByProductId(any(ProductId.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productApplicationService.getProduct("NONEXISTENT"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_updatePrice_when_validCommandProvided() {
        // Given
        when(productRepository.findByProductId(any(ProductId.class))).thenReturn(Optional.of(mockProduct));
        when(productRepository.save(any(Product.class))).thenReturn(mockProduct);
        when(productAssembler.toDTO(any(Product.class))).thenReturn(mockProductDTO);

        // When
        ProductDTO result = productApplicationService.updatePrice(mockProduct.getProductIdValue(), updatePriceCommand);

        // Then
        assertThat(result).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void should_throwException_when_updatePriceWithNullCommand() {
        // When & Then
        assertThatThrownBy(() -> productApplicationService.updatePrice(mockProduct.getProductIdValue(), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_activateProduct_when_productExistsAndCanBeActivated() {
        // Given
        when(productRepository.findByProductId(any(ProductId.class))).thenReturn(Optional.of(mockProduct));
        when(productRepository.save(any(Product.class))).thenReturn(mockProduct);
        when(productAssembler.toDTO(any(Product.class))).thenReturn(mockProductDTO);

        // When
        ProductDTO result = productApplicationService.activateProduct(mockProduct.getProductIdValue());

        // Then
        assertThat(result).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void should_throwException_when_activateProductNotExists() {
        // Given
        when(productRepository.findByProductId(any(ProductId.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productApplicationService.activateProduct("NONEXISTENT"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_deactivateProduct_when_productExistsAndCanBeDeactivated() {
        // Given - 需要先上架才能下架
        mockProduct.activate();
        when(productRepository.findByProductId(any(ProductId.class))).thenReturn(Optional.of(mockProduct));
        when(productRepository.save(any(Product.class))).thenReturn(mockProduct);
        mockProductDTO.setStatus("INACTIVE");
        when(productAssembler.toDTO(any(Product.class))).thenReturn(mockProductDTO);

        // When
        ProductDTO result = productApplicationService.deactivateProduct(mockProduct.getProductIdValue());

        // Then
        assertThat(result).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void should_throwException_when_deactivateProductNotExists() {
        // Given
        when(productRepository.findByProductId(any(ProductId.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productApplicationService.deactivateProduct("NONEXISTENT"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_returnProducts_when_listByStatus() {
        // Given
        when(productRepository.findByStatus(ProductStatus.DRAFT)).thenReturn(Arrays.asList(mockProduct));
        when(productAssembler.toDTOList(any())).thenReturn(Arrays.asList(mockProductDTO));

        // When
        java.util.List<ProductDTO> results = productApplicationService.listProducts(ProductStatus.DRAFT);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo("DRAFT");
    }

    @Test
    void should_returnEmptyList_when_listByStatusNoProducts() {
        // Given
        when(productRepository.findByStatus(ProductStatus.INACTIVE)).thenReturn(Collections.emptyList());
        when(productAssembler.toDTOList(any())).thenReturn(Collections.emptyList());

        // When
        java.util.List<ProductDTO> results = productApplicationService.listProducts(ProductStatus.INACTIVE);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void should_returnPageDTO_when_listProductsPageable() {
        // Given
        PageRequest pageRequest = new PageRequest(0, 10, null, SortDirection.ASC);
        Page<Product> mockPage = new Page<Product>(
                Arrays.asList(mockProduct),
                1L,
                0,
                10
        );
        PageDTO<ProductDTO> mockPageDTO = new PageDTO<ProductDTO>();
        mockPageDTO.setContent(Arrays.asList(mockProductDTO));
        mockPageDTO.setTotalElements(1L);
        mockPageDTO.setTotalPages(1);
        mockPageDTO.setPage(0);
        mockPageDTO.setSize(10);

        when(productRepository.findAll(any(PageRequest.class))).thenReturn(mockPage);
        when(pageAssembler.toPageDTO(any(Page.class), any())).thenReturn(mockPageDTO);

        // When
        PageDTO<ProductDTO> result = productApplicationService.listProducts(pageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1L);
    }
}