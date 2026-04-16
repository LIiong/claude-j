package com.claudej.application.auth.service;

import com.claudej.application.auth.command.ChangePasswordCommand;
import com.claudej.application.auth.command.LoginCommand;
import com.claudej.application.auth.command.LogoutCommand;
import com.claudej.application.auth.command.RefreshTokenCommand;
import com.claudej.application.auth.command.RegisterCommand;
import com.claudej.application.auth.command.ResetPasswordCommand;
import com.claudej.application.auth.command.SmsLoginCommand;
import com.claudej.application.auth.dto.AuthUserDTO;
import com.claudej.application.auth.dto.LoginLogDTO;
import com.claudej.application.auth.dto.TokenResponseDTO;
import com.claudej.application.auth.dto.UserSessionDTO;

import java.util.List;

/**
 * 认证应用服务接口
 */
public interface AuthApplicationService {

    /**
     * 用户注册
     */
    TokenResponseDTO register(RegisterCommand command);

    /**
     * 用户登录（密码）
     */
    TokenResponseDTO login(LoginCommand command);

    /**
     * 用户登录（短信验证码）
     */
    TokenResponseDTO loginBySms(SmsLoginCommand command);

    /**
     * 用户登出
     */
    void logout(LogoutCommand command);

    /**
     * 刷新Token
     */
    TokenResponseDTO refreshToken(RefreshTokenCommand command);

    /**
     * 修改密码
     */
    void changePassword(ChangePasswordCommand command);

    /**
     * 重置密码
     */
    void resetPassword(ResetPasswordCommand command);

    /**
     * 获取认证用户信息
     */
    AuthUserDTO getAuthUser(String userId);

    /**
     * 获取用户会话列表
     */
    List<UserSessionDTO> getUserSessions(String userId);

    /**
     * 获取用户登录日志
     */
    List<LoginLogDTO> getLoginLogs(String userId);

    /**
     * 强制下线所有会话
     */
    void forceLogoutAllSessions(String userId);
}
