package com.claudej.adapter.link.web;

import com.claudej.adapter.common.ApiResult;
import com.claudej.adapter.common.response.PageResponse;
import com.claudej.adapter.link.web.request.CreateLinkRequest;
import com.claudej.adapter.link.web.request.UpdateLinkRequest;
import com.claudej.adapter.link.web.response.LinkResponse;
import com.claudej.application.common.dto.PageDTO;
import com.claudej.application.link.command.CreateLinkCommand;
import com.claudej.application.link.command.DeleteLinkCommand;
import com.claudej.application.link.command.UpdateLinkCommand;
import com.claudej.application.link.dto.LinkDTO;
import com.claudej.application.link.service.LinkApplicationService;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.common.model.valobj.PageRequest;
import com.claudej.domain.common.model.valobj.SortDirection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 链接管理 Controller
 *
 * 权限说明：
 * - 所有操作需要 USER 角色
 */
@Tag(name = "链接管理", description = "链接的创建、更新、删除、查询")
@RestController
@RequestMapping("/api/v1/links")
@PreAuthorize("hasRole('USER')")
public class LinkController {

    /**
     * Link聚合允许的排序字段白名单（防止 SQL注入）
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = new HashSet<String>(
            Arrays.asList("createTime", "updateTime", "name")
    );

    private final LinkApplicationService linkApplicationService;

    public LinkController(LinkApplicationService linkApplicationService) {
        this.linkApplicationService = linkApplicationService;
    }

    /**
     * 创建链接
     */
    @Operation(summary = "创建链接", description = "创建一个新的链接记录")
    @PostMapping
    public ApiResult<LinkResponse> createLink(@Valid @RequestBody CreateLinkRequest request) {
        CreateLinkCommand command = new CreateLinkCommand();
        command.setName(request.getName());
        command.setUrl(request.getUrl());
        command.setDescription(request.getDescription());
        command.setCategory(request.getCategory());

        LinkDTO dto = linkApplicationService.createLink(command);
        LinkResponse response = convertToResponse(dto);

        return ApiResult.ok(response);
    }

    /**
     * 更新链接
     */
    @Operation(summary = "更新链接", description = "根据ID更新链接信息")
    @PutMapping("/{id}")
    public ApiResult<LinkResponse> updateLink(@PathVariable Long id,
                                               @Valid @RequestBody UpdateLinkRequest request) {
        UpdateLinkCommand command = new UpdateLinkCommand();
        command.setId(id);
        command.setName(request.getName());
        command.setUrl(request.getUrl());
        command.setDescription(request.getDescription());
        command.setCategory(request.getCategory());

        LinkDTO dto = linkApplicationService.updateLink(command);
        LinkResponse response = convertToResponse(dto);

        return ApiResult.ok(response);
    }

    /**
     * 删除链接
     */
    @Operation(summary = "删除链接", description = "根据ID删除链接")
    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteLink(@PathVariable Long id) {
        DeleteLinkCommand command = new DeleteLinkCommand();
        command.setId(id);

        linkApplicationService.deleteLink(command);

        return ApiResult.ok(null);
    }

    /**
     * 根据ID查询链接
     */
    @Operation(summary = "根据ID查询链接", description = "根据链接ID获取链接详情")
    @GetMapping("/{id}")
    public ApiResult<LinkResponse> getLinkById(@PathVariable Long id) {
        LinkDTO dto = linkApplicationService.getLinkById(id);
        LinkResponse response = convertToResponse(dto);

        return ApiResult.ok(response);
    }

    /**
     * 查询所有链接
     */
    @Operation(summary = "查询所有链接", description = "获取所有链接列表")
    @GetMapping
    public ApiResult<List<LinkResponse>> getAllLinks() {
        List<LinkDTO> dtoList = linkApplicationService.getAllLinks();
        List<LinkResponse> responseList = dtoList.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ApiResult.ok(responseList);
    }

    /**
     * 分页查询所有链接
     * 路径改为 /query/paged 以避免与/{id} 路径冲突
     */
    @Operation(summary = "分页查询所有链接", description = "分页获取链接列表，支持排序")
    @GetMapping("/query/paged")
    public ApiResult<PageResponse<LinkResponse>> getAllLinksPaged(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortDirection) {
        // 排序字段白名单校验
        validateSortField(sortField);
        PageRequest pageRequest = PageRequest.of(page, size, sortField, SortDirection.fromString(sortDirection));
        PageDTO<LinkDTO> pageDTO = linkApplicationService.getAllLinks(pageRequest);
        PageResponse<LinkResponse> response = convertToPageResponse(pageDTO);
        return ApiResult.ok(response);
    }

    /**
     * 根据分类查询链接
     */
    @Operation(summary = "根据分类查询链接", description = "获取指定分类下的所有链接")
    @GetMapping("/category")
    public ApiResult<List<LinkResponse>> getLinksByCategory(@RequestParam String category) {
        List<LinkDTO> dtoList = linkApplicationService.getLinksByCategory(category);
        List<LinkResponse> responseList = dtoList.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ApiResult.ok(responseList);
    }

    /**
     * 分页查询指定分类的链接
     */
    @Operation(summary = "分页查询分类链接", description = "分页获取指定分类下的链接列表")
    @GetMapping("/category/paged")
    public ApiResult<PageResponse<LinkResponse>> getLinksByCategoryPaged(
            @RequestParam String category,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortDirection) {
        // 排序字段白名单校验
        validateSortField(sortField);
        PageRequest pageRequest = PageRequest.of(page, size, sortField, SortDirection.fromString(sortDirection));
        PageDTO<LinkDTO> pageDTO = linkApplicationService.getLinksByCategory(category, pageRequest);
        PageResponse<LinkResponse> response = convertToPageResponse(pageDTO);
        return ApiResult.ok(response);
    }

    /**
     * 校验排序字段是否在白名单中
     */
    private void validateSortField(String sortField) {
        if (sortField != null && !sortField.trim().isEmpty()) {
            if (!ALLOWED_SORT_FIELDS.contains(sortField.trim())) {
                throw new BusinessException(ErrorCode.INVALID_SORT_FIELD);
            }
        }
    }

    private LinkResponse convertToResponse(LinkDTO dto) {
        LinkResponse response = new LinkResponse();
        response.setId(dto.getId());
        response.setName(dto.getName());
        response.setUrl(dto.getUrl());
        response.setDescription(dto.getDescription());
        response.setCategory(dto.getCategory());
        response.setCreateTime(dto.getCreateTime());
        response.setUpdateTime(dto.getUpdateTime());
        return response;
    }

    private PageResponse<LinkResponse> convertToPageResponse(PageDTO<LinkDTO> pageDTO) {
        PageResponse<LinkResponse> response = new PageResponse<LinkResponse>();
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
