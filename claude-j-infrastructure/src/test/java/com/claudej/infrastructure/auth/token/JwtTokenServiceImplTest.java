package com.claudej.infrastructure.auth.token;

import com.claudej.domain.auth.model.valobj.JwtToken;
import com.claudej.domain.user.model.valobj.Role;
import com.claudej.domain.user.model.valobj.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtTokenServiceImpl 测试
 * 测试角色提取逻辑
 */
class JwtTokenServiceImplTest {

    // 测试用的 secret（至少 256 位，满足 HMAC-SHA256 要求）
    private static final String TEST_SECRET = "test-secret-key-for-jwt-token-generation-minimum-256-bits";

    private JwtTokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new JwtTokenServiceImpl(TEST_SECRET, 60, 7);
    }

    @Test
    void should_extractRolesFromValidToken() {
        // Arrange
        UserId userId = new UserId("UR1234567890ABCDEF");
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);
        roles.add(Role.ADMIN);

        JwtToken tokenPair = tokenService.generateTokenPair(userId, roles);
        String accessToken = tokenPair.getAccessToken();

        // Act
        Set<Role> extractedRoles = tokenService.extractRolesFromToken(accessToken);

        // Assert
        assertThat(extractedRoles).isNotNull();
        assertThat(extractedRoles).hasSize(2);
        assertThat(extractedRoles).contains(Role.USER, Role.ADMIN);
    }

    @Test
    void should_returnDefaultUserRole_when_tokenHasNoRoles() {
        // Arrange
        UserId userId = new UserId("UR1234567890ABCDEF");
        // 使用 generateTokenPair(userId) 会生成默认只有 USER 角色的 token

        JwtToken tokenPair = tokenService.generateTokenPair(userId);
        String accessToken = tokenPair.getAccessToken();

        // Act
        Set<Role> extractedRoles = tokenService.extractRolesFromToken(accessToken);

        // Assert
        assertThat(extractedRoles).isNotNull();
        assertThat(extractedRoles).hasSize(1);
        assertThat(extractedRoles).contains(Role.USER);
    }

    @Test
    void should_returnDefaultUserRole_when_tokenInvalid() {
        // Arrange
        String invalidToken = "invalid.token.string";

        // Act
        Set<Role> extractedRoles = tokenService.extractRolesFromToken(invalidToken);

        // Assert
        assertThat(extractedRoles).isNotNull();
        assertThat(extractedRoles).hasSize(1);
        assertThat(extractedRoles).contains(Role.USER);
    }

    @Test
    void should_validateAccessToken_when_valid() {
        // Arrange
        UserId userId = new UserId("UR1234567890ABCDEF");
        JwtToken tokenPair = tokenService.generateTokenPair(userId);
        String accessToken = tokenPair.getAccessToken();

        // Act
        boolean isValid = tokenService.validateAccessToken(accessToken);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    void should_notValidateAccessToken_when_invalid() {
        // Arrange
        String invalidToken = "invalid.token.string";

        // Act
        boolean isValid = tokenService.validateAccessToken(invalidToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    void should_extractUserIdFromValidToken() {
        // Arrange
        UserId userId = new UserId("UR1234567890ABCDEF");
        JwtToken tokenPair = tokenService.generateTokenPair(userId);
        String accessToken = tokenPair.getAccessToken();

        // Act
        UserId extractedUserId = tokenService.extractUserIdFromToken(accessToken);

        // Assert
        assertThat(extractedUserId).isNotNull();
        assertThat(extractedUserId.getValue()).isEqualTo("UR1234567890ABCDEF");
    }

    @Test
    void should_returnNullUserId_when_tokenInvalid() {
        // Arrange
        String invalidToken = "invalid.token.string";

        // Act
        UserId extractedUserId = tokenService.extractUserIdFromToken(invalidToken);

        // Assert
        assertThat(extractedUserId).isNull();
    }
}