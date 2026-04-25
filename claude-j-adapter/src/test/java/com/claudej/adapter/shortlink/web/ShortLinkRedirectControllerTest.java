package com.claudej.adapter.shortlink.web;

import com.claudej.adapter.common.GlobalExceptionHandler;
import com.claudej.application.shortlink.dto.ShortLinkDTO;
import com.claudej.application.shortlink.service.ShortLinkApplicationService;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {ShortLinkRedirectController.class, GlobalExceptionHandler.class})
class ShortLinkRedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShortLinkApplicationService shortLinkApplicationService;

    @Test
    void should_return302_when_shortCodeExists() throws Exception {
        ShortLinkDTO dto = new ShortLinkDTO();
        dto.setShortCode("abc123");
        dto.setOriginalUrl("https://www.example.com/long-path");

        when(shortLinkApplicationService.resolveShortLink("abc123")).thenReturn(dto);

        mockMvc.perform(get("/s/abc123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://www.example.com/long-path"));
    }

    @Test
    void should_return404_when_shortCodeNotFound() throws Exception {
        when(shortLinkApplicationService.resolveShortLink("zzzzzz"))
                .thenThrow(new BusinessException(ErrorCode.SHORT_LINK_NOT_FOUND));

        mockMvc.perform(get("/s/zzzzzz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("SHORT_LINK_NOT_FOUND"));
    }
}
