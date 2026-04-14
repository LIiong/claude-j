package com.claudej.application.user.service;

import com.claudej.application.user.assembler.UserAssembler;
import com.claudej.application.user.command.CreateUserCommand;
import com.claudej.application.user.dto.UserDTO;
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

import java.util.List;

/**
 * 用户应用服务
 */
@Service
public class UserApplicationService {

    private final UserRepository userRepository;
    private final UserAssembler userAssembler;
    private final InviteCodeGenerator inviteCodeGenerator;

    public UserApplicationService(UserRepository userRepository,
                                  UserAssembler userAssembler,
                                  InviteCodeGenerator inviteCodeGenerator) {
        this.userRepository = userRepository;
        this.userAssembler = userAssembler;
        this.inviteCodeGenerator = inviteCodeGenerator;
    }

    /**
     * 创建用户
     */
    @Transactional
    public UserDTO createUser(CreateUserCommand command) {
        if (command == null || command.getUsername() == null || command.getUsername().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_USERNAME, "用户名不能为空");
        }

        Username username = new Username(command.getUsername());

        // 检查用户名是否已存在
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        // 生成唯一邀请码
        InviteCode inviteCode = generateUniqueInviteCode();

        // 创建用户
        User user = User.create(username, inviteCode);

        // 设置邮箱（可选）
        if (command.getEmail() != null && !command.getEmail().trim().isEmpty()) {
            user.setEmail(new Email(command.getEmail()));
        }

        // 设置手机号（可选）
        if (command.getPhone() != null && !command.getPhone().trim().isEmpty()) {
            user.setPhone(new Phone(command.getPhone()));
        }

        // 设置邀请人（可选）
        if (command.getInviteCode() != null && !command.getInviteCode().trim().isEmpty()) {
            InviteCode inviterCode = new InviteCode(command.getInviteCode());
            User inviter = userRepository.findByInviteCode(inviterCode)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_CODE, "邀请码无效"));
            user.setInviter(inviter.getUserId());
        }

        user = userRepository.save(user);
        return userAssembler.toDTO(user);
    }

    /**
     * 根据用户ID查询用户
     */
    public UserDTO getUserById(String userId) {
        User user = userRepository.findByUserId(new UserId(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return userAssembler.toDTO(user);
    }

    /**
     * 根据用户名查询用户
     */
    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(new Username(username))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return userAssembler.toDTO(user);
    }

    /**
     * 根据邀请码查询用户
     */
    public UserDTO getUserByInviteCode(String inviteCode) {
        User user = userRepository.findByInviteCode(new InviteCode(inviteCode))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return userAssembler.toDTO(user);
    }

    /**
     * 查询被邀请的用户列表
     */
    public List<UserDTO> getInvitedUsers(String userId) {
        List<User> users = userRepository.findByInviterId(new UserId(userId));
        return userAssembler.toDTOList(users);
    }

    /**
     * 冻结用户
     */
    @Transactional
    public UserDTO freezeUser(String userId) {
        User user = userRepository.findByUserId(new UserId(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.freeze();
        user = userRepository.save(user);
        return userAssembler.toDTO(user);
    }

    /**
     * 解冻用户
     */
    @Transactional
    public UserDTO unfreezeUser(String userId) {
        User user = userRepository.findByUserId(new UserId(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.unfreeze();
        user = userRepository.save(user);
        return userAssembler.toDTO(user);
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
