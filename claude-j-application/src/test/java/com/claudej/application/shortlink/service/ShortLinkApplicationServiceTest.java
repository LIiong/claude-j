package com.claudej.application.shortlink.service;

import com.claudej.application.shortlink.assembler.ShortLinkAssembler;
import com.claudej.application.shortlink.command.CreateShortLinkCommand;
import com.claudej.application.shortlink.dto.ShortLinkDTO;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.shortlink.model.aggregate.ShortLink;
import com.claudej.domain.shortlink.model.valobj.OriginalUrl;
import com.claudej.domain.shortlink.model.valobj.ShortCode;
import com.claudej.domain.shortlink.repository.ShortLinkRepository;
import com.claudej.domain.shortlink.service.ShortCodeGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortLinkApplicationServiceTest {

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @Mock
    private ShortLinkAssembler shortLinkAssembler;

    @InjectMocks
    private ShortLinkApplicationService service;

    @Test
    void should_createNewShortLink_when_urlNotExists() {
        // Arrange
        CreateShortLinkCommand command = new CreateShortLinkCommand();
        command.setOriginalUrl("https://www.example.com");

        ShortLink savedLink = ShortLink.create(new OriginalUrl("https://www.example.com"));
        savedLink.setId(1L);

        ShortCode generatedCode = new ShortCode("a1b2c3");

        when(shortLinkRepository.findByOriginalUrl(any())).thenReturn(Optional.empty());
        when(shortLinkRepository.save(any())).thenReturn(savedLink);
        when(shortCodeGenerator.generate(1L)).thenReturn(generatedCode);

        ShortLinkDTO expectedDTO = new ShortLinkDTO();
        expectedDTO.setShortCode("a1b2c3");
        expectedDTO.setOriginalUrl("https://www.example.com");
        when(shortLinkAssembler.toDTO(any())).thenReturn(expectedDTO);

        // Act
        ShortLinkDTO result = service.createShortLink(command);

        // Assert
        assertThat(result.getShortCode()).isEqualTo("a1b2c3");
        assertThat(result.getOriginalUrl()).isEqualTo("https://www.example.com");
        verify(shortLinkRepository, times(2)).save(any());
        verify(shortCodeGenerator).generate(1L);
    }

    @Test
    void should_returnExisting_when_urlAlreadyExists() {
        // Arrange
        CreateShortLinkCommand command = new CreateShortLinkCommand();
        command.setOriginalUrl("https://www.example.com");

        ShortLink existingLink = ShortLink.reconstruct(
                1L, new ShortCode("abc123"),
                new OriginalUrl("https://www.example.com"),
                LocalDateTime.now(), null
        );

        when(shortLinkRepository.findByOriginalUrl(any())).thenReturn(Optional.of(existingLink));

        ShortLinkDTO expectedDTO = new ShortLinkDTO();
        expectedDTO.setShortCode("abc123");
        expectedDTO.setOriginalUrl("https://www.example.com");
        when(shortLinkAssembler.toDTO(existingLink)).thenReturn(expectedDTO);

        // Act
        ShortLinkDTO result = service.createShortLink(command);

        // Assert
        assertThat(result.getShortCode()).isEqualTo("abc123");
        verify(shortLinkRepository, never()).save(any());
        verify(shortCodeGenerator, never()).generate(any());
    }

    @Test
    void should_resolveShortLink_when_validShortCode() {
        // Arrange
        ShortLink shortLink = ShortLink.reconstruct(
                1L, new ShortCode("abc123"),
                new OriginalUrl("https://www.example.com"),
                LocalDateTime.now(), null
        );

        when(shortLinkRepository.findByShortCode(any())).thenReturn(Optional.of(shortLink));

        ShortLinkDTO expectedDTO = new ShortLinkDTO();
        expectedDTO.setShortCode("abc123");
        expectedDTO.setOriginalUrl("https://www.example.com");
        when(shortLinkAssembler.toDTO(shortLink)).thenReturn(expectedDTO);

        // Act
        ShortLinkDTO result = service.resolveShortLink("abc123");

        // Assert
        assertThat(result.getOriginalUrl()).isEqualTo("https://www.example.com");
    }

    @Test
    void should_throwNotFound_when_shortCodeDoesNotExist() {
        when(shortLinkRepository.findByShortCode(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveShortLink("abc123"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SHORT_LINK_NOT_FOUND));
    }

    @Test
    void should_throwExpired_when_shortLinkIsExpired() {
        ShortLink expiredLink = ShortLink.reconstruct(
                1L, new ShortCode("abc123"),
                new OriginalUrl("https://www.example.com"),
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1)
        );

        when(shortLinkRepository.findByShortCode(any())).thenReturn(Optional.of(expiredLink));

        assertThatThrownBy(() -> service.resolveShortLink("abc123"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SHORT_LINK_EXPIRED));
    }
}
