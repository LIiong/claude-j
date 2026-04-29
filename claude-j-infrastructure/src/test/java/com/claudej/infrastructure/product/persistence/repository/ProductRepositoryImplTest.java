package com.claudej.infrastructure.product.persistence.repository;

import com.claudej.domain.common.model.valobj.Page;
import com.claudej.domain.common.model.valobj.PageRequest;
import com.claudej.domain.common.model.valobj.SortDirection;
import com.claudej.domain.product.model.aggregate.Product;
import com.claudej.domain.product.model.valobj.ProductId;
import com.claudej.domain.product.model.valobj.ProductName;
import com.claudej.domain.product.model.valobj.ProductStatus;
import com.claudej.domain.product.model.valobj.SKU;
import com.claudej.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProductRepositoryImplTest {

    @SpringBootApplication(scanBasePackages = {"com.claudej.infrastructure", "com.claudej.application"})
    @MapperScan("com.claudej.infrastructure.**.mapper")
    static class TestConfig {
    }

    @Autowired
    private ProductRepositoryImpl productRepository;

    @Test
    void should_saveProduct_when_createNewProduct() {
        // Given
        ProductName name = new ProductName("测试商品");
        SKU sku = new SKU("SKU001", 100);
        Product product = Product.create(name, sku, new BigDecimal("99.00"), new BigDecimal("79.00"), "测试描述");

        // When
        Product saved = productRepository.save(product);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProductIdValue()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ProductStatus.DRAFT);
    }

    @Test
    void should_findProductById_when_productExists() {
        // Given
        ProductName name = new ProductName("测试商品");
        SKU sku = new SKU("SKU002", 50);
        Product product = Product.create(name, sku, new BigDecimal("100.00"), null, "描述");
        Product saved = productRepository.save(product);

        // When
        Optional<Product> found = productRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName().getValue()).isEqualTo("测试商品");
        assertThat(found.get().getSku().getSkuCode()).isEqualTo("SKU002");
    }

    @Test
    void should_returnEmpty_when_findByIdNotExists() {
        // When
        Optional<Product> found = productRepository.findById(99999L);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void should_findByProductId_when_productExists() {
        // Given
        ProductName name = new ProductName("查询商品");
        SKU sku = new SKU("SKU003", 200);
        Product product = Product.create(name, sku, new BigDecimal("50.00"), new BigDecimal("45.00"), null);
        Product saved = productRepository.save(product);

        // When
        Optional<Product> found = productRepository.findByProductId(new ProductId(saved.getProductIdValue()));

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName().getValue()).isEqualTo("查询商品");
    }

    @Test
    void should_returnEmpty_when_findByProductIdNotExists() {
        // When
        Optional<Product> found = productRepository.findByProductId(new ProductId("NONEXISTENT"));

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void should_findByStatus_when_productsExistWithStatus() {
        // Given - 创建并上架一个商品
        ProductName name = new ProductName("上架商品");
        SKU sku = new SKU("SKU004", 30);
        Product product = Product.create(name, sku, new BigDecimal("200.00"), null, "上架测试");
        product.activate();
        Product saved = productRepository.save(product);

        // When
        List<Product> activeProducts = productRepository.findByStatus(ProductStatus.ACTIVE);

        // Then
        assertThat(activeProducts).isNotEmpty();
        assertThat(activeProducts.stream().anyMatch(p -> p.getProductIdValue().equals(saved.getProductIdValue()))).isTrue();
    }

    @Test
    void should_updateProduct_when_activateProduct() {
        // Given
        ProductName name = new ProductName("状态变更商品");
        SKU sku = new SKU("SKU005", 10);
        Product product = Product.create(name, sku, new BigDecimal("300.00"), null, null);
        Product saved = productRepository.save(product);

        // When
        saved.activate();
        Product updated = productRepository.save(saved);

        // Then
        assertThat(updated.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        Optional<Product> found = productRepository.findById(updated.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void should_updatePrice_when_updateProductPrice() {
        // Given
        ProductName name = new ProductName("调价商品");
        SKU sku = new SKU("SKU006", 5);
        Product product = Product.create(name, sku, new BigDecimal("100.00"), null, null);
        Product saved = productRepository.save(product);

        // When
        saved.updatePrice(new BigDecimal("150.00"), new BigDecimal("120.00"));
        Product updated = productRepository.save(saved);

        // Then
        assertThat(updated.getOriginalPrice().getAmount()).isEqualByComparingTo("150.00");
        assertThat(updated.getPromotionalPrice().getAmount()).isEqualByComparingTo("120.00");
    }

    @Test
    void should_returnPage_when_findAllWithPageRequest() {
        // Given - 创建多个商品
        for (int i = 1; i <= 5; i++) {
            ProductName name = new ProductName("分页商品P" + i);
            SKU sku = new SKU("SKU-P-" + i, 10);
            Product product = Product.create(name, sku, new BigDecimal("10.00"), null, null);
            productRepository.save(product);
        }

        PageRequest pageRequest = new PageRequest(0, 3, null, SortDirection.ASC);

        // When
        Page<Product> page = productRepository.findAll(pageRequest);

        // Then - 验证分页对象返回正确
        assertThat(page).isNotNull();
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getPage()).isEqualTo(0);
        assertThat(page.getSize()).isEqualTo(3);
        assertThat(page.isFirst()).isTrue();
    }
}