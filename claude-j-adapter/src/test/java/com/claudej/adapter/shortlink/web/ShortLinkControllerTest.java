package com.claudej.adapter.shortlink.web;

import com.claudej.adapter.common.GlobalExceptionHandler;
import com.claudej.application.shortlink.dto.ShortLinkDTO;
import com.claudej.application.shortlink.service.ShortLinkApplicationService;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {ShortLinkController.class, GlobalExceptionHandler.class})
class ShortLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShortLinkApplicationService shortLinkApplicationService;

    @Test
    void should_return200WithShortLink_when_validRequest() throws Exception {
        ShortLinkDTO dto = new ShortLinkDTO();
        dto.setShortCode("a1b2c3");
        dto.setOriginalUrl("https://www.example.com");
        when(shortLinkApplicationService.createShortLink(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/short-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"https://www.example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shortCode").value("a1b2c3"))
                .andExpect(jsonPath("$.data.originalUrl").value("https://www.example.com"))
                .andExpect(jsonPath("$.data.shortUrl").isNotEmpty());
    }

    @Test
    void should_return400_when_originalUrlIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/short-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void should_return400_when_originalUrlIsInvalid() throws Exception {
        when(shortLinkApplicationService.createShortLink(any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_ORIGINAL_URL));

        mockMvc.perform(post("/api/v1/short-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"not-a-url\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_ORIGINAL_URL"));
    }
}
