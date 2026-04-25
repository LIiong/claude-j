package com.claudej.adapter.inventory.web;

import com.claudej.adapter.common.ApiResult;
import com.claudej.adapter.inventory.web.request.AdjustStockRequest;
import com.claudej.adapter.inventory.web.request.CreateInventoryRequest;
import com.claudej.adapter.inventory.web.response.InventoryResponse;
import com.claudej.application.inventory.command.AdjustStockCommand;
import com.claudej.application.inventory.command.CreateInventoryCommand;
import com.claudej.application.inventory.dto.InventoryDTO;
import com.claudej.application.inventory.service.InventoryApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 库存 Controller
 *
 * 权限说明：
 * - 创建库存：ADMIN
 * - 查询库存：USER
 * - 调整库存：ADMIN
 */
@Tag(name = "库存服务", description = "库存创建、查询、调整")
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryApplicationService inventoryApplicationService;

    public InventoryController(InventoryApplicationService inventoryApplicationService) {
        this.inventoryApplicationService = inventoryApplicationService;
    }

    /**
     * 创建库存（管理员操作）
     */
    @Operation(summary = "创建库存", description = "为商品创建库存记录")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResult<InventoryResponse> createInventory(@Valid @RequestBody CreateInventoryRequest request) {
        CreateInventoryCommand command = new CreateInventoryCommand();
        command.setProductId(request.getProductId());
        command.setSkuCode(request.getSkuCode());
        command.setInitialStock(request.getInitialStock() != null ? request.getInitialStock() : 0);

        InventoryDTO dto = inventoryApplicationService.createInventory(command);
        InventoryResponse response = convertToResponse(dto);

        return ApiResult.ok(response);
    }

    /**
     * 根据库存ID查询库存
     */
    @Operation(summary = "查询库存详情", description = "根据库存ID查询库存详情")
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{inventoryId}")
    public ApiResult<InventoryResponse> getInventoryById(@PathVariable String inventoryId) {
        InventoryDTO dto = inventoryApplicationService.getInventoryById(inventoryId);
        InventoryResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 根据商品ID查询库存
     */
    @Operation(summary = "按商品查询库存", description = "根据商品ID查询库存详情")
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/product/{productId}")
    public ApiResult<InventoryResponse> getInventoryByProductId(@PathVariable String productId) {
        InventoryDTO dto = inventoryApplicationService.getInventoryByProductId(productId);
        InventoryResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 根据SKU编码查询库存
     */
    @Operation(summary = "按SKU查询库存", description = "根据SKU编码查询库存详情")
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/sku/{skuCode}")
    public ApiResult<InventoryResponse> getInventoryBySkuCode(@PathVariable String skuCode) {
        InventoryDTO dto = inventoryApplicationService.getInventoryBySkuCode(skuCode);
        InventoryResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 调整库存（管理员操作）
     */
    @Operation(summary = "调整库存", description = "管理员调整库存数量")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{inventoryId}/adjust")
    public ApiResult<InventoryResponse> adjustStock(
            @PathVariable String inventoryId,
            @Valid @RequestBody AdjustStockRequest request) {
        AdjustStockCommand command = new AdjustStockCommand();
        command.setInventoryId(inventoryId);
        command.setAdjustment(request.getAdjustment());
        command.setReason(request.getReason());

        InventoryDTO dto = inventoryApplicationService.adjustStock(command);
        InventoryResponse response = convertToResponse(dto);

        return ApiResult.ok(response);
    }

    private InventoryResponse convertToResponse(InventoryDTO dto) {
        InventoryResponse response = new InventoryResponse();
        response.setInventoryId(dto.getInventoryId());
        response.setProductId(dto.getProductId());
        response.setSkuCode(dto.getSkuCode());
        response.setAvailableStock(dto.getAvailableStock());
        response.setReservedStock(dto.getReservedStock());
        response.setCreateTime(dto.getCreateTime());
        response.setUpdateTime(dto.getUpdateTime());
        return response;
    }
}