package com.claudej.domain.user.model.valobj;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void should_haveUserAndAdminRoles() {
        // Assert
        assertThat(Role.USER).isNotNull();
        assertThat(Role.ADMIN).isNotNull();
    }

    @Test
    void should_returnCorrectRoleName() {
        // Assert
        assertThat(Role.USER.name()).isEqualTo("USER");
        assertThat(Role.ADMIN.name()).isEqualTo("ADMIN");
    }

    @Test
    void should_parseRoleFromString() {
        // Act
        Role userRole = Role.valueOf("USER");
        Role adminRole = Role.valueOf("ADMIN");

        // Assert
        assertThat(userRole).isEqualTo(Role.USER);
        assertThat(adminRole).isEqualTo(Role.ADMIN);
    }

    @Test
    void should_checkIsAdmin() {
        // Assert
        assertThat(Role.USER.isAdmin()).isFalse();
        assertThat(Role.ADMIN.isAdmin()).isTrue();
    }

    @Test
    void should_checkIsUser() {
        // Assert
        assertThat(Role.USER.isUser()).isTrue();
        assertThat(Role.ADMIN.isUser()).isFalse();
    }
}