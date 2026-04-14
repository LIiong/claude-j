package com.claudej.application.user.service;

import com.claudej.application.user.assembler.UserAssembler;
import com.claudej.application.user.command.CreateUserCommand;
import com.claudej.application.user.dto.UserDTO;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.user.model.aggregate.User;
import com.claudej.domain.user.model.valobj.InviteCode;
import com.claudej.domain.user.model.valobj.UserId;
import com.claudej.domain.user.model.valobj.Username;
import com.claudej.domain.user.repository.UserRepository;
import com.claudej.domain.user.service.InviteCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserApplicationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAssembler userAssembler;

    @Mock
    private InviteCodeGenerator inviteCodeGenerator;

    @InjectMocks
    private UserApplicationService userApplicationService;

    private CreateUserCommand createCommand;
    private User mockUser;
    private UserDTO mockUserDTO;

    @BeforeEach
    void setUp() {
        createCommand = new CreateUserCommand();
        createCommand.setUsername("testuser");

        mockUser = User.create(new Username("testuser"), new InviteCode("ABC234"));

        mockUserDTO = new UserDTO();
        mockUserDTO.setUserId("UR1234567890ABCDEF");
        mockUserDTO.setUsername("testuser");
        mockUserDTO.setStatus("ACTIVE");
        mockUserDTO.setInviteCode("ABC234");
    }

    @Test
    void should_createUser_when_validCommandProvided() {
        // Given
        when(userRepository.existsByUsername(any(Username.class))).thenReturn(false);
        when(inviteCodeGenerator.generate()).thenReturn("ABC234");
        when(userRepository.existsByInviteCode(any(InviteCode.class))).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(userAssembler.toDTO(any(User.class))).thenReturn(mockUserDTO);

        // When
        UserDTO result = userApplicationService.createUser(createCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void should_throwException_when_createUserWithNullCommand() {
        // When & Then
        assertThatThrownBy(() -> userApplicationService.createUser(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名不能为空");
    }

    @Test
    void should_throwException_when_createUserWithEmptyUsername() {
        // Given
        createCommand.setUsername("");

        // When & Then
        assertThatThrownBy(() -> userApplicationService.createUser(createCommand))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名不能为空");
    }

    @Test
    void should_throwException_when_usernameAlreadyExists() {
        // Given
        when(userRepository.existsByUsername(any(Username.class))).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userApplicationService.createUser(createCommand))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已存在");
    }

    @Test
    void should_createUserWithInviter_when_inviteCodeProvided() {
        // Given
        createCommand.setInviteCode("DEF567");

        User inviter = User.create(new Username("inviter"), new InviteCode("DEF567"));

        when(userRepository.existsByUsername(any(Username.class))).thenReturn(false);
        when(inviteCodeGenerator.generate()).thenReturn("ABC234");
        when(userRepository.existsByInviteCode(any(InviteCode.class))).thenReturn(false);
        when(userRepository.findByInviteCode(any(InviteCode.class))).thenReturn(Optional.of(inviter));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(userAssembler.toDTO(any(User.class))).thenReturn(mockUserDTO);

        // When
        UserDTO result = userApplicationService.createUser(createCommand);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void should_throwException_when_inviteCodeInvalid() {
        // Given
        createCommand.setInviteCode("ABCDEF");

        when(userRepository.existsByUsername(any(Username.class))).thenReturn(false);
        when(inviteCodeGenerator.generate()).thenReturn("ABC234");
        when(userRepository.existsByInviteCode(any(InviteCode.class))).thenReturn(false);
        when(userRepository.findByInviteCode(any(InviteCode.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userApplicationService.createUser(createCommand))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邀请码无效");
    }

    @Test
    void should_returnUser_when_getUserByIdExists() {
        // Given
        when(userRepository.findByUserId(any(UserId.class))).thenReturn(Optional.of(mockUser));
        when(userAssembler.toDTO(any(User.class))).thenReturn(mockUserDTO);

        // When
        UserDTO result = userApplicationService.getUserById("UR1234567890ABCDEF");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    void should_throwException_when_getUserByIdNotExists() {
        // Given
        when(userRepository.findByUserId(any(UserId.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userApplicationService.getUserById("UR1234567890ABCDEF"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    void should_freezeUser_when_userExistsAndActive() {
        // Given
        when(userRepository.findByUserId(any(UserId.class))).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(userAssembler.toDTO(any(User.class))).thenReturn(mockUserDTO);

        // When
        UserDTO result = userApplicationService.freezeUser("UR1234567890ABCDEF");

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void should_unfreezeUser_when_userExistsAndFrozen() {
        // Given
        mockUser.freeze();
        when(userRepository.findByUserId(any(UserId.class))).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(userAssembler.toDTO(any(User.class))).thenReturn(mockUserDTO);

        // When
        UserDTO result = userApplicationService.unfreezeUser("UR1234567890ABCDEF");

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void should_returnInvitedUsers_when_userHasInvitedOthers() {
        // Given
        User invitedUser = User.create(new Username("invited"), new InviteCode("GHJ234"));
        when(userRepository.findByInviterId(any(UserId.class))).thenReturn(Arrays.asList(invitedUser));
        when(userAssembler.toDTOList(any())).thenReturn(Arrays.asList(mockUserDTO));

        // When
        java.util.List<UserDTO> result = userApplicationService.getInvitedUsers("UR1234567890ABCDEF");

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
    }
}
