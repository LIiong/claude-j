package com.claudej.application.link.service;

import com.claudej.application.link.assembler.LinkAssembler;
import com.claudej.application.link.command.CreateLinkCommand;
import com.claudej.application.link.command.DeleteLinkCommand;
import com.claudej.application.link.command.UpdateLinkCommand;
import com.claudej.application.link.dto.LinkDTO;
import com.claudej.domain.common.exception.BusinessException;
import com.claudej.domain.common.exception.ErrorCode;
import com.claudej.domain.link.model.aggregate.Link;
import com.claudej.domain.link.model.valobj.LinkCategory;
import com.claudej.domain.link.model.valobj.LinkName;
import com.claudej.domain.link.model.valobj.LinkUrl;
import com.claudej.domain.link.repository.LinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 链接应用服务
 */
@Service
public class LinkApplicationService {

    private final LinkRepository linkRepository;
    private final LinkAssembler linkAssembler;

    public LinkApplicationService(LinkRepository linkRepository, LinkAssembler linkAssembler) {
        this.linkRepository = linkRepository;
        this.linkAssembler = linkAssembler;
    }

    /**
     * 创建链接
     */
    @Transactional
    public LinkDTO createLink(CreateLinkCommand command) {
        LinkName name = new LinkName(command.getName());
        LinkUrl url = new LinkUrl(command.getUrl());
        LinkCategory category = command.getCategory() != null
                ? new LinkCategory(command.getCategory())
                : null;

        Link link = Link.create(name, url, command.getDescription(), category);
        link = linkRepository.save(link);

        return linkAssembler.toDTO(link);
    }

    /**
     * 更新链接
     */
    @Transactional
    public LinkDTO updateLink(UpdateLinkCommand command) {
        Link link = linkRepository.findById(command.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LINK_NOT_FOUND));

        LinkName newName = command.getName() != null ? new LinkName(command.getName()) : null;
        LinkUrl newUrl = command.getUrl() != null ? new LinkUrl(command.getUrl()) : null;
        LinkCategory newCategory = command.getCategory() != null
                ? new LinkCategory(command.getCategory())
                : null;

        link.update(newName, newUrl, command.getDescription(), newCategory);
        link = linkRepository.save(link);

        return linkAssembler.toDTO(link);
    }

    /**
     * 删除链接
     */
    @Transactional
    public void deleteLink(DeleteLinkCommand command) {
        if (!linkRepository.existsById(command.getId())) {
            throw new BusinessException(ErrorCode.LINK_NOT_FOUND);
        }
        linkRepository.deleteById(command.getId());
    }

    /**
     * 根据 ID 查询链接
     */
    public LinkDTO getLinkById(Long id) {
        Link link = linkRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.LINK_NOT_FOUND));
        return linkAssembler.toDTO(link);
    }

    /**
     * 查询所有链接
     */
    public List<LinkDTO> getAllLinks() {
        List<Link> links = linkRepository.findAll();
        return linkAssembler.toDTOList(links);
    }

    /**
     * 根据分类查询链接
     */
    public List<LinkDTO> getLinksByCategory(String category) {
        LinkCategory linkCategory = new LinkCategory(category);
        List<Link> links = linkRepository.findByCategory(linkCategory);
        return linkAssembler.toDTOList(links);
    }
}
