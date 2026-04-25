package com.claudej.domain.user.model.aggregate;

import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.user.model.valobj.Email;
import com.claudej.domain.user.model.valobj.InviteCode;
import com.claudej.domain.user.model.valobj.Phone;
import com.claudej.domain.user.model.valobj.Role;
import com.claudej.domain.user.model.valobj.UserId;
import com.claudej.domain.user.model.valobj.UserStatus;
import com.claudej.domain.user.model.valobj.Username;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void should_createUser_when_validInput() {
        // Arrange
        Username username = new Username("testuser");
        InviteCode inviteCode = InviteCode.generate();

        // Act
        User user = User.create(username, inviteCode);

        // Assert
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getInviteCode()).isEqualTo(inviteCode);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getUserId()).isNotNull();
        assertThat(user.getUserIdValue()).startsWith("UR");
        assertThat(user.isActive()).isTrue();
        assertThat(user.isFrozen()).isFalse();
        assertThat(user.isInvited()).isFalse();
        assertThat(user.getRoles()).containsExactly(Role.USER);
    }

    @Test
    void should_throwException_when_usernameIsNull() {
        // Arrange
        InviteCode inviteCode = InviteCode.generate();

        // Act & Assert
        assertThatThrownBy(() -> User.create(null, inviteCode))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名不能为空");
    }

    @Test
    void should_throwException_when_inviteCodeIsNull() {
        // Arrange
        Username username = new Username("testuser");

        // Act & Assert
        assertThatThrownBy(() -> User.create(username, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邀请码不能为空");
    }

    @Test
    void should_reconstructUser_when_validInput() {
        // Arrange
        Long id = 1L;
        UserId userId = UserId.generate();
        Username username = new Username("testuser");
        Email email = new Email("test@example.com");
        Phone phone = new Phone("13800138000");
        UserStatus status = UserStatus.ACTIVE;
        InviteCode inviteCode = new InviteCode("ABC234");
        UserId inviterId = UserId.generate();
        Set<Role> roles = new HashSet<>(Arrays.asList(Role.USER));
        LocalDateTime createTime = LocalDateTime.now();
        LocalDateTime updateTime = LocalDateTime.now();

        // Act
        User user = User.reconstruct(id, userId, username, email, phone, status,
                inviteCode, inviterId, roles, createTime, updateTime);

        // Assert
        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getUserId()).isEqualTo(userId);
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getPhone()).isEqualTo(phone);
        assertThat(user.getStatus()).isEqualTo(status);
        assertThat(user.getInviteCode()).isEqualTo(inviteCode);
        assertThat(user.getInviterId()).isEqualTo(inviterId);
        assertThat(user.isInvited()).isTrue();
        assertThat(user.getRoles()).isEqualTo(roles);
    }

    @Test
    void should_setEmail() {
        // Arrange
        User user = createTestUser();
        Email email = new Email("test@example.com");

        // Act
        user.setEmail(email);

        // Assert
        assertThat(user.getEmail()).isEqualTo(email);
    }

    @Test
    void should_setPhone() {
        // Arrange
        User user = createTestUser();
        Phone phone = new Phone("13800138000");

        // Act
        user.setPhone(phone);

        // Assert
        assertThat(user.getPhone()).isEqualTo(phone);
    }

    @Test
    void should_setInviter() {
        // Arrange
        User user = createTestUser();
        UserId inviterId = UserId.generate();

        // Act
        user.setInviter(inviterId);

        // Assert
        assertThat(user.getInviterId()).isEqualTo(inviterId);
        assertThat(user.isInvited()).isTrue();
    }

    @Test
    void should_freezeUser() {
        // Arrange
        User user = createTestUser();

        // Act
        user.freeze();

        // Assert
        assertThat(user.getStatus()).isEqualTo(UserStatus.FROZEN);
        assertThat(user.isFrozen()).isTrue();
        assertThat(user.isActive()).isFalse();
    }

    @Test
    void should_unfreezeUser() {
        // Arrange
        User user = createFrozenUser();

        // Act
        user.unfreeze();

        // Assert
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.isFrozen()).isFalse();
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void should_deactivateUser() {
        // Arrange
        User user = createTestUser();

        // Act
        user.deactivate();

        // Assert
        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(user.isActive()).isFalse();
    }

    @Test
    void should_activateUser() {
        // Arrange
        User user = createInactiveUser();

        // Act
        user.activate();

        // Assert
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void should_setId() {
        // Arrange
        User user = createTestUser();
        Long id = 100L;

        // Act
        user.setId(id);

        // Assert
        assertThat(user.getId()).isEqualTo(id);
    }

    @Test
    void should_returnInviteCodeValue() {
        // Arrange
        InviteCode inviteCode = new InviteCode("ABC234");
        User user = User.create(new Username("testuser"), inviteCode);

        // Act & Assert
        assertThat(user.getInviteCodeValue()).isEqualTo("ABC234");
    }

    @Test
    void should_returnInviterIdValue_when_hasInviter() {
        // Arrange
        User user = createTestUser();
        UserId inviterId = UserId.generate();
        user.setInviter(inviterId);

        // Act & Assert
        assertThat(user.getInviterIdValue()).isEqualTo(inviterId.getValue());
    }

    @Test
    void should_returnNull_when_noInviter() {
        // Arrange
        User user = createTestUser();

        // Act & Assert
        assertThat(user.getInviterIdValue()).isNull();
    }

    @Test
    void should_throwException_when_freezingFrozenUser() {
        // Arrange
        User user = createFrozenUser();

        // Act & Assert
        assertThatThrownBy(user::freeze)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许冻结");
    }

    @Test
    void should_throwException_when_unfreezingActiveUser() {
        // Arrange
        User user = createTestUser();

        // Act & Assert
        assertThatThrownBy(user::unfreeze)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许激活");
    }

    private User createTestUser() {
        return User.create(new Username("testuser"), InviteCode.generate());
    }

    private User createFrozenUser() {
        User user = createTestUser();
        user.freeze();
        return user;
    }

    private User createInactiveUser() {
        User user = createTestUser();
        user.deactivate();
        return user;
    }
}
