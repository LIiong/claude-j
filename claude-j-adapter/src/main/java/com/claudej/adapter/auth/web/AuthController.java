package com.claudej.adapter.auth.web;

import com.claudej.adapter.auth.web.request.ChangePasswordRequest;
import com.claudej.adapter.auth.web.request.LoginRequest;
import com.claudej.adapter.auth.web.request.LogoutRequest;
import com.claudej.adapter.auth.web.request.RefreshTokenRequest;
import com.claudej.adapter.auth.web.request.RegisterRequest;
import com.claudej.adapter.auth.web.request.ResetPasswordRequest;
import com.claudej.adapter.auth.web.request.SmsLoginRequest;
import com.claudej.adapter.auth.web.response.AuthUserResponse;
import com.claudej.adapter.auth.web.response.TokenResponse;
import com.claudej.adapter.common.ApiResult;
import com.claudej.application.auth.command.ChangePasswordCommand;
import com.claudej.application.auth.command.LoginCommand;
import com.claudej.application.auth.command.LogoutCommand;
import com.claudej.application.auth.command.RefreshTokenCommand;
import com.claudej.application.auth.command.RegisterCommand;
import com.claudej.application.auth.command.ResetPasswordCommand;
import com.claudej.application.auth.command.SmsLoginCommand;
import com.claudej.application.auth.dto.AuthUserDTO;
import com.claudej.application.auth.dto.TokenResponseDTO;
import com.claudej.application.auth.service.AuthApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 认证 Controller
 *
 * 权限说明：
 * - 所有端点公开（permitAll），无需认证即可访问
 */
@Tag(name = "认证服务", description = "用户注册、登录、Token 刷新等认证相关接口")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "新用户注册账号")
    @PostMapping("/register")
    public ApiResult<TokenResponse> register(@Valid @RequestBody RegisterRequest request,
                                              HttpServletRequest httpRequest) {
        RegisterCommand command = new RegisterCommand();
        command.setUsername(request.getUsername());
        command.setPassword(request.getPassword());
        command.setEmail(request.getEmail());
        command.setPhone(request.getPhone());
        command.setVerificationCode(request.getVerificationCode());
        command.setInviteCode(request.getInviteCode());
        command.setIpAddress(getClientIp(httpRequest));
        command.setUserAgent(getUserAgent(httpRequest));

        TokenResponseDTO dto = authApplicationService.register(command);
        return ApiResult.ok(convertToTokenResponse(dto));
    }

    /**
     * 用户登录（密码）
     */
    @Operation(summary = "用户登录", description = "使用账号密码登录")
    @PostMapping("/login")
    public ApiResult<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                           HttpServletRequest httpRequest) {
        LoginCommand command = new LoginCommand();
        command.setAccount(request.getAccount());
        command.setPassword(request.getPassword());
        command.setRememberMe(request.isRememberMe());
        command.setIpAddress(getClientIp(httpRequest));
        command.setUserAgent(getUserAgent(httpRequest));

        TokenResponseDTO dto = authApplicationService.login(command);
        return ApiResult.ok(convertToTokenResponse(dto));
    }

    /**
     * 用户登录（短信验证码）
     */
    @Operation(summary = "短信验证码登录", description = "使用手机号和短信验证码登录")
    @PostMapping("/login/sms")
    public ApiResult<TokenResponse> loginBySms(@Valid @RequestBody SmsLoginRequest request,
                                                HttpServletRequest httpRequest) {
        SmsLoginCommand command = new SmsLoginCommand();
        command.setPhone(request.getPhone());
        command.setVerificationCode(request.getVerificationCode());
        command.setRememberMe(request.isRememberMe());
        command.setIpAddress(getClientIp(httpRequest));
        command.setUserAgent(getUserAgent(httpRequest));

        TokenResponseDTO dto = authApplicationService.loginBySms(command);
        return ApiResult.ok(convertToTokenResponse(dto));
    }

    /**
     * 用户登出
     */
    @Operation(summary = "用户登出", description = "退出当前登录会话")
    @PostMapping("/logout")
    public ApiResult<String> logout(@Valid @RequestBody LogoutRequest request) {
        LogoutCommand command = new LogoutCommand();
        command.setUserId(request.getUserId());
        command.setSessionId(request.getSessionId());

        authApplicationService.logout(command);
        return ApiResult.ok("Logout successful");
    }

    /**
     * 刷新Token
     */
    @Operation(summary = "刷新Token", description = "使用refreshToken获取新的accessToken")
    @PostMapping("/refresh")
    public ApiResult<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenCommand command = new RefreshTokenCommand();
        command.setRefreshToken(request.getRefreshToken());

        TokenResponseDTO dto = authApplicationService.refreshToken(command);
        return ApiResult.ok(convertToTokenResponse(dto));
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码", description = "修改用户密码")
    @PostMapping("/password/change")
    public ApiResult<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        ChangePasswordCommand command = new ChangePasswordCommand();
        command.setUserId(request.getUserId());
        command.setOldPassword(request.getOldPassword());
        command.setNewPassword(request.getNewPassword());

        authApplicationService.changePassword(command);
        return ApiResult.ok("Password changed successfully");
    }

    /**
     * 重置密码
     */
    @Operation(summary = "重置密码", description = "通过邮箱验证码重置密码")
    @PostMapping("/password/reset")
    public ApiResult<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ResetPasswordCommand command = new ResetPasswordCommand();
        command.setEmail(request.getEmail());
        command.setVerificationCode(request.getVerificationCode());
        command.setNewPassword(request.getNewPassword());

        authApplicationService.resetPassword(command);
        return ApiResult.ok("Password reset successfully");
    }

    /**
     * 获取认证用户信息
     */
    @Operation(summary = "获取认证用户信息", description = "根据用户ID获取认证相关信息")
    @GetMapping("/users/{userId}")
    public ApiResult<AuthUserResponse> getAuthUser(@PathVariable String userId) {
        AuthUserDTO dto = authApplicationService.getAuthUser(userId);
        return ApiResult.ok(convertToAuthUserResponse(dto));
    }

    private TokenResponse convertToTokenResponse(TokenResponseDTO dto) {
        TokenResponse response = new TokenResponse();
        response.setAccessToken(dto.getAccessToken());
        response.setRefreshToken(dto.getRefreshToken());
        response.setExpiresIn(dto.getExpiresIn());
        response.setUserId(dto.getUserId());
        response.setUsername(dto.getUsername());
        response.setEmail(dto.getEmail());
        response.setPhone(dto.getPhone());
        return response;
    }

    private AuthUserResponse convertToAuthUserResponse(AuthUserDTO dto) {
        AuthUserResponse response = new AuthUserResponse();
        response.setUserId(dto.getUserId());
        response.setUsername(dto.getUsername());
        response.setEmail(dto.getEmail());
        response.setPhone(dto.getPhone());
        response.setEmailVerified(dto.isEmailVerified());
        response.setPhoneVerified(dto.isPhoneVerified());
        response.setStatus(dto.getStatus());
        response.setFailedLoginAttempts(dto.getFailedLoginAttempts());
        return response;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 取第一个IP（多层代理情况）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
