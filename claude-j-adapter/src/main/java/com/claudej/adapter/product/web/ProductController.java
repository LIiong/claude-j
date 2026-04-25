package com.claudej.adapter.product.web;

import com.claudej.adapter.common.ApiResult;
import com.claudej.adapter.common.response.PageResponse;
import com.claudej.adapter.product.web.request.CreateProductRequest;
import com.claudej.adapter.product.web.request.UpdatePriceRequest;
import com.claudej.adapter.product.web.response.ProductResponse;
import com.claudej.application.common.dto.PageDTO;
import com.claudej.application.product.command.CreateProductCommand;
import com.claudej.application.product.command.UpdatePriceCommand;
import com.claudej.application.product.dto.ProductDTO;
import com.claudej.application.product.service.ProductApplicationService;
import com.claudej.domain.common.model.valobj.PageRequest;
import com.claudej.domain.common.model.valobj.SortDirection;
import com.claudej.domain.product.model.valobj.ProductStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品 Controller
 */
@Tag(name = "商品服务", description = "商品创建、查询、调价、上架、下架")
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductApplicationService productApplicationService;

    public ProductController(ProductApplicationService productApplicationService) {
        this.productApplicationService = productApplicationService;
    }

    /**
     * 创建商品
     */
    @Operation(summary = "创建商品", description = "创建一个新商品")
    @PostMapping
    public ApiResult<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        CreateProductCommand command = new CreateProductCommand();
        command.setName(request.getName());
        command.setSkuCode(request.getSkuCode());
        command.setStock(request.getStock());
        command.setOriginalPrice(request.getOriginalPrice());
        command.setPromotionalPrice(request.getPromotionalPrice());
        command.setDescription(request.getDescription());

        ProductDTO dto = productApplicationService.createProduct(command);
        ProductResponse response = convertToResponse(dto);

        return ApiResult.ok(response);
    }

    /**
     * 根据商品ID查询
     */
    @Operation(summary = "根据ID查询商品", description = "根据商品ID获取商品详情")
    @GetMapping("/{productId}")
    public ApiResult<ProductResponse> getProductById(@PathVariable String productId) {
        ProductDTO dto = productApplicationService.getProduct(productId);
        ProductResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 调价
     */
    @Operation(summary = "调整商品价格", description = "更新商品原价和促销价")
    @PutMapping("/{productId}/price")
    public ApiResult<ProductResponse> updatePrice(@PathVariable String productId,
                                                   @Valid @RequestBody UpdatePriceRequest request) {
        UpdatePriceCommand command = new UpdatePriceCommand();
        command.setOriginalPrice(request.getOriginalPrice());
        command.setPromotionalPrice(request.getPromotionalPrice());

        ProductDTO dto = productApplicationService.updatePrice(productId, command);
        ProductResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 上架商品
     */
    @Operation(summary = "上架商品", description = "将商品上架销售")
    @PutMapping("/{productId}/activate")
    public ApiResult<ProductResponse> activateProduct(@PathVariable String productId) {
        ProductDTO dto = productApplicationService.activateProduct(productId);
        ProductResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 下架商品
     */
    @Operation(summary = "下架商品", description = "将商品下架停止销售")
    @PutMapping("/{productId}/deactivate")
    public ApiResult<ProductResponse> deactivateProduct(@PathVariable String productId) {
        ProductDTO dto = productApplicationService.deactivateProduct(productId);
        ProductResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 查询商品列表
     */
    @Operation(summary = "查询商品列表", description = "根据状态或分页查询商品")
    @GetMapping
    public ApiResult<Object> listProducts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortDirection) {

        // 如果指定了状态，按状态查询
        if (status != null && !status.isEmpty()) {
            ProductStatus productStatus = ProductStatus.valueOf(status);
            List<ProductDTO> dtoList = productApplicationService.listProducts(productStatus);
            List<ProductResponse> responseList = dtoList.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            return ApiResult.ok(responseList);
        }

        // 否则分页查询
        PageRequest pageRequest = PageRequest.of(page, size, sortField, SortDirection.fromString(sortDirection));
        PageDTO<ProductDTO> pageDTO = productApplicationService.listProducts(pageRequest);
        PageResponse<ProductResponse> response = convertToPageResponse(pageDTO);
        return ApiResult.ok(response);
    }

    private ProductResponse convertToResponse(ProductDTO dto) {
        ProductResponse response = new ProductResponse();
        response.setProductId(dto.getProductId());
        response.setName(dto.getName());
        response.setDescription(dto.getDescription());
        response.setSkuCode(dto.getSkuCode());
        response.setStock(dto.getStock());
        response.setOriginalPrice(dto.getOriginalPrice());
        response.setPromotionalPrice(dto.getPromotionalPrice());
        response.setEffectivePrice(dto.getEffectivePrice());
        response.setCurrency(dto.getCurrency());
        response.setStatus(dto.getStatus());
        response.setCreateTime(dto.getCreateTime());
        response.setUpdateTime(dto.getUpdateTime());
        return response;
    }

    private PageResponse<ProductResponse> convertToPageResponse(PageDTO<ProductDTO> pageDTO) {
        PageResponse<ProductResponse> response = new PageResponse<ProductResponse>();
        response.setContent(pageDTO.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList()));
        response.setTotalElements(pageDTO.getTotalElements());
        response.setTotalPages(pageDTO.getTotalPages());
        response.setPage(pageDTO.getPage());
        response.setSize(pageDTO.getSize());
        response.setFirst(pageDTO.isFirst());
        response.setLast(pageDTO.isLast());
        response.setEmpty(pageDTO.isEmpty());
        return response;
    }
}