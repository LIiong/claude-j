package com.claudej.application.product.assembler;

import com.claudej.application.product.dto.ProductDTO;
import com.claudej.domain.product.model.aggregate.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Product 组装器 - Domain 转 DTO
 */
@Mapper(componentModel = "spring")
public interface ProductAssembler {

    /**
     * Domain 转 DTO
     */
    @Mapping(target = "productId", expression = "java(product.getProductIdValue())")
    @Mapping(target = "name", expression = "java(product.getName().getValue())")
    @Mapping(target = "skuCode", expression = "java(product.getSku().getSkuCode())")
    @Mapping(target = "stock", expression = "java(product.getSku().getStock())")
    @Mapping(target = "originalPrice", expression = "java(product.getOriginalPrice().getAmount())")
    @Mapping(target = "promotionalPrice", expression = "java(product.getPromotionalPrice() != null ? product.getPromotionalPrice().getAmount() : null)")
    @Mapping(target = "effectivePrice", expression = "java(product.getEffectivePrice().getAmount())")
    @Mapping(target = "currency", expression = "java(product.getOriginalPrice().getCurrency())")
    @Mapping(target = "status", expression = "java(product.getStatus().name())")
    ProductDTO toDTO(Product product);

    /**
     * Domain 列表转 DTO 列表
     */
    List<ProductDTO> toDTOList(List<Product> products);
}