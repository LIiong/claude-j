package com.claudej.adapter.user.web;

import com.claudej.adapter.common.ApiResult;
import com.claudej.adapter.common.response.PageResponse;
import com.claudej.adapter.user.web.request.CreateUserRequest;
import com.claudej.adapter.user.web.request.ValidateInviteCodeRequest;
import com.claudej.adapter.user.web.response.UserResponse;
import com.claudej.application.common.dto.PageDTO;
import com.claudej.application.user.command.CreateUserCommand;
import com.claudej.application.user.dto.UserDTO;
import com.claudej.application.user.service.UserApplicationService;
import com.claudej.domain.common.model.valobj.PageRequest;
import com.claudej.domain.common.model.valobj.SortDirection;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户 Controller
 */
@Tag(name = "用户服务", description = "用户注册、查询、管理")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserApplicationService userApplicationService;

    public UserController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    /**
     * 创建用户
     */
    @Operation(summary = "创建用户", description = "创建新用户账号")
    @PostMapping
    public ApiResult<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        CreateUserCommand command = new CreateUserCommand();
        command.setUsername(request.getUsername());
        command.setEmail(request.getEmail());
        command.setPhone(request.getPhone());
        command.setInviteCode(request.getInviteCode());

        UserDTO dto = userApplicationService.createUser(command);
        UserResponse response = convertToResponse(dto);

        return ApiResult.ok(response);
    }

    /**
     * 根据用户ID查询用户
     */
    @Operation(summary = "根据ID查询用户", description = "根据用户ID获取用户信息")
    @GetMapping("/{userId}")
    public ApiResult<UserResponse> getUserById(@PathVariable String userId) {
        UserDTO dto = userApplicationService.getUserById(userId);
        UserResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 根据用户名查询用户
     */
    @Operation(summary = "根据用户名查询", description = "根据用户名获取用户信息")
    @GetMapping("/by-username/{username}")
    public ApiResult<UserResponse> getUserByUsername(@PathVariable String username) {
        UserDTO dto = userApplicationService.getUserByUsername(username);
        UserResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 冻结用户
     */
    @Operation(summary = "冻结用户", description = "冻结指定用户账号")
    @PostMapping("/{userId}/freeze")
    public ApiResult<UserResponse> freezeUser(@PathVariable String userId) {
        UserDTO dto = userApplicationService.freezeUser(userId);
        UserResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 解冻用户
     */
    @Operation(summary = "解冻用户", description = "解冻被冻结的用户账号")
    @PostMapping("/{userId}/unfreeze")
    public ApiResult<UserResponse> unfreezeUser(@PathVariable String userId) {
        UserDTO dto = userApplicationService.unfreezeUser(userId);
        UserResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 查询被邀请的用户列表
     */
    @Operation(summary = "查询被邀请用户", description = "查询用户邀请的所有用户")
    @GetMapping("/{userId}/invited-users")
    public ApiResult<List<UserResponse>> getInvitedUsers(@PathVariable String userId) {
        List<UserDTO> dtoList = userApplicationService.getInvitedUsers(userId);
        List<UserResponse> responseList = dtoList.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ApiResult.ok(responseList);
    }

    /**
     * 分页查询被邀请的用户列表
     */
    @Operation(summary = "分页查询被邀请用户", description = "分页查询用户邀请的用户列表")
    @GetMapping("/{userId}/invited-users/paged")
    public ApiResult<PageResponse<UserResponse>> getInvitedUsersPaged(
            @PathVariable String userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortDirection) {
        PageRequest pageRequest = PageRequest.of(page, size, sortField, SortDirection.fromString(sortDirection));
        PageDTO<UserDTO> pageDTO = userApplicationService.getInvitedUsers(userId, pageRequest);
        PageResponse<UserResponse> response = convertToPageResponse(pageDTO);
        return ApiResult.ok(response);
    }

    /**
     * 验证邀请码
     */
    @Operation(summary = "验证邀请码", description = "验证邀请码是否有效")
    @PostMapping("/validate-invite-code")
    public ApiResult<UserResponse> validateInviteCode(@Valid @RequestBody ValidateInviteCodeRequest request) {
        UserDTO dto = userApplicationService.getUserByInviteCode(request.getInviteCode());
        UserResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    private UserResponse convertToResponse(UserDTO dto) {
        UserResponse response = new UserResponse();
        response.setUserId(dto.getUserId());
        response.setUsername(dto.getUsername());
        response.setEmail(dto.getEmail());
        response.setPhone(dto.getPhone());
        response.setStatus(dto.getStatus());
        response.setInviteCode(dto.getInviteCode());
        response.setInviterId(dto.getInviterId());
        response.setCreateTime(dto.getCreateTime());
        response.setUpdateTime(dto.getUpdateTime());
        return response;
    }

    private PageResponse<UserResponse> convertToPageResponse(PageDTO<UserDTO> pageDTO) {
        PageResponse<UserResponse> response = new PageResponse<UserResponse>();
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
