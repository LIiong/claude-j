package com.claudej.application.link.service;

import com.claudej.application.common.assembler.PageAssembler;
import com.claudej.application.common.dto.PageDTO;
import com.claudej.application.link.assembler.LinkAssembler;
import com.claudej.application.link.command.CreateLinkCommand;
import com.claudej.application.link.command.DeleteLinkCommand;
import com.claudej.application.link.command.UpdateLinkCommand;
import com.claudej.application.link.dto.LinkDTO;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.model.valobj.PageRequest;
import com.claudej.domain.common.model.valobj.Page;
import com.claudej.domain.common.model.valobj.SortDirection;
import com.claudej.domain.link.model.aggregate.Link;
import com.claudej.domain.link.repository.LinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkApplicationServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private LinkAssembler linkAssembler;

    @Mock
    private PageAssembler pageAssembler;

    @InjectMocks
    private LinkApplicationService linkApplicationService;

    private CreateLinkCommand createCommand;
    private UpdateLinkCommand updateCommand;
    private Link mockLink;
    private LinkDTO mockLinkDTO;

    @BeforeEach
    void setUp() {
        createCommand = new CreateLinkCommand();
        createCommand.setName("Test Link");
        createCommand.setUrl("https://example.com");
        createCommand.setDescription("Test Description");
        createCommand.setCategory("tech");

        updateCommand = new UpdateLinkCommand();
        updateCommand.setId(1L);
        updateCommand.setName("Updated Link");
        updateCommand.setUrl("https://updated.com");

        mockLink = Link.create(
                new com.claudej.domain.link.model.valobj.LinkName("Test Link"),
                new com.claudej.domain.link.model.valobj.LinkUrl("https://example.com"),
                "Test Description",
                new com.claudej.domain.link.model.valobj.LinkCategory("tech")
        );
        mockLink.setId(1L);

        mockLinkDTO = new LinkDTO();
        mockLinkDTO.setId(1L);
        mockLinkDTO.setName("Test Link");
        mockLinkDTO.setUrl("https://example.com");
    }

    @Test
    void should_createLink_when_validCommandProvided() {
        // Given
        when(linkRepository.save(any(Link.class))).thenReturn(mockLink);
        when(linkAssembler.toDTO(any(Link.class))).thenReturn(mockLinkDTO);

        // When
        LinkDTO result = linkApplicationService.createLink(createCommand);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(linkRepository).save(any(Link.class));
    }

    @Test
    void should_updateLink_when_linkExists() {
        // Given
        when(linkRepository.findById(1L)).thenReturn(Optional.of(mockLink));
        when(linkRepository.save(any(Link.class))).thenReturn(mockLink);
        when(linkAssembler.toDTO(any(Link.class))).thenReturn(mockLinkDTO);

        // When
        LinkDTO result = linkApplicationService.updateLink(updateCommand);

        // Then
        assertThat(result).isNotNull();
        verify(linkRepository).findById(1L);
        verify(linkRepository).save(any(Link.class));
    }

    @Test
    void should_throwException_when_updatingNonExistentLink() {
        // Given
        when(linkRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> linkApplicationService.updateLink(updateCommand))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("链接不存在");
    }

    @Test
    void should_deleteLink_when_linkExists() {
        // Given
        DeleteLinkCommand deleteCommand = new DeleteLinkCommand();
        deleteCommand.setId(1L);
        when(linkRepository.existsById(1L)).thenReturn(true);
        doNothing().when(linkRepository).deleteById(1L);

        // When
        linkApplicationService.deleteLink(deleteCommand);

        // Then
        verify(linkRepository).existsById(1L);
        verify(linkRepository).deleteById(1L);
    }

    @Test
    void should_throwException_when_deletingNonExistentLink() {
        // Given
        DeleteLinkCommand deleteCommand = new DeleteLinkCommand();
        deleteCommand.setId(1L);
        when(linkRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> linkApplicationService.deleteLink(deleteCommand))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("链接不存在");
        verify(linkRepository, never()).deleteById(any());
    }

    @Test
    void should_returnLink_when_getLinkByIdExists() {
        // Given
        when(linkRepository.findById(1L)).thenReturn(Optional.of(mockLink));
        when(linkAssembler.toDTO(any(Link.class))).thenReturn(mockLinkDTO);

        // When
        LinkDTO result = linkApplicationService.getLinkById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void should_throwException_when_getLinkByIdNotExists() {
        // Given
        when(linkRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> linkApplicationService.getLinkById(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("链接不存在");
    }

    @Test
    void should_returnAllLinks_when_getAllLinks() {
        // Given
        Link link2 = Link.create(
                new com.claudej.domain.link.model.valobj.LinkName("Link 2"),
                new com.claudej.domain.link.model.valobj.LinkUrl("https://link2.com"),
                null,
                null
        );
        List<Link> links = Arrays.asList(mockLink, link2);
        when(linkRepository.findAll()).thenReturn(links);

        LinkDTO dto2 = new LinkDTO();
        dto2.setId(2L);
        dto2.setName("Link 2");
        when(linkAssembler.toDTOList(links)).thenReturn(Arrays.asList(mockLinkDTO, dto2));

        // When
        List<LinkDTO> result = linkApplicationService.getAllLinks();

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    void should_returnLinksByCategory_when_getLinksByCategory() {
        // Given
        when(linkRepository.findByCategory(any())).thenReturn(Arrays.asList(mockLink));
        when(linkAssembler.toDTOList(any())).thenReturn(Arrays.asList(mockLinkDTO));

        // When
        List<LinkDTO> result = linkApplicationService.getLinksByCategory("tech");

        // Then
        assertThat(result).hasSize(1);
        verify(linkRepository).findByCategory(any());
    }

    // ========== 分页方法测试 ==========

    @Test
    void should_returnPagedResult_when_getAllLinksPaged() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 20, "createTime", SortDirection.DESC);
        Page<Link> mockPage = new Page<Link>(
                Arrays.asList(mockLink),
                1L,
                0,
                20
        );

        when(linkRepository.findAll(any(PageRequest.class))).thenReturn(mockPage);

        // 使用 doAnswer 绕过泛型检查
        doAnswer(invocation -> {
            PageDTO<LinkDTO> result = new PageDTO<LinkDTO>();
            result.setContent(Arrays.asList(mockLinkDTO));
            result.setTotalElements(1L);
            result.setTotalPages(1);
            result.setPage(0);
            result.setSize(20);
            result.setFirst(true);
            result.setLast(true);
            result.setEmpty(false);
            return result;
        }).when(pageAssembler).toPageDTO(eq(mockPage), any());

        // When
        PageDTO<LinkDTO> result = linkApplicationService.getAllLinks(pageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);
        verify(linkRepository).findAll(any(PageRequest.class));
    }

    @Test
    void should_returnPagedResultByCategory_when_getLinksByCategoryPaged() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 20, null, null);
        Page<Link> mockPage = new Page<Link>(
                Arrays.asList(mockLink),
                1L,
                0,
                20
        );

        when(linkRepository.findByCategory(any(), any(PageRequest.class))).thenReturn(mockPage);

        // 使用 doAnswer 绕过泛型检查
        doAnswer(invocation -> {
            PageDTO<LinkDTO> result = new PageDTO<LinkDTO>();
            result.setContent(Arrays.asList(mockLinkDTO));
            result.setTotalElements(1L);
            result.setTotalPages(1);
            result.setPage(0);
            result.setSize(20);
            result.setFirst(true);
            result.setLast(true);
            result.setEmpty(false);
            return result;
        }).when(pageAssembler).toPageDTO(eq(mockPage), any());

        // When
        PageDTO<LinkDTO> result = linkApplicationService.getLinksByCategory("tech", pageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);
        verify(linkRepository).findByCategory(any(), any(PageRequest.class));
    }
}
