package com.claudej.application.product.service;

import com.claudej.application.common.assembler.PageAssembler;
import com.claudej.application.common.dto.PageDTO;
import com.claudej.application.product.assembler.ProductAssembler;
import com.claudej.application.product.command.CreateProductCommand;
import com.claudej.application.product.command.UpdatePriceCommand;
import com.claudej.application.product.dto.ProductDTO;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.common.model.valobj.Page;
import com.claudej.domain.common.model.valobj.PageRequest;
import com.claudej.domain.product.model.aggregate.Product;
import com.claudej.domain.product.model.valobj.ProductId;
import com.claudej.domain.product.model.valobj.ProductName;
import com.claudej.domain.product.model.valobj.ProductStatus;
import com.claudej.domain.product.model.valobj.SKU;
import com.claudej.domain.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品应用服务
 */
@Service
public class ProductApplicationService {

    private final ProductRepository productRepository;
    private final ProductAssembler productAssembler;
    private final PageAssembler pageAssembler;

    public ProductApplicationService(ProductRepository productRepository,
                                     ProductAssembler productAssembler,
                                     PageAssembler pageAssembler) {
        this.productRepository = productRepository;
        this.productAssembler = productAssembler;
        this.pageAssembler = pageAssembler;
    }

    /**
     * 创建商品
     */
    @Transactional
    public ProductDTO createProduct(CreateProductCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NAME_EMPTY, "命令不能为空");
        }

        ProductName name = new ProductName(command.getName());
        SKU sku = new SKU(command.getSkuCode(), command.getStock());

        Product product = Product.create(
                name,
                sku,
                command.getOriginalPrice(),
                command.getPromotionalPrice(),
                command.getDescription()
        );

        product = productRepository.save(product);
        return productAssembler.toDTO(product);
    }

    /**
     * 根据商品ID查询
     */
    public ProductDTO getProduct(String productId) {
        Product product = productRepository.findByProductId(new ProductId(productId))
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return productAssembler.toDTO(product);
    }

    /**
     * 调价
     */
    @Transactional
    public ProductDTO updatePrice(String productId, UpdatePriceCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.PRODUCT_PRICE_NEGATIVE, "调价命令不能为空");
        }

        Product product = productRepository.findByProductId(new ProductId(productId))
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.updatePrice(command.getOriginalPrice(), command.getPromotionalPrice());
        product = productRepository.save(product);
        return productAssembler.toDTO(product);
    }

    /**
     * 上架商品
     */
    @Transactional
    public ProductDTO activateProduct(String productId) {
        Product product = productRepository.findByProductId(new ProductId(productId))
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.activate();
        product = productRepository.save(product);
        return productAssembler.toDTO(product);
    }

    /**
     * 下架商品
     */
    @Transactional
    public ProductDTO deactivateProduct(String productId) {
        Product product = productRepository.findByProductId(new ProductId(productId))
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.deactivate();
        product = productRepository.save(product);
        return productAssembler.toDTO(product);
    }

    /**
     * 根据状态查询商品列表
     */
    public List<ProductDTO> listProducts(ProductStatus status) {
        List<Product> products = productRepository.findByStatus(status);
        return productAssembler.toDTOList(products);
    }

    /**
     * 分页查询商品
     */
    public PageDTO<ProductDTO> listProducts(PageRequest pageRequest) {
        Page<Product> page = productRepository.findAll(pageRequest);
        return pageAssembler.toPageDTO(page, productAssembler::toDTO);
    }
}