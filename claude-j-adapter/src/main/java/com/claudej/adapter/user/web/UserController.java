package com.claudej.adapter.user.web;

import com.claudej.adapter.common.ApiResult;
import com.claudej.adapter.user.web.request.CreateUserRequest;
import com.claudej.adapter.user.web.request.ValidateInviteCodeRequest;
import com.claudej.adapter.user.web.response.UserResponse;
import com.claudej.application.user.command.CreateUserCommand;
import com.claudej.application.user.dto.UserDTO;
import com.claudej.application.user.service.UserApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户 Controller
 */
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
    @GetMapping("/{userId}")
    public ApiResult<UserResponse> getUserById(@PathVariable String userId) {
        UserDTO dto = userApplicationService.getUserById(userId);
        UserResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 根据用户名查询用户
     */
    @GetMapping("/by-username/{username}")
    public ApiResult<UserResponse> getUserByUsername(@PathVariable String username) {
        UserDTO dto = userApplicationService.getUserByUsername(username);
        UserResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 冻结用户
     */
    @PostMapping("/{userId}/freeze")
    public ApiResult<UserResponse> freezeUser(@PathVariable String userId) {
        UserDTO dto = userApplicationService.freezeUser(userId);
        UserResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 解冻用户
     */
    @PostMapping("/{userId}/unfreeze")
    public ApiResult<UserResponse> unfreezeUser(@PathVariable String userId) {
        UserDTO dto = userApplicationService.unfreezeUser(userId);
        UserResponse response = convertToResponse(dto);
        return ApiResult.ok(response);
    }

    /**
     * 查询被邀请的用户列表
     */
    @GetMapping("/{userId}/invited-users")
    public ApiResult<List<UserResponse>> getInvitedUsers(@PathVariable String userId) {
        List<UserDTO> dtoList = userApplicationService.getInvitedUsers(userId);
        List<UserResponse> responseList = dtoList.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ApiResult.ok(responseList);
    }

    /**
     * 验证邀请码
     */
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
}
