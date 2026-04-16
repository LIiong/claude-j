package com.claudej.application.auth.service;

import com.claudej.application.auth.assembler.AuthUserAssembler;
import com.claudej.application.auth.assembler.LoginLogAssembler;
import com.claudej.application.auth.assembler.UserSessionAssembler;
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
import com.claudej.domain.auth.model.aggregate.AuthUser;
import com.claudej.domain.auth.model.entity.LoginLog;
import com.claudej.domain.auth.model.entity.UserSession;
import com.claudej.domain.auth.model.valobj.AuthProvider;
import com.claudej.domain.auth.model.valobj.DeviceInfo;
import com.claudej.domain.auth.model.valobj.JwtToken;
import com.claudej.domain.auth.repository.AuthUserRepository;
import com.claudej.domain.auth.repository.LoginLogRepository;
import com.claudej.domain.auth.repository.UserSessionRepository;
import com.claudej.domain.auth.service.PasswordEncoder;
import com.claudej.domain.auth.service.TokenService;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.user.model.aggregate.User;
import com.claudej.domain.user.model.valobj.Email;
import com.claudej.domain.user.model.valobj.InviteCode;
import com.claudej.domain.user.model.valobj.Phone;
import com.claudej.domain.user.model.valobj.UserId;
import com.claudej.domain.user.model.valobj.Username;
import com.claudej.domain.user.repository.UserRepository;
import com.claudej.domain.user.service.InviteCodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 认证应用服务实现
 */
@Service
public class AuthApplicationServiceImpl implements AuthApplicationService {

    private final UserRepository userRepository;
    private final AuthUserRepository authUserRepository;
    private final UserSessionRepository userSessionRepository;
    private final LoginLogRepository loginLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final AuthUserAssembler authUserAssembler;
    private final UserSessionAssembler userSessionAssembler;
    private final LoginLogAssembler loginLogAssembler;

    public AuthApplicationServiceImpl(UserRepository userRepository,
                                      AuthUserRepository authUserRepository,
                                      UserSessionRepository userSessionRepository,
                                      LoginLogRepository loginLogRepository,
                                      PasswordEncoder passwordEncoder,
                                      TokenService tokenService,
                                      InviteCodeGenerator inviteCodeGenerator,
                                      AuthUserAssembler authUserAssembler,
                                      UserSessionAssembler userSessionAssembler,
                                      LoginLogAssembler loginLogAssembler) {
        this.userRepository = userRepository;
        this.authUserRepository = authUserRepository;
        this.userSessionRepository = userSessionRepository;
        this.loginLogRepository = loginLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.authUserAssembler = authUserAssembler;
        this.userSessionAssembler = userSessionAssembler;
        this.loginLogAssembler = loginLogAssembler;
    }

    @Override
    @Transactional
    public TokenResponseDTO register(RegisterCommand command) {
        // 1. 创建用户
        Username username = new Username(command.getUsername());
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        InviteCode inviteCode = generateUniqueInviteCode();
        User user = User.create(username, inviteCode);

        if (command.getEmail() != null && !command.getEmail().trim().isEmpty()) {
            user.setEmail(new Email(command.getEmail()));
        }
        if (command.getPhone() != null && !command.getPhone().trim().isEmpty()) {
            user.setPhone(new Phone(command.getPhone()));
        }
        if (command.getInviteCode() != null && !command.getInviteCode().trim().isEmpty()) {
            InviteCode inviterCode = new InviteCode(command.getInviteCode());
            User inviter = userRepository.findByInviteCode(inviterCode)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_CODE, "邀请码无效"));
            user.setInviter(inviter.getUserId());
        }

        user = userRepository.save(user);

        // 2. 创建认证用户
        String passwordHash = passwordEncoder.encode(command.getPassword());
        AuthUser authUser = AuthUser.create(user.getUserId(), passwordHash);
        if (command.getEmail() != null && !command.getEmail().trim().isEmpty()) {
            authUser.markEmailVerified();
        }
        if (command.getPhone() != null && !command.getPhone().trim().isEmpty()) {
            authUser.markPhoneVerified();
        }
        authUserRepository.save(authUser);

        // 3. 生成Token
        JwtToken jwtToken = tokenService.generateTokenPair(user.getUserId());

        // 4. 创建会话
        UserSession session = createSession(user.getUserId(), jwtToken.getRefreshToken(),
                command.getIpAddress(), command.getUserAgent());

        // 5. 记录登录日志
        recordLoginLog(user.getUserId(), AuthProvider.PASSWORD, command.getIpAddress(),
                command.getUserAgent(), true, null);

        // 6. 记录登录成功
        authUser.recordLoginSuccess();
        authUserRepository.update(authUser);

        return buildTokenResponse(jwtToken, user);
    }

    @Override
    @Transactional
    public TokenResponseDTO login(LoginCommand command) {
        // 1. 查找用户
        User user = findUserByAccount(command.getAccount());

        // 2. 查找认证用户
        AuthUser authUser = authUserRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

        // 3. 验证是否可以登录
        try {
            authUser.validateCanLogin();
        } catch (BusinessException e) {
            recordLoginLog(user.getUserId(), AuthProvider.PASSWORD, command.getIpAddress(),
                    command.getUserAgent(), false, e.getMessage());
            throw e;
        }

        // 4. 验证密码
        if (!passwordEncoder.matches(command.getPassword(), authUser.getPasswordHash())) {
            authUser.recordLoginFailure();
            authUserRepository.update(authUser);
            recordLoginLog(user.getUserId(), AuthProvider.PASSWORD, command.getIpAddress(),
                    command.getUserAgent(), false, "密码错误");
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 5. 生成Token
        JwtToken jwtToken = tokenService.generateTokenPair(user.getUserId());

        // 6. 创建会话
        UserSession session = createSession(user.getUserId(), jwtToken.getRefreshToken(),
                command.getIpAddress(), command.getUserAgent());

        // 7. 记录登录成功
        authUser.recordLoginSuccess();
        authUserRepository.update(authUser);

        // 8. 记录登录日志
        recordLoginLog(user.getUserId(), AuthProvider.PASSWORD, command.getIpAddress(),
                command.getUserAgent(), true, null);

        return buildTokenResponse(jwtToken, user);
    }

    @Override
    @Transactional
    public TokenResponseDTO loginBySms(SmsLoginCommand command) {
        // 1. 查找用户
        User user = userRepository.findByUsername(new Username(command.getPhone()))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 查找认证用户
        AuthUser authUser = authUserRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

        // 3. 验证是否可以登录
        try {
            authUser.validateCanLogin();
        } catch (BusinessException e) {
            recordLoginLog(user.getUserId(), AuthProvider.SMS, command.getIpAddress(),
                    command.getUserAgent(), false, e.getMessage());
            throw e;
        }

        // 4. 验证验证码（简化实现，实际应调用验证码服务）
        if (!"123456".equals(command.getVerificationCode())) {
            authUser.recordLoginFailure();
            authUserRepository.update(authUser);
            recordLoginLog(user.getUserId(), AuthProvider.SMS, command.getIpAddress(),
                    command.getUserAgent(), false, "验证码错误");
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        // 5. 生成Token
        JwtToken jwtToken = tokenService.generateTokenPair(user.getUserId());

        // 6. 创建会话
        UserSession session = createSession(user.getUserId(), jwtToken.getRefreshToken(),
                command.getIpAddress(), command.getUserAgent());

        // 7. 记录登录成功
        authUser.recordLoginSuccess();
        authUserRepository.update(authUser);

        // 8. 记录登录日志
        recordLoginLog(user.getUserId(), AuthProvider.SMS, command.getIpAddress(),
                command.getUserAgent(), true, null);

        return buildTokenResponse(jwtToken, user);
    }

    @Override
    @Transactional
    public void logout(LogoutCommand command) {
        UserId userId = new UserId(command.getUserId());

        if (command.getSessionId() != null && !command.getSessionId().isEmpty()) {
            // 删除指定会话
            userSessionRepository.deleteByUserId(userId);
        } else {
            // 删除所有会话
            userSessionRepository.deleteByUserId(userId);
        }
    }

    @Override
    @Transactional
    public TokenResponseDTO refreshToken(RefreshTokenCommand command) {
        // 1. 验证刷新令牌
        if (!tokenService.validateRefreshToken(command.getRefreshToken())) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 2. 查找会话
        UserSession session = userSessionRepository.findByRefreshToken(command.getRefreshToken())
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        if (session.isExpired()) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }

        // 3. 刷新Token
        JwtToken jwtToken = tokenService.refreshAccessToken(command.getRefreshToken());

        // 4. 更新会话
        UserSession newSession = UserSession.create(session.getUserId(), jwtToken.getRefreshToken(),
                LocalDateTime.now().plusDays(7));
        newSession.setDeviceInfo(session.getDeviceInfo());
        newSession.setIpAddress(session.getIpAddress());
        userSessionRepository.save(newSession);

        // 5. 删除旧会话
        userSessionRepository.deleteBySessionId(session.getSessionId());

        // 6. 查找用户信息
        User user = userRepository.findByUserId(session.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return buildTokenResponse(jwtToken, user);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordCommand command) {
        UserId userId = new UserId(command.getUserId());

        AuthUser authUser = authUserRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

        // 验证旧密码
        if (!passwordEncoder.matches(command.getOldPassword(), authUser.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 修改密码
        String newPasswordHash = passwordEncoder.encode(command.getNewPassword());
        authUser.changePassword(newPasswordHash);
        authUserRepository.update(authUser);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordCommand command) {
        // 查找用户
        User user = userRepository.findByUsername(new Username(command.getEmail()))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 验证验证码（简化实现）
        if (!"123456".equals(command.getVerificationCode())) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        AuthUser authUser = authUserRepository.findByUserId(user.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

        // 重置密码
        String newPasswordHash = passwordEncoder.encode(command.getNewPassword());
        authUser.changePassword(newPasswordHash);
        authUserRepository.update(authUser);
    }

    @Override
    public AuthUserDTO getAuthUser(String userId) {
        AuthUser authUser = authUserRepository.findByUserId(new UserId(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));
        return authUserAssembler.toDTO(authUser);
    }

    @Override
    public List<UserSessionDTO> getUserSessions(String userId) {
        List<UserSession> sessions = userSessionRepository.findByUserId(new UserId(userId));
        return sessions.stream()
                .map(userSessionAssembler::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<LoginLogDTO> getLoginLogs(String userId) {
        List<LoginLog> logs = loginLogRepository.findByUserId(new UserId(userId));
        return logs.stream()
                .map(loginLogAssembler::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void forceLogoutAllSessions(String userId) {
        userSessionRepository.deleteByUserId(new UserId(userId));
    }

    /**
     * 根据账号查找用户（邮箱或手机号）
     */
    private User findUserByAccount(String account) {
        Optional<User> user = Optional.empty();

        // 尝试作为邮箱查找
        if (account.contains("@")) {
            try {
                user = userRepository.findByUsername(new Username(account));
            } catch (Exception e) {
                // 忽略
            }
        } else {
            // 尝试作为手机号查找
            try {
                user = userRepository.findByUsername(new Username(account));
            } catch (Exception e) {
                // 忽略
            }
        }

        return user.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 创建会话
     */
    private UserSession createSession(UserId userId, String refreshToken, String ipAddress, String userAgent) {
        UserSession session = UserSession.create(userId, refreshToken, LocalDateTime.now().plusDays(7));
        if (userAgent != null) {
            session.setDeviceInfo(DeviceInfo.fromUserAgent(userAgent));
        }
        session.setIpAddress(ipAddress);
        return userSessionRepository.save(session);
    }

    /**
     * 记录登录日志
     */
    private void recordLoginLog(UserId userId, AuthProvider loginType, String ipAddress,
                                String userAgent, boolean success, String failReason) {
        DeviceInfo deviceInfo = userAgent != null ? DeviceInfo.fromUserAgent(userAgent) : null;
        LoginLog log;
        if (success) {
            log = LoginLog.createSuccess(userId, loginType, ipAddress, deviceInfo);
        } else {
            log = LoginLog.createFailure(userId, loginType, ipAddress, deviceInfo, failReason);
        }
        loginLogRepository.save(log);
    }

    /**
     * 构建Token响应
     */
    private TokenResponseDTO buildTokenResponse(JwtToken jwtToken, User user) {
        TokenResponseDTO dto = new TokenResponseDTO();
        dto.setAccessToken(jwtToken.getAccessToken());
        dto.setRefreshToken(jwtToken.getRefreshToken());
        dto.setExpiresIn(jwtToken.getExpiresIn());
        dto.setUserId(user.getUserIdValue());
        dto.setUsername(user.getUsername() != null ? user.getUsername().getValue() : null);
        dto.setEmail(user.getEmail() != null ? user.getEmail().getValue() : null);
        dto.setPhone(user.getPhone() != null ? user.getPhone().getValue() : null);
        return dto;
    }

    /**
     * 生成唯一邀请码
     */
    private InviteCode generateUniqueInviteCode() {
        String code;
        int maxAttempts = 100;
        int attempts = 0;

        do {
            code = inviteCodeGenerator.generate();
            attempts++;
        } while (userRepository.existsByInviteCode(new InviteCode(code)) && attempts < maxAttempts);

        if (attempts >= maxAttempts) {
            throw new BusinessException(ErrorCode.INVITE_CODE_ALREADY_EXISTS, "无法生成唯一邀请码");
        }

        return new InviteCode(code);
    }
}
