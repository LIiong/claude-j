package com.claudej.adapter.user.web;

import com.claudej.adapter.common.GlobalExceptionHandler;
import com.claudej.adapter.test.TestSecurityConfig;
import com.claudej.adapter.user.web.request.CreateUserRequest;
import com.claudej.adapter.user.web.request.ValidateInviteCodeRequest;
import com.claudej.application.user.dto.UserDTO;
import com.claudej.application.user.service.UserApplicationService;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {UserController.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserApplicationService userApplicationService;

    private UserDTO mockUserDTO;

    @BeforeEach
    void setUp() {
        mockUserDTO = new UserDTO();
        mockUserDTO.setUserId("UR1234567890ABCDEF");
        mockUserDTO.setUsername("testuser");
        mockUserDTO.setEmail("test@example.com");
        mockUserDTO.setPhone("13800138000");
        mockUserDTO.setStatus("ACTIVE");
        mockUserDTO.setInviteCode("ABC234");
        mockUserDTO.setCreateTime(LocalDateTime.now());
        mockUserDTO.setUpdateTime(LocalDateTime.now());
    }

    @Test
    void should_return200_when_createUserSuccess() throws Exception {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPhone("13800138000");

        when(userApplicationService.createUser(any())).thenReturn(mockUserDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is("UR1234567890ABCDEF")))
                .andExpect(jsonPath("$.data.username", is("testuser")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.inviteCode", is("ABC234")));
    }

    @Test
    void should_return400_when_createUserWithInvalidInput() throws Exception {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("a");  // Too short

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return200_when_getUserByIdSuccess() throws Exception {
        // Given
        when(userApplicationService.getUserById("UR1234567890ABCDEF")).thenReturn(mockUserDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/users/UR1234567890ABCDEF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is("UR1234567890ABCDEF")))
                .andExpect(jsonPath("$.data.username", is("testuser")));
    }

    @Test
    void should_return404_when_getNonExistentUser() throws Exception {
        // Given
        when(userApplicationService.getUserById("URNONEXISTENT"))
                .thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/api/v1/users/URNONEXISTENT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("USER_NOT_FOUND")));
    }

    @Test
    void should_return200_when_freezeUserSuccess() throws Exception {
        // Given
        mockUserDTO.setStatus("FROZEN");
        when(userApplicationService.freezeUser("UR1234567890ABCDEF")).thenReturn(mockUserDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/users/UR1234567890ABCDEF/freeze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("FROZEN")));
    }

    @Test
    void should_return200_when_unfreezeUserSuccess() throws Exception {
        // Given
        mockUserDTO.setStatus("ACTIVE");
        when(userApplicationService.unfreezeUser("UR1234567890ABCDEF")).thenReturn(mockUserDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/users/UR1234567890ABCDEF/unfreeze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));
    }

    @Test
    void should_return200_when_getInvitedUsersSuccess() throws Exception {
        // Given
        UserDTO invitedUser = new UserDTO();
        invitedUser.setUserId("UR0987654321FEDCBA");
        invitedUser.setUsername("inviteduser");
        invitedUser.setStatus("ACTIVE");

        when(userApplicationService.getInvitedUsers("UR1234567890ABCDEF"))
                .thenReturn(Arrays.asList(invitedUser));

        // When & Then
        mockMvc.perform(get("/api/v1/users/UR1234567890ABCDEF/invited-users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].userId", is("UR0987654321FEDCBA")))
                .andExpect(jsonPath("$.data[0].username", is("inviteduser")));
    }

    @Test
    void should_return200_when_validateInviteCodeSuccess() throws Exception {
        // Given
        ValidateInviteCodeRequest request = new ValidateInviteCodeRequest();
        request.setInviteCode("ABC234");

        when(userApplicationService.getUserByInviteCode("ABC234")).thenReturn(mockUserDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/users/validate-invite-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.inviteCode", is("ABC234")))
                .andExpect(jsonPath("$.data.username", is("testuser")));
    }

    @Test
    void should_return404_when_validateNonExistentInviteCode() throws Exception {
        // Given
        ValidateInviteCodeRequest request = new ValidateInviteCodeRequest();
        request.setInviteCode("ABC234");

        when(userApplicationService.getUserByInviteCode("ABC234"))
                .thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

        // When & Then
        mockMvc.perform(post("/api/v1/users/validate-invite-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("USER_NOT_FOUND")));
    }
}
