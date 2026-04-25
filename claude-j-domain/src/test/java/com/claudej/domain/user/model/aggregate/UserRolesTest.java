package com.claudej.domain.user.model.aggregate;

import com.claudej.domain.user.model.valobj.InviteCode;
import com.claudej.domain.user.model.valobj.Role;
import com.claudej.domain.user.model.valobj.UserId;
import com.claudej.domain.user.model.valobj.Username;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserRolesTest {

    @Test
    void should_createUserWithDefaultUserRole() {
        // Arrange
        Username username = new Username("testuser");
        InviteCode inviteCode = InviteCode.generate();

        // Act
        User user = User.create(username, inviteCode);

        // Assert
        assertThat(user.getRoles()).isNotNull();
        assertThat(user.getRoles()).containsExactly(Role.USER);
    }

    @Test
    void should_reconstructUserWithRoles() {
        // Arrange
        Long id = 1L;
        UserId userId = UserId.generate();
        Username username = new Username("testuser");
        Set<Role> roles = new HashSet<>(Arrays.asList(Role.USER, Role.ADMIN));

        // Act
        User user = User.reconstruct(id, userId, username, null, null, null,
                InviteCode.generate(), null, roles, null, null);

        // Assert
        assertThat(user.getRoles()).isEqualTo(roles);
        assertThat(user.getRoles()).contains(Role.USER, Role.ADMIN);
    }

    @Test
    void should_haveUserRole_when_newUser() {
        // Arrange
        User user = createTestUser();

        // Assert
        assertThat(user.hasRole(Role.USER)).isTrue();
        assertThat(user.hasRole(Role.ADMIN)).isFalse();
    }

    @Test
    void should_checkAdminRole_when_hasAdmin() {
        // Arrange
        User user = createAdminUser();

        // Assert
        assertThat(user.hasRole(Role.ADMIN)).isTrue();
        assertThat(user.isAdmin()).isTrue();
    }

    @Test
    void should_checkNotAdmin_when_onlyUser() {
        // Arrange
        User user = createTestUser();

        // Assert
        assertThat(user.isAdmin()).isFalse();
    }

    @Test
    void should_addRole() {
        // Arrange
        User user = createTestUser();

        // Act
        user.addRole(Role.ADMIN);

        // Assert
        assertThat(user.getRoles()).contains(Role.USER, Role.ADMIN);
        assertThat(user.isAdmin()).isTrue();
    }

    @Test
    void should_notAddDuplicateRole() {
        // Arrange
        User user = createTestUser();
        int initialSize = user.getRoles().size();

        // Act
        user.addRole(Role.USER);

        // Assert
        assertThat(user.getRoles().size()).isEqualTo(initialSize);
    }

    @Test
    void should_removeRole() {
        // Arrange
        User user = createAdminUser();

        // Act
        user.removeRole(Role.ADMIN);

        // Assert
        assertThat(user.getRoles()).containsExactly(Role.USER);
        assertThat(user.isAdmin()).isFalse();
    }

    @Test
    void should_notRemoveLastRole() {
        // Arrange
        User user = createTestUser();

        // Act & Assert - should throw or not remove
        // User must have at least one role
        assertThat(user.getRoles()).contains(Role.USER);
        // After trying to remove USER, should still have USER
        user.removeRole(Role.USER);
        assertThat(user.getRoles()).contains(Role.USER);
    }

    @Test
    void should_returnRolesAsString() {
        // Arrange
        User user = createAdminUser();

        // Act
        String rolesStr = user.getRolesAsString();

        // Assert
        assertThat(rolesStr).contains("USER");
        assertThat(rolesStr).contains("ADMIN");
    }

    private User createTestUser() {
        return User.create(new Username("testuser"), InviteCode.generate());
    }

    private User createAdminUser() {
        User user = createTestUser();
        user.addRole(Role.ADMIN);
        return user;
    }
}