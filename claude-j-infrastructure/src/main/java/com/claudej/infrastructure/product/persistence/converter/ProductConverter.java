package com.claudej.infrastructure.product.persistence.converter;

import com.claudej.domain.product.model.aggregate.Product;
import com.claudej.domain.product.model.valobj.ProductId;
import com.claudej.domain.product.model.valobj.ProductName;
import com.claudej.domain.product.model.valobj.ProductStatus;
import com.claudej.domain.product.model.valobj.SKU;
import com.claudej.infrastructure.product.persistence.dataobject.ProductDO;
import org.springframework.stereotype.Component;

/**
 * Product 转换器 - DO <-> Domain
 */
@Component
public class ProductConverter {

    /**
     * DO 转 Domain
     */
    public Product toDomain(ProductDO productDO) {
        if (productDO == null) {
            return null;
        }

        return Product.reconstruct(
                productDO.getId(),
                new ProductId(productDO.getProductId()),
                new ProductName(productDO.getName()),
                new SKU(productDO.getSkuCode(), productDO.getStock()),
                productDO.getOriginalPrice(),
                productDO.getPromotionalPrice(),
                ProductStatus.valueOf(productDO.getStatus()),
                productDO.getDescription(),
                productDO.getCreateTime(),
                productDO.getUpdateTime()
        );
    }

    /**
     * Domain 转 DO
     */
    public ProductDO toDO(Product product) {
        if (product == null) {
            return null;
        }
        ProductDO productDO = new ProductDO();
        productDO.setId(product.getId());
        productDO.setProductId(product.getProductIdValue());
        productDO.setName(product.getName().getValue());
        productDO.setDescription(product.getDescription());
        productDO.setSkuCode(product.getSku().getSkuCode());
        productDO.setStock(product.getSku().getStock());
        productDO.setOriginalPrice(product.getOriginalPrice().getAmount());
        if (product.getPromotionalPrice() != null) {
            productDO.setPromotionalPrice(product.getPromotionalPrice().getAmount());
        }
        productDO.setCurrency(product.getOriginalPrice().getCurrency());
        productDO.setStatus(product.getStatus().name());
        productDO.setCreateTime(product.getCreateTime());
        productDO.setUpdateTime(product.getUpdateTime());
        productDO.setDeleted(0);
        return productDO;
    }
}