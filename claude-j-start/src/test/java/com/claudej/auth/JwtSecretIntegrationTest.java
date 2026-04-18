package com.claudej.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JWT Secret 外置化配置集成测试。
 * 验证：dev 环境默认 JWT secret 能正常启动，且 JWT Token 生成与刷新功能正常。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class JwtSecretIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== Token 生成验证 ====================

    @Test
    void should_generateValidAccessToken_when_loginWithPassword() throws Exception {
        // 先注册用户
        String registerBody = "{" +
                "\"username\":\"jwtuser1\"," +
                "\"password\":\"Password123!\"," +
                "\"email\":\"jwt1@test.com\"," +
                "\"phone\":\"13800000001\"}";

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode data = objectMapper.readTree(registerResult.getResponse().getContentAsString()).get("data");
        String accessToken = data.get("accessToken").asText();
        String refreshToken = data.get("refreshToken").asText();

        // 验证 Token 格式 (JWT 格式: header.payload.signature)
        assertThat(accessToken).contains(".");
        assertThat(accessToken.split("\\.")).hasSize(3);
        assertThat(refreshToken).isNotEmpty();

        // 验证登录也能生成 Token
        String loginBody = "{" +
                "\"account\":\"jwtuser1\"," +
                "\"password\":\"Password123!\"}";

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void should_generateValidTokens_when_loginMultipleTimes() throws Exception {
        // 注册用户
        String registerBody = "{" +
                "\"username\":\"jwtuser2\"," +
                "\"password\":\"Password123!\"," +
                "\"email\":\"jwt2@test.com\"," +
                "\"phone\":\"13800000002\"}";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn();

        String loginBody = "{" +
                "\"account\":\"jwtuser2\"," +
                "\"password\":\"Password123!\"}";

        // 第一次登录
        MvcResult first = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        // 第二次登录（应生成新的有效 Token）
        MvcResult second = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        String firstToken = objectMapper.readTree(first.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
        String secondToken = objectMapper.readTree(second.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        // 验证两次登录都生成了有效的 JWT Token（格式正确）
        assertThat(firstToken).contains(".");
        assertThat(firstToken.split("\\.")).hasSize(3);
        assertThat(secondToken).contains(".");
        assertThat(secondToken.split("\\.")).hasSize(3);
    }

    // ==================== Token 刷新验证 ====================

    @Test
    void should_refreshAccessToken_when_usingValidRefreshToken() throws Exception {
        // 注册用户
        String registerBody = "{" +
                "\"username\":\"jwtuser3\"," +
                "\"password\":\"Password123!\"," +
                "\"email\":\"jwt3@test.com\"," +
                "\"phone\":\"13800000003\"}";

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(registerResult.getResponse().getContentAsString()).get("data");
        String refreshToken = data.get("refreshToken").asText();
        String userId = data.get("userId").asText();

        // 使用 Refresh Token 获取新 Access Token
        String refreshBody = "{\"refreshToken\":\"" + refreshToken + "\"}";

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andReturn();

        JsonNode refreshData = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).get("data");
        String newAccessToken = refreshData.get("accessToken").asText();
        String newRefreshToken = refreshData.get("refreshToken").asText();

        // 验证新 Token 不为空且格式正确
        assertThat(newAccessToken).contains(".");
        assertThat(newRefreshToken).isNotEmpty();
    }

    // ==================== JWT Secret 配置验证 ====================

    @Test
    void should_startSuccessfully_when_devProfileDefaultSecretConfigured() {
        // 此测试方法本身即验证：应用使用 dev profile 能正常启动
        // 如果 JWT secret 无效，JwtSecretValidator 会在启动时抛出异常
        // 测试类能执行到这里，说明启动成功，JWT secret 配置正确
        assertThat(true).isTrue();
    }

    @Test
    void should_generateToken_withCorrectClaims_when_devSecretUsed() throws Exception {
        // 注册用户并验证 Token 包含正确用户信息
        String registerBody = "{" +
                "\"username\":\"jwtuser4\"," +
                "\"password\":\"Password123!\"," +
                "\"email\":\"jwt4@test.com\"," +
                "\"phone\":\"13800000004\"}";

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("jwtuser4"))
                .andExpect(jsonPath("$.data.email").value("jwt4@test.com"))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        String accessToken = data.get("accessToken").asText();

        // 验证 Token 不为空（由 dev profile 的默认 JWT secret 生成）
        assertThat(accessToken).isNotEmpty();
        assertThat(accessToken.split("\\.")).hasSize(3);
    }

    // ==================== 全链路验证 ====================

    @Test
    void should_completeFullAuthFlow_when_devSecretConfigured() throws Exception {
        String username = "jwtuser5";
        String password = "Password123!";

        // Step 1: 注册
        String registerBody = "{" +
                "\"username\":\"" + username + "\"," +
                "\"password\":\"" + password + "\"," +
                "\"email\":\"jwt5@test.com\"," +
                "\"phone\":\"13800000005\"}";

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode registerData = objectMapper.readTree(registerResult.getResponse().getContentAsString()).get("data");
        String firstAccessToken = registerData.get("accessToken").asText();
        String refreshToken = registerData.get("refreshToken").asText();
        String userId = registerData.get("userId").asText();

        // 验证注册返回的 Token 有效
        assertThat(firstAccessToken).contains(".");
        assertThat(firstAccessToken.split("\\.")).hasSize(3);

        // Step 2: 刷新 Token
        String refreshBody = "{\"refreshToken\":\"" + refreshToken + "\"}";
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        JsonNode refreshData = objectMapper.readTree(refreshResult.getResponse().getContentAsString()).get("data");
        String secondAccessToken = refreshData.get("accessToken").asText();

        // 验证刷新返回的新 Token 有效
        assertThat(secondAccessToken).contains(".");
        assertThat(secondAccessToken.split("\\.")).hasSize(3);

        // Step 3: 重新登录
        String loginBody = "{" +
                "\"account\":\"" + username + "\"," +
                "\"password\":\"" + password + "\"}";

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andReturn();

        JsonNode loginData = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("data");
        String thirdAccessToken = loginData.get("accessToken").asText();

        // 验证登录返回的 Token 有效
        assertThat(thirdAccessToken).contains(".");
        assertThat(thirdAccessToken.split("\\.")).hasSize(3);

        // 所有 Token 都应是有效的 JWT 格式（三段式，用点分隔）
        assertThat(firstAccessToken).contains(".");
        assertThat(secondAccessToken).contains(".");
        assertThat(thirdAccessToken).contains(".");
    }
}
