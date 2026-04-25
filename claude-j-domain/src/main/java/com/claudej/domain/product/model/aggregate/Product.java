package com.claudej.domain.product.model.aggregate;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.product.model.valobj.Money;
import com.claudej.domain.product.model.valobj.ProductId;
import com.claudej.domain.product.model.valobj.ProductName;
import com.claudej.domain.product.model.valobj.ProductStatus;
import com.claudej.domain.product.model.valobj.SKU;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 商品聚合根 - 封装商品业务不变量
 */
@Getter
public class Product {

    private Long id;
    private ProductId productId;
    private ProductName name;
    private String description;
    private SKU sku;
    private Money originalPrice;
    private Money promotionalPrice;
    private ProductStatus status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private Product(ProductName name, SKU sku, BigDecimal originalPrice, BigDecimal promotionalPrice, String description, LocalDateTime createTime) {
        this.name = name;
        this.sku = sku;
        this.originalPrice = validatePrice(originalPrice);
        this.promotionalPrice = promotionalPrice != null ? validatePrice(promotionalPrice) : null;
        this.description = description;
        this.status = ProductStatus.DRAFT;
        this.createTime = createTime;
        this.updateTime = createTime;
    }

    /**
     * 工厂方法：创建新商品
     */
    public static Product create(ProductName name, SKU sku, BigDecimal originalPrice, BigDecimal promotionalPrice, String description) {
        if (name == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NAME_EMPTY, "商品名称不能为空");
        }
        if (sku == null) {
            throw new BusinessException(ErrorCode.PRODUCT_SKU_CODE_EMPTY, "SKU不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        Product product = new Product(name, sku, originalPrice, promotionalPrice, description, now);
        product.productId = new ProductId(UUID.randomUUID().toString().replace("-", ""));
        return product;
    }

    /**
     * 从持久化层重建聚合根
     */
    public static Product reconstruct(Long id, ProductId productId, ProductName name, SKU sku,
                                       BigDecimal originalPrice, BigDecimal promotionalPrice,
                                       ProductStatus status, String description,
                                       LocalDateTime createTime, LocalDateTime updateTime) {
        Product product = new Product(name, sku, originalPrice, promotionalPrice, description, createTime);
        product.id = id;
        product.productId = productId;
        product.status = status;
        product.updateTime = updateTime;
        return product;
    }

    /**
     * 验证价格（必须大于0）
     */
    private Money validatePrice(BigDecimal price) {
        if (price == null) {
            throw new BusinessException(ErrorCode.PRODUCT_PRICE_NEGATIVE, "价格不能为空");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PRODUCT_PRICE_NEGATIVE);
        }
        if (price.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException(ErrorCode.PRODUCT_PRICE_ZERO);
        }
        return Money.cny(price);
    }

    /**
     * 上架商品
     */
    public void activate() {
        this.status = this.status.toActive();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 下架商品
     */
    public void deactivate() {
        this.status = this.status.toInactive();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 更新价格
     */
    public void updatePrice(BigDecimal newOriginalPrice, BigDecimal newPromotionalPrice) {
        this.originalPrice = validatePrice(newOriginalPrice);
        this.promotionalPrice = newPromotionalPrice != null ? validatePrice(newPromotionalPrice) : null;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 获取有效售价（促销价优先，无促销价返回原价）
     */
    public Money getEffectivePrice() {
        return promotionalPrice != null ? promotionalPrice : originalPrice;
    }

    /**
     * 设置数据库自增 ID（持久化后回填）
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 便捷获取商品ID字符串值
     */
    public String getProductIdValue() {
        return productId.getValue();
    }
}