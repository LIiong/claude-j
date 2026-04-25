package com.claudej.domain.product.model.aggregate;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.product.model.valobj.ProductId;
import com.claudej.domain.product.model.valobj.ProductName;
import com.claudej.domain.product.model.valobj.ProductStatus;
import com.claudej.domain.product.model.valobj.SKU;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    // === 创建商品测试 ===

    @Test
    void should_createProduct_when_validParameters() {
        // Given
        ProductName name = new ProductName("Valid Product");
        SKU sku = new SKU("SKU001", 100);
        BigDecimal originalPrice = new BigDecimal("99.00");
        BigDecimal promotionalPrice = new BigDecimal("79.00");
        String description = "Test product description";

        // When
        Product product = Product.create(name, sku, originalPrice, promotionalPrice, description);

        // Then
        assertThat(product.getProductId()).isNotNull();
        assertThat(product.getName()).isEqualTo(name);
        assertThat(product.getSku()).isEqualTo(sku);
        assertThat(product.getOriginalPrice().getAmount()).isEqualByComparingTo(originalPrice);
        assertThat(product.getPromotionalPrice().getAmount()).isEqualByComparingTo(promotionalPrice);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(product.getDescription()).isEqualTo(description);
        assertThat(product.getCreateTime()).isNotNull();
        assertThat(product.getUpdateTime()).isNotNull();
    }

    @Test
    void should_createProduct_when_promotionalPriceIsNull() {
        // Given
        ProductName name = new ProductName("Valid Product");
        SKU sku = new SKU("SKU001", 100);
        BigDecimal originalPrice = new BigDecimal("99.00");

        // When
        Product product = Product.create(name, sku, originalPrice, null, null);

        // Then
        assertThat(product.getPromotionalPrice()).isNull();
        assertThat(product.getEffectivePrice().getAmount()).isEqualByComparingTo(originalPrice);
    }

    @Test
    void should_throwException_when_originalPriceIsNegative() {
        // Given
        ProductName name = new ProductName("Valid Product");
        SKU sku = new SKU("SKU001", 100);
        BigDecimal negativePrice = BigDecimal.valueOf(-10);

        // When & Then
        assertThatThrownBy(() -> Product.create(name, sku, negativePrice, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_PRICE_NEGATIVE);
    }

    @Test
    void should_throwException_when_originalPriceIsZero() {
        // Given
        ProductName name = new ProductName("Valid Product");
        SKU sku = new SKU("SKU001", 100);
        BigDecimal zeroPrice = BigDecimal.ZERO;

        // When & Then
        assertThatThrownBy(() -> Product.create(name, sku, zeroPrice, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_PRICE_ZERO);
    }

    @Test
    void should_throwException_when_promotionalPriceIsNegative() {
        // Given
        ProductName name = new ProductName("Valid Product");
        SKU sku = new SKU("SKU001", 100);
        BigDecimal originalPrice = new BigDecimal("99.00");
        BigDecimal negativePromoPrice = BigDecimal.valueOf(-10);

        // When & Then
        assertThatThrownBy(() -> Product.create(name, sku, originalPrice, negativePromoPrice, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_PRICE_NEGATIVE);
    }

    @Test
    void should_throwException_when_promotionalPriceIsZero() {
        // Given
        ProductName name = new ProductName("Valid Product");
        SKU sku = new SKU("SKU001", 100);
        BigDecimal originalPrice = new BigDecimal("99.00");
        BigDecimal zeroPromoPrice = BigDecimal.ZERO;

        // When & Then
        assertThatThrownBy(() -> Product.create(name, sku, originalPrice, zeroPromoPrice, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_PRICE_ZERO);
    }

    // === 状态转换测试 ===

    @Test
    void should_activate_when_statusIsDraft() {
        // Given
        Product product = createTestProduct();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);

        // When
        product.activate();

        // Then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void should_activate_when_statusIsInactive() {
        // Given
        Product product = createTestProduct();
        product.activate();
        product.deactivate();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);

        // When
        product.activate();

        // Then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void should_throwException_when_activateFromActive() {
        // Given
        Product product = createTestProduct();
        product.activate();

        // When & Then
        assertThatThrownBy(() -> product.activate())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许上架");
    }

    @Test
    void should_deactivate_when_statusIsActive() {
        // Given
        Product product = createTestProduct();
        product.activate();

        // When
        product.deactivate();

        // Then
        assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);
    }

    @Test
    void should_throwException_when_deactivateFromDraft() {
        // Given
        Product product = createTestProduct();

        // When & Then
        assertThatThrownBy(() -> product.deactivate())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许下架");
    }

    @Test
    void should_throwException_when_deactivateFromInactive() {
        // Given
        Product product = createTestProduct();
        product.activate();
        product.deactivate();

        // When & Then
        assertThatThrownBy(() -> product.deactivate())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许下架");
    }

    // === 调价测试 ===

    @Test
    void should_updatePrice_when_statusIsDraft() {
        // Given
        Product product = createTestProduct();
        BigDecimal newOriginalPrice = new BigDecimal("129.00");
        BigDecimal newPromotionalPrice = new BigDecimal("99.00");

        // When
        product.updatePrice(newOriginalPrice, newPromotionalPrice);

        // Then
        assertThat(product.getOriginalPrice().getAmount()).isEqualByComparingTo(newOriginalPrice);
        assertThat(product.getPromotionalPrice().getAmount()).isEqualByComparingTo(newPromotionalPrice);
    }

    @Test
    void should_updatePrice_when_statusIsActive() {
        // Given
        Product product = createTestProduct();
        product.activate();
        BigDecimal newOriginalPrice = new BigDecimal("129.00");

        // When
        product.updatePrice(newOriginalPrice, null);

        // Then
        assertThat(product.getOriginalPrice().getAmount()).isEqualByComparingTo(newOriginalPrice);
    }

    @Test
    void should_updatePrice_when_statusIsInactive() {
        // Given
        Product product = createTestProduct();
        product.activate();
        product.deactivate();
        BigDecimal newOriginalPrice = new BigDecimal("150.00");

        // When
        product.updatePrice(newOriginalPrice, null);

        // Then
        assertThat(product.getOriginalPrice().getAmount()).isEqualByComparingTo(newOriginalPrice);
    }

    @Test
    void should_throwException_when_updatePriceWithNegativeValue() {
        // Given
        Product product = createTestProduct();
        BigDecimal negativePrice = BigDecimal.valueOf(-10);

        // When & Then
        assertThatThrownBy(() -> product.updatePrice(negativePrice, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_PRICE_NEGATIVE);
    }

    @Test
    void should_throwException_when_updatePriceWithZeroValue() {
        // Given
        Product product = createTestProduct();
        BigDecimal zeroPrice = BigDecimal.ZERO;

        // When & Then
        assertThatThrownBy(() -> product.updatePrice(zeroPrice, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_PRICE_ZERO);
    }

    // === 有效价格测试 ===

    @Test
    void should_getEffectivePrice_when_promotionalPriceExists() {
        // Given
        Product product = createTestProductWithPromo();

        // When
        BigDecimal effectivePrice = product.getEffectivePrice().getAmount();

        // Then
        assertThat(effectivePrice).isEqualByComparingTo(new BigDecimal("79.00"));
    }

    @Test
    void should_getEffectivePrice_when_promotionalPriceIsNull() {
        // Given
        Product product = createTestProduct();

        // When
        BigDecimal effectivePrice = product.getEffectivePrice().getAmount();

        // Then
        assertThat(effectivePrice).isEqualByComparingTo(new BigDecimal("99.00"));
    }

    // === 重建聚合测试 ===

    @Test
    void should_reconstruct_when_validParameters() {
        // Given
        Long id = 1L;
        ProductId productId = new ProductId("product-uuid-123");
        ProductName name = new ProductName("Test Product");
        SKU sku = new SKU("SKU001", 50);
        BigDecimal originalPrice = new BigDecimal("99.00");
        BigDecimal promotionalPrice = new BigDecimal("79.00");
        ProductStatus status = ProductStatus.ACTIVE;
        String description = "Test description";
        java.time.LocalDateTime createTime = java.time.LocalDateTime.now().minusDays(1);
        java.time.LocalDateTime updateTime = java.time.LocalDateTime.now();

        // When
        Product product = Product.reconstruct(id, productId, name, sku, originalPrice, promotionalPrice, status, description, createTime, updateTime);

        // Then
        assertThat(product.getId()).isEqualTo(id);
        assertThat(product.getProductId()).isEqualTo(productId);
        assertThat(product.getName()).isEqualTo(name);
        assertThat(product.getSku()).isEqualTo(sku);
        assertThat(product.getOriginalPrice().getAmount()).isEqualByComparingTo(originalPrice);
        assertThat(product.getPromotionalPrice().getAmount()).isEqualByComparingTo(promotionalPrice);
        assertThat(product.getStatus()).isEqualTo(status);
        assertThat(product.getDescription()).isEqualTo(description);
        assertThat(product.getCreateTime()).isEqualTo(createTime);
        assertThat(product.getUpdateTime()).isEqualTo(updateTime);
    }

    // === 辅助方法 ===

    private Product createTestProduct() {
        ProductName name = new ProductName("Test Product");
        SKU sku = new SKU("SKU001", 100);
        BigDecimal originalPrice = new BigDecimal("99.00");
        return Product.create(name, sku, originalPrice, null, "Test description");
    }

    private Product createTestProductWithPromo() {
        ProductName name = new ProductName("Test Product");
        SKU sku = new SKU("SKU001", 100);
        BigDecimal originalPrice = new BigDecimal("99.00");
        BigDecimal promotionalPrice = new BigDecimal("79.00");
        return Product.create(name, sku, originalPrice, promotionalPrice, "Test description");
    }
}