package com.claudej.domain.product.repository;

import com.claudej.domain.common.model.valobj.Page;
import com.claudej.domain.common.model.valobj.PageRequest;
import com.claudej.domain.product.model.aggregate.Product;
import com.claudej.domain.product.model.valobj.ProductId;
import com.claudej.domain.product.model.valobj.ProductStatus;

import java.util.List;
import java.util.Optional;

/**
 * 商品 Repository 端口接口
 */
public interface ProductRepository {

    /**
     * 保存商品
     */
    Product save(Product product);

    /**
     * 根据数据库 ID 查找商品
     */
    Optional<Product> findById(Long id);

    /**
     * 根据业务 ID 查找商品
     */
    Optional<Product> findByProductId(ProductId productId);

    /**
     * 根据状态查询商品列表
     */
    List<Product> findByStatus(ProductStatus status);

    /**
     * 分页查询商品
     */
    Page<Product> findAll(PageRequest pageRequest);
}