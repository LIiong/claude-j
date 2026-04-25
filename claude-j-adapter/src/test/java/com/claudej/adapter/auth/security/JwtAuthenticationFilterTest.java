package com.claudej.adapter.auth.security;

import com.claudej.domain.auth.service.TokenService;
import com.claudej.domain.user.model.valobj.Role;
import com.claudej.domain.user.model.valobj.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter 测试
 * 测试 token 角色提取和 GrantedAuthority 构建
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(tokenService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_buildGrantedAuthorities_when_tokenContainsRoles() throws Exception {
        // Arrange
        String validToken = "valid.jwt.token";
        UserId userId = new UserId("UR1234567890ABCDEF");
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);
        roles.add(Role.ADMIN);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(tokenService.validateAccessToken(validToken)).thenReturn(true);
        when(tokenService.extractUserIdFromToken(validToken)).thenReturn(userId);
        when(tokenService.extractRolesFromToken(validToken)).thenReturn(roles);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo("UR1234567890ABCDEF");
        assertThat(authentication.getAuthorities()).hasSize(2);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("ROLE_USER", "ROLE_ADMIN");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void should_defaultToUserRole_when_tokenHasNoRoles() throws Exception {
        // Arrange
        String validToken = "valid.jwt.token";
        UserId userId = new UserId("UR1234567890ABCDEF");
        Set<Role> defaultRoles = new HashSet<>();
        defaultRoles.add(Role.USER);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(tokenService.validateAccessToken(validToken)).thenReturn(true);
        when(tokenService.extractUserIdFromToken(validToken)).thenReturn(userId);
        when(tokenService.extractRolesFromToken(validToken)).thenReturn(defaultRoles);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).hasSize(1);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void should_clearSecurityContext_when_tokenInvalid() throws Exception {
        // Arrange
        String invalidToken = "invalid.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(tokenService.validateAccessToken(invalidToken)).thenReturn(false);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
        verify(tokenService, never()).extractUserIdFromToken(anyString());
        verify(tokenService, never()).extractRolesFromToken(anyString());
    }

    @Test
    void should_clearSecurityContext_when_noTokenProvided() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
        verify(tokenService, never()).validateAccessToken(anyString());
    }

    @Test
    void should_clearSecurityContext_when_tokenServiceThrowsException() throws Exception {
        // Arrange
        String validToken = "valid.jwt.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(tokenService.validateAccessToken(validToken)).thenReturn(true);
        when(tokenService.extractUserIdFromToken(validToken)).thenThrow(new RuntimeException("Token parsing error"));

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void should_notProcessToken_when_headerNotBearerFormat() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Basic somecredentials");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();

        verify(filterChain).doFilter(request, response);
        verify(tokenService, never()).validateAccessToken(anyString());
    }
}