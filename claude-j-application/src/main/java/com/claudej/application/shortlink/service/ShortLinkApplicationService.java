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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ShortLinkApplicationService {

    private final ShortLinkRepository shortLinkRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final ShortLinkAssembler shortLinkAssembler;

    public ShortLinkApplicationService(ShortLinkRepository shortLinkRepository,
                                        ShortCodeGenerator shortCodeGenerator,
                                        ShortLinkAssembler shortLinkAssembler) {
        this.shortLinkRepository = shortLinkRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.shortLinkAssembler = shortLinkAssembler;
    }

    @Transactional
    public ShortLinkDTO createShortLink(CreateShortLinkCommand command) {
        OriginalUrl originalUrl = new OriginalUrl(command.getOriginalUrl());

        // 去重：相同 URL 返回已有短链
        Optional<ShortLink> existing = shortLinkRepository.findByOriginalUrl(originalUrl);
        if (existing.isPresent()) {
            return shortLinkAssembler.toDTO(existing.get());
        }

        // 创建新短链
        ShortLink shortLink = ShortLink.create(originalUrl);
        shortLink = shortLinkRepository.save(shortLink);

        // 基于 ID 生成短码并分配
        ShortCode shortCode = shortCodeGenerator.generate(shortLink.getId());
        shortLink.assignShortCode(shortCode);
        shortLink = shortLinkRepository.save(shortLink);

        return shortLinkAssembler.toDTO(shortLink);
    }

    public ShortLinkDTO resolveShortLink(String shortCodeValue) {
        ShortCode shortCode = new ShortCode(shortCodeValue);
        ShortLink shortLink = shortLinkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHORT_LINK_NOT_FOUND));

        if (shortLink.isExpired()) {
            throw new BusinessException(ErrorCode.SHORT_LINK_EXPIRED);
        }

        return shortLinkAssembler.toDTO(shortLink);
    }
}
