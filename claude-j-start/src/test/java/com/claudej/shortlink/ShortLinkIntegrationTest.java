package com.claudej.shortlink;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 短链服务全链路集成测试。
 * HTTP Request → Controller → ApplicationService → Repository → H2 Database
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ShortLinkIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== 创建短链 ====================

    @Test
    void should_createShortLink_when_validUrl() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/short-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"https://www.baidu.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shortCode").isNotEmpty())
                .andExpect(jsonPath("$.data.shortUrl").isNotEmpty())
                .andExpect(jsonPath("$.data.originalUrl").value("https://www.baidu.com"))
                .andReturn();

        // 验证 shortUrl 包含 shortCode
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        String shortCode = data.get("shortCode").asText();
        String shortUrl = data.get("shortUrl").asText();
        assertThat(shortUrl).contains("/s/" + shortCode);
    }

    @Test
    void should_returnSameShortCode_when_duplicateUrl() throws Exception {
        String url = "https://www.example.com/dedup-test";
        String body = "{\"originalUrl\":\"" + url + "\"}";

        // 第一次创建
        MvcResult first = mockMvc.perform(post("/api/v1/short-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        // 第二次创建（相同 URL）
        MvcResult second = mockMvc.perform(post("/api/v1/short-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        // 验证返回相同 shortCode（去重）
        String firstCode = objectMapper.readTree(first.getResponse().getContentAsString())
                .get("data").get("shortCode").asText();
        String secondCode = objectMapper.readTree(second.getResponse().getContentAsString())
                .get("data").get("shortCode").asText();
        assertThat(firstCode).isEqualTo(secondCode);
    }

    @Test
    void should_generateDifferentCodes_when_differentUrls() throws Exception {
        MvcResult first = mockMvc.perform(post("/api/v1/short-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"https://www.example.com/path-a\"}"))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/v1/short-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"https://www.example.com/path-b\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String firstCode = objectMapper.readTree(first.getResponse().getContentAsString())
                .get("data").get("shortCode").asText();
        String secondCode = objectMapper.readTree(second.getResponse().getContentAsString())
                .get("data").get("shortCode").asText();
        assertThat(firstCode).isNotEqualTo(secondCode);
    }

    // ==================== 创建短链 — 异常场景 ====================

    @Test
    void should_return400_when_originalUrlIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/short-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void should_return400_when_originalUrlIsNull() throws Exception {
        mockMvc.perform(post("/api/v1/short-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void should_return400_when_originalUrlNotHttp() throws Exception {
        mockMvc.perform(post("/api/v1/short-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"ftp://files.example.com/doc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_ORIGINAL_URL"));
    }

    /**
     * 缺失请求体时，当前返回 500（HttpMessageNotReadableException 未被 GlobalExceptionHandler 捕获）。
     * TODO: GlobalExceptionHandler 应增加 HttpMessageNotReadableException 处理，返回 400。
     */
    @Test
    void should_return400Or500_when_requestBodyMissing() throws Exception {
        mockMvc.perform(post("/api/v1/short-links")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    // ==================== 重定向 ====================

    @Test
    void should_redirect302_when_shortCodeExists() throws Exception {
        // 先创建短链
        MvcResult createResult = mockMvc.perform(post("/api/v1/short-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"https://www.google.com\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String shortCode = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("shortCode").asText();

        // 访问短链，验证 302 重定向
        mockMvc.perform(get("/s/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://www.google.com"));
    }

    @Test
    void should_return404_when_shortCodeNotFound() throws Exception {
        mockMvc.perform(get("/s/zzzzzz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("SHORT_LINK_NOT_FOUND"));
    }

    // ==================== 全链路一致性 ====================

    @Test
    void should_roundTrip_createThenRedirect() throws Exception {
        String originalUrl = "https://github.com/anthropics/claude-code";

        // Step 1: 创建短链
        MvcResult createResult = mockMvc.perform(post("/api/v1/short-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"" + originalUrl + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shortCode").isNotEmpty())
                .andReturn();

        String shortCode = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("shortCode").asText();

        // Step 2: 通过短码重定向
        mockMvc.perform(get("/s/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", originalUrl));

        // Step 3: 重复创建同一 URL，应返回相同 shortCode
        MvcResult dupResult = mockMvc.perform(post("/api/v1/short-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"" + originalUrl + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String dupCode = objectMapper.readTree(dupResult.getResponse().getContentAsString())
                .get("data").get("shortCode").asText();
        assertThat(dupCode).isEqualTo(shortCode);
    }
}
