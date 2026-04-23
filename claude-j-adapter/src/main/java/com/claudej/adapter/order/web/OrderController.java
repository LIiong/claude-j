package com.claudej.adapter.order.web;

import com.claudej.adapter.common.ApiResult;
import com.claudej.adapter.common.response.PageResponse;
import com.claudej.adapter.order.web.request.CreateOrderFromCartRequest;
import com.claudej.adapter.order.web.request.CreateOrderRequest;
import com.claudej.adapter.order.web.response.OrderItemResponse;
import com.claudej.adapter.order.web.response.OrderResponse;
import com.claudej.application.common.dto.PageDTO;
import com.claudej.application.order.command.CreateOrderCommand;
import com.claudej.application.order.command.CreateOrderFromCartCommand;
import com.claudej.application.order.dto.OrderDTO;
import com.claudej.application.order.service.OrderApplicationService;
import com.claudej.domain.common.model.valobj.PageRequest;
import com.claudej.domain.common.model.valobj.SortDirection;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单 Controller
 */
@Tag(name = "订单服务", description = "订单创建、查询、支付、取消")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderApplicationService orderApplicationService;

    public OrderController(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    /**
     * 创建订单
     */
    @PostMapping
    public ApiResult<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        CreateOrderCommand command = new CreateOrderCommand();
        command.setCustomerId(request.getCustomerId());

        List<CreateOrderCommand.OrderItemCommand> itemCommands = request.getItems().stream()
                .map(item -> {
                    CreateOrderCommand.OrderItemCommand itemCmd = new CreateOrderCommand.OrderItemCommand();
                    itemCmd.setProductId(item.getProductId());
                    itemCmd.setProductName(item.getProductName());
                    itemCmd.setQuantity(item.getQuantity());
                    itemCmd.setUnitPrice(item.getUnitPrice());
                    return itemCmd;
                })
                .collect(Collectors.toList());
        command.setItems(itemCommands);

        OrderDTO dto = orderApplicationService.createOrder(command);
        OrderResponse response = convertToResponse(dto);

        return ApiResult.ok(response);
    }

    /**
     * 根据订单号查询订单
     */
    @GetMapping("/{orderId}")
    public ApiResult<OrderResponse> getOrderById(@PathVariable String orderId) {
        OrderDTO dto = orderApplicationService.getOrderById(orderId);
        OrderResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 根据客户ID查询订单列表
     */
    @GetMapping
    public ApiResult<List<OrderResponse>> getOrdersByCustomerId(@RequestParam String customerId) {
        List<OrderDTO> dtoList = orderApplicationService.getOrdersByCustomerId(customerId);
        List<OrderResponse> responseList = dtoList.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ApiResult.ok(responseList);
    }

    /**
     * 分页查询客户订单列表
     */
    @GetMapping("/paged")
    public ApiResult<PageResponse<OrderResponse>> getOrdersByCustomerIdPaged(
            @RequestParam String customerId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortDirection) {
        PageRequest pageRequest = PageRequest.of(page, size, sortField, SortDirection.fromString(sortDirection));
        PageDTO<OrderDTO> pageDTO = orderApplicationService.getOrdersByCustomerId(customerId, pageRequest);
        PageResponse<OrderResponse> response = convertToPageResponse(pageDTO);
        return ApiResult.ok(response);
    }

    /**
     * 支付订单
     */
    @PostMapping("/{orderId}/pay")
    public ApiResult<OrderResponse> payOrder(@PathVariable String orderId) {
        OrderDTO dto = orderApplicationService.payOrder(orderId);
        OrderResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 取消订单
     */
    @PostMapping("/{orderId}/cancel")
    public ApiResult<OrderResponse> cancelOrder(@PathVariable String orderId) {
        OrderDTO dto = orderApplicationService.cancelOrder(orderId);
        OrderResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 从购物车创建订单
     */
    @PostMapping("/from-cart")
    public ApiResult<OrderResponse> createOrderFromCart(@Valid @RequestBody CreateOrderFromCartRequest request) {
        CreateOrderFromCartCommand command = new CreateOrderFromCartCommand();
        command.setCustomerId(request.getCustomerId());
        command.setCouponId(request.getCouponId());

        OrderDTO dto = orderApplicationService.createOrderFromCart(command);
        OrderResponse response = convertToResponse(dto);

        return ApiResult.ok(response);
    }

    /**
     * 发货
     */
    @PostMapping("/{orderId}/ship")
    public ApiResult<OrderResponse> shipOrder(@PathVariable String orderId) {
        OrderDTO dto = orderApplicationService.shipOrder(orderId);
        OrderResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 确认送达
     */
    @PostMapping("/{orderId}/deliver")
    public ApiResult<OrderResponse> deliverOrder(@PathVariable String orderId) {
        OrderDTO dto = orderApplicationService.deliverOrder(orderId);
        OrderResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 退款
     */
    @PostMapping("/{orderId}/refund")
    public ApiResult<OrderResponse> refundOrder(@PathVariable String orderId) {
        OrderDTO dto = orderApplicationService.refundOrder(orderId);
        OrderResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    private OrderResponse convertToResponse(OrderDTO dto) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(dto.getOrderId());
        response.setCustomerId(dto.getCustomerId());
        response.setStatus(dto.getStatus());
        response.setTotalAmount(dto.getTotalAmount());
        response.setCurrency(dto.getCurrency());

        if (dto.getItems() != null) {
            List<OrderItemResponse> itemResponses = dto.getItems().stream()
                    .map(this::convertToItemResponse)
                    .collect(Collectors.toList());
            response.setItems(itemResponses);
        }

        response.setCreateTime(dto.getCreateTime());
        response.setUpdateTime(dto.getUpdateTime());
        return response;
    }

    private OrderItemResponse convertToItemResponse(com.claudej.application.order.dto.OrderItemDTO dto) {
        OrderItemResponse response = new OrderItemResponse();
        response.setProductId(dto.getProductId());
        response.setProductName(dto.getProductName());
        response.setQuantity(dto.getQuantity());
        response.setUnitPrice(dto.getUnitPrice());
        response.setSubtotal(dto.getSubtotal());
        return response;
    }

    private PageResponse<OrderResponse> convertToPageResponse(PageDTO<OrderDTO> pageDTO) {
        PageResponse<OrderResponse> response = new PageResponse<OrderResponse>();
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
