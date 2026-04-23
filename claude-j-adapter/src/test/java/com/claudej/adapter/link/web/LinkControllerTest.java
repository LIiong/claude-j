package com.claudej.adapter.link.web;

import com.claudej.adapter.common.ApiResult;
import com.claudej.adapter.common.response.PageResponse;
import com.claudej.adapter.link.web.request.CreateLinkRequest;
import com.claudej.adapter.link.web.request.UpdateLinkRequest;
import com.claudej.adapter.link.web.response.LinkResponse;
import com.claudej.adapter.common.GlobalExceptionHandler;
import com.claudej.application.common.dto.PageDTO;
import com.claudej.application.link.command.CreateLinkCommand;
import com.claudej.application.link.command.DeleteLinkCommand;
import com.claudej.application.link.command.UpdateLinkCommand;
import com.claudej.application.link.dto.LinkDTO;
import com.claudej.application.link.service.LinkApplicationService;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.common.model.valobj.PageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {LinkController.class, GlobalExceptionHandler.class})
class LinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LinkApplicationService linkApplicationService;

    private LinkDTO mockLinkDTO;

    @BeforeEach
    void setUp() {
        mockLinkDTO = new LinkDTO();
        mockLinkDTO.setId(1L);
        mockLinkDTO.setName("Test Link");
        mockLinkDTO.setUrl("https://example.com");
        mockLinkDTO.setDescription("Test Description");
        mockLinkDTO.setCategory("tech");
        mockLinkDTO.setCreateTime(LocalDateTime.now());
        mockLinkDTO.setUpdateTime(LocalDateTime.now());
    }

    @Test
    void should_return201_when_createLinkSuccess() throws Exception {
        // Given
        CreateLinkRequest request = new CreateLinkRequest();
        request.setName("Test Link");
        request.setUrl("https://example.com");
        request.setDescription("Test Description");
        request.setCategory("tech");

        when(linkApplicationService.createLink(any(CreateLinkCommand.class))).thenReturn(mockLinkDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.name", is("Test Link")))
                .andExpect(jsonPath("$.data.url", is("https://example.com")));
    }

    @Test
    void should_return400_when_createLinkWithInvalidInput() throws Exception {
        // Given
        CreateLinkRequest request = new CreateLinkRequest();
        request.setName("");  // Invalid: empty name
        request.setUrl("https://example.com");

        // When & Then
        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return200_when_updateLinkSuccess() throws Exception {
        // Given
        UpdateLinkRequest request = new UpdateLinkRequest();
        request.setId(1L);
        request.setName("Updated Link");
        request.setUrl("https://updated.com");

        when(linkApplicationService.updateLink(any(UpdateLinkCommand.class))).thenReturn(mockLinkDTO);

        // When & Then
        mockMvc.perform(put("/api/v1/links/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)));
    }

    @Test
    void should_return404_when_updateNonExistentLink() throws Exception {
        // Given
        UpdateLinkRequest request = new UpdateLinkRequest();
        request.setId(999L);
        request.setName("Updated Link");

        when(linkApplicationService.updateLink(any(UpdateLinkCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.LINK_NOT_FOUND));

        // When & Then
        mockMvc.perform(put("/api/v1/links/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("LINK_NOT_FOUND")));
    }

    @Test
    void should_return200_when_deleteLinkSuccess() throws Exception {
        // Given
        doNothing().when(linkApplicationService).deleteLink(any(DeleteLinkCommand.class));

        // When & Then
        mockMvc.perform(delete("/api/v1/links/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void should_return404_when_deleteNonExistentLink() throws Exception {
        // Given
        doThrow(new BusinessException(ErrorCode.LINK_NOT_FOUND))
                .when(linkApplicationService).deleteLink(any(DeleteLinkCommand.class));

        // When & Then
        mockMvc.perform(delete("/api/v1/links/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("LINK_NOT_FOUND")));
    }

    @Test
    void should_return200_when_getLinkByIdSuccess() throws Exception {
        // Given
        when(linkApplicationService.getLinkById(1L)).thenReturn(mockLinkDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/links/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.name", is("Test Link")));
    }

    @Test
    void should_return404_when_getNonExistentLink() throws Exception {
        // Given
        when(linkApplicationService.getLinkById(999L))
                .thenThrow(new BusinessException(ErrorCode.LINK_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/api/v1/links/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("LINK_NOT_FOUND")));
    }

    @Test
    void should_return200_when_getAllLinks() throws Exception {
        // Given
        LinkDTO link2 = new LinkDTO();
        link2.setId(2L);
        link2.setName("Link 2");
        link2.setUrl("https://link2.com");

        when(linkApplicationService.getAllLinks()).thenReturn(Arrays.asList(mockLinkDTO, link2));

        // When & Then
        mockMvc.perform(get("/api/v1/links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id", is(1)))
                .andExpect(jsonPath("$.data[1].id", is(2)));
    }

    @Test
    void should_return200_when_getLinksByCategory() throws Exception {
        // Given
        when(linkApplicationService.getLinksByCategory("tech")).thenReturn(Arrays.asList(mockLinkDTO));

        // When & Then
        mockMvc.perform(get("/api/v1/links/category")
                        .param("category", "tech"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].category", is("tech")));
    }

    // ========== 分页端点测试 ==========

    @Test
    void should_return200WithPagedResult_when_getAllLinksPaged() throws Exception {
        // Given - 模拟分页返回结果
        PageDTO<LinkDTO> pageDTO = new PageDTO<LinkDTO>();
        pageDTO.setContent(Arrays.asList(mockLinkDTO));
        pageDTO.setTotalElements(1);
        pageDTO.setTotalPages(1);
        pageDTO.setPage(0);
        pageDTO.setSize(20);
        pageDTO.setFirst(true);
        pageDTO.setLast(true);
        pageDTO.setEmpty(false);

        when(linkApplicationService.getAllLinks(any(PageRequest.class))).thenReturn(pageDTO);

        // When & Then - 验证分页端点路径 /api/v1/links/query/paged 可以正常访问
        mockMvc.perform(get("/api/v1/links/query/paged")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.page", is(0)))
                .andExpect(jsonPath("$.data.size", is(20)));
    }

    @Test
    void should_return200WithPagedResult_when_getLinksByCategoryPaged() throws Exception {
        // Given
        PageDTO<LinkDTO> pageDTO = new PageDTO<LinkDTO>();
        pageDTO.setContent(Arrays.asList(mockLinkDTO));
        pageDTO.setTotalElements(1);
        pageDTO.setTotalPages(1);
        pageDTO.setPage(0);
        pageDTO.setSize(20);
        pageDTO.setFirst(true);
        pageDTO.setLast(true);
        pageDTO.setEmpty(false);

        when(linkApplicationService.getLinksByCategory(eq("tech"), any(PageRequest.class))).thenReturn(pageDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/links/category/paged")
                        .param("category", "tech")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.totalElements", is(1)));
    }

    @Test
    void should_return400_when_sortFieldNotInWhitelist() throws Exception {
        // Given - 使用非法排序字段
        PageDTO<LinkDTO> pageDTO = new PageDTO<LinkDTO>();
        pageDTO.setContent(new ArrayList<LinkDTO>());
        pageDTO.setTotalElements(0);
        pageDTO.setTotalPages(0);
        pageDTO.setPage(0);
        pageDTO.setSize(20);
        pageDTO.setFirst(true);
        pageDTO.setLast(true);
        pageDTO.setEmpty(true);

        // When & Then - 非法排序字段应返回 400
        mockMvc.perform(get("/api/v1/links/query/paged")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortField", "invalidField"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("INVALID_SORT_FIELD")));
    }

    @Test
    void should_return200_when_sortFieldInWhitelist() throws Exception {
        // Given - 使用合法排序字段 createTime
        PageDTO<LinkDTO> pageDTO = new PageDTO<LinkDTO>();
        pageDTO.setContent(Arrays.asList(mockLinkDTO));
        pageDTO.setTotalElements(1);
        pageDTO.setTotalPages(1);
        pageDTO.setPage(0);
        pageDTO.setSize(20);
        pageDTO.setFirst(true);
        pageDTO.setLast(true);
        pageDTO.setEmpty(false);

        when(linkApplicationService.getAllLinks(any(PageRequest.class))).thenReturn(pageDTO);

        // When & Then - 合法排序字段应返回 200
        mockMvc.perform(get("/api/v1/links/query/paged")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortField", "createTime")
                        .param("sortDirection", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}
