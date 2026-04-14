package com.claudej.infrastructure.link.persistence.converter;

import com.claudej.domain.link.model.aggregate.Link;
import com.claudej.domain.link.model.valobj.LinkCategory;
import com.claudej.domain.link.model.valobj.LinkName;
import com.claudej.domain.link.model.valobj.LinkUrl;
import com.claudej.infrastructure.link.persistence.dataobject.LinkDO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Link 转换器
 */
@Component
public class LinkConverter {

    /**
     * DO 转 Domain
     */
    public Link toDomain(LinkDO linkDO) {
        if (linkDO == null) {
            return null;
        }
        return Link.reconstruct(
                linkDO.getId(),
                new LinkName(linkDO.getName()),
                new LinkUrl(linkDO.getUrl()),
                linkDO.getDescription(),
                linkDO.getCategory() != null ? new LinkCategory(linkDO.getCategory()) : null,
                linkDO.getCreateTime(),
                linkDO.getUpdateTime()
        );
    }

    /**
     * Domain 转 DO
     */
    public LinkDO toDO(Link link) {
        if (link == null) {
            return null;
        }
        LinkDO linkDO = new LinkDO();
        linkDO.setId(link.getId());
        linkDO.setName(link.getNameValue());
        linkDO.setUrl(link.getUrlValue());
        linkDO.setDescription(link.getDescription());
        linkDO.setCategory(link.getCategoryValue());
        linkDO.setCreateTime(link.getCreateTime());
        linkDO.setUpdateTime(link.getUpdateTime());
        linkDO.setDeleted(0);
        return linkDO;
    }

    /**
     * DO 列表转 Domain 列表
     */
    public List<Link> toDomainList(List<LinkDO> linkDOList) {
        if (linkDOList == null) {
            return null;
        }
        return linkDOList.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
}
