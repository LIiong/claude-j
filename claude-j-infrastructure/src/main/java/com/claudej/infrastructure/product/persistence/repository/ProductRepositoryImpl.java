package com.claudej.infrastructure.product.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.claudej.domain.common.model.valobj.PageRequest;
import com.claudej.domain.product.model.aggregate.Product;
import com.claudej.domain.product.model.valobj.ProductId;
import com.claudej.domain.product.model.valobj.ProductStatus;
import com.claudej.domain.product.repository.ProductRepository;
import com.claudej.infrastructure.common.persistence.PageHelper;
import com.claudej.infrastructure.product.persistence.converter.ProductConverter;
import com.claudej.infrastructure.product.persistence.dataobject.ProductDO;
import com.claudej.infrastructure.product.persistence.mapper.ProductMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 商品 Repository 实现
 */
@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductMapper productMapper;
    private final ProductConverter productConverter;

    public ProductRepositoryImpl(ProductMapper productMapper, ProductConverter productConverter) {
        this.productMapper = productMapper;
        this.productConverter = productConverter;
    }

    @Override
    @Transactional
    public Product save(Product product) {
        ProductDO productDO = productConverter.toDO(product);

        if (product.getId() == null) {
            // 新增
            productMapper.insert(productDO);
            product.setId(productDO.getId());
        } else {
            // 更新
            productMapper.updateById(productDO);
        }

        return product;
    }

    @Override
    public Optional<Product> findById(Long id) {
        ProductDO productDO = productMapper.selectById(id);
        if (productDO == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(productConverter.toDomain(productDO));
    }

    @Override
    public Optional<Product> findByProductId(ProductId productId) {
        LambdaQueryWrapper<ProductDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductDO::getProductId, productId.getValue());
        ProductDO productDO = productMapper.selectOne(wrapper);

        if (productDO == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(productConverter.toDomain(productDO));
    }

    @Override
    public List<Product> findByStatus(ProductStatus status) {
        LambdaQueryWrapper<ProductDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductDO::getStatus, status.name());
        List<ProductDO> productDOList = productMapper.selectList(wrapper);

        return productDOList.stream()
                .map(productConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public com.claudej.domain.common.model.valobj.Page<Product> findAll(PageRequest pageRequest) {
        Page<ProductDO> mybatisPage = PageHelper.createMybatisPlusPage(pageRequest);
        LambdaQueryWrapper<ProductDO> wrapper = new LambdaQueryWrapper<>();
        // 默认按创建时间降序
        wrapper.orderByDesc(ProductDO::getCreateTime);
        IPage<ProductDO> iPage = productMapper.selectPage(mybatisPage, wrapper);
        return PageHelper.toDomainPage(iPage, productConverter::toDomain);
    }
}