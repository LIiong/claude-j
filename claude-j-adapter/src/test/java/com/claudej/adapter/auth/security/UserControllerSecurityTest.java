package com.claudej.adapter.auth.security;

import com.claudej.adapter.common.ApiResult;
import com.claudej.adapter.common.GlobalExceptionHandler;
import com.claudej.adapter.user.web.UserController;
import com.claudej.application.user.dto.UserDTO;
import com.claudej.application.user.service.UserApplicationService;
import com.claudej.domain.auth.service.TokenService;
import com.claudej.domain.user.model.valobj.Role;
import com.claudej.domain.user.model.valobj.UserId;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController 安全测试
 * 测试授权逻辑（401/403 响应）
 * 注意：此测试启用安全过滤器
 */
@WebMvcTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = {UserController.class,
                                  SecurityConfig.class, JwtAuthenticationFilter.class,
                                  JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class,
                                  UserControllerSecurityTest.TestConfig.class})
@ActiveProfiles("security-test")
class UserControllerSecurityTest {

    /**
     * 测试配置 - 包含 JSON 和异常处理配置
     */
    @Configuration
    @RestControllerAdvice
    static class TestConfig {

        private static final Logger log = LoggerFactory.getLogger(TestConfig.class);

        @Bean
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper;
        }

        /**
         * 处理方法级安全抛出的 AccessDeniedException
         */
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiResult<Void>> handleAccessDeniedException(AccessDeniedException ex) {
            log.warn("Access denied: {}", ex.getMessage());
            ApiResult<Void> result = ApiResult.fail("403", "权限不足：您没有访问此资源的权限");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
    }

    // 使用硬编码的测试 token 字符串（TokenService 被 mock，内容不重要）
    private static final String USER_TOKEN = "test-user-token";
    private static final String ADMIN_TOKEN = "test-admin-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserApplicationService userApplicationService;

    @MockBean
    private TokenService tokenService;

    private UserDTO mockUserDTO;

    @BeforeEach
    void setUp() {
        mockUserDTO = new UserDTO();
        mockUserDTO.setUserId("UR1234567890ABCDEF");
        mockUserDTO.setUsername("testuser");
        mockUserDTO.setEmail("test@example.com");
        mockUserDTO.setPhone("13800138000");
        mockUserDTO.setStatus("ACTIVE");
        mockUserDTO.setCreateTime(LocalDateTime.now());
        mockUserDTO.setUpdateTime(LocalDateTime.now());

        // Mock TokenService behavior - USER token
        when(tokenService.validateAccessToken(USER_TOKEN)).thenReturn(true);
        when(tokenService.extractUserIdFromToken(USER_TOKEN))
                .thenReturn(new UserId("UR1234567890ABCDEF"));
        Set<Role> userRoles = new HashSet<>();
        userRoles.add(Role.USER);
        when(tokenService.extractRolesFromToken(USER_TOKEN)).thenReturn(userRoles);

        // Mock TokenService behavior - ADMIN token
        when(tokenService.validateAccessToken(ADMIN_TOKEN)).thenReturn(true);
        when(tokenService.extractUserIdFromToken(ADMIN_TOKEN))
                .thenReturn(new UserId("UR9876543210ABCDEF"));
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(Role.USER);
        adminRoles.add(Role.ADMIN);
        when(tokenService.extractRolesFromToken(ADMIN_TOKEN)).thenReturn(adminRoles);

        // Mock service
        when(userApplicationService.freezeUser(any())).thenReturn(mockUserDTO);
        when(userApplicationService.getUserById(any())).thenReturn(mockUserDTO);
    }

    @Test
    void should_return403_when_userAccessAdminEndpoint() throws Exception {
        // Arrange - USER token accessing ADMIN endpoint (freezeUser)

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/UR1234567890ABCDEF/freeze")
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("403")));
    }

    @Test
    void should_return200_when_adminAccessAdminEndpoint() throws Exception {
        // Arrange - ADMIN token accessing ADMIN endpoint

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/UR1234567890ABCDEF/freeze")
                        .header("Authorization", "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void should_return401_when_noTokenProvided() throws Exception {
        // Arrange - No token accessing authenticated endpoint

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/UR1234567890ABCDEF"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("401")));
    }

    @Test
    void should_return401_when_invalidTokenProvided() throws Exception {
        // Arrange
        when(tokenService.validateAccessToken("invalid.token")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/UR1234567890ABCDEF")
                        .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return200_when_userAccessUserEndpoint() throws Exception {
        // Arrange - USER token accessing USER endpoint

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/UR1234567890ABCDEF")
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}