package com.claudej.application.link.assembler;

import com.claudej.application.link.dto.LinkDTO;
import com.claudej.domain.link.model.aggregate.Link;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Link 转换器
 */
@Mapper(componentModel = "spring")
public interface LinkAssembler {

    /**
     * Domain 转 DTO
     */
    @Mapping(target = "name", expression = "java(link.getNameValue())")
    @Mapping(target = "url", expression = "java(link.getUrlValue())")
    @Mapping(target = "category", expression = "java(link.getCategoryValue())")
    LinkDTO toDTO(Link link);

    /**
     * Domain 列表转 DTO 列表
     */
    List<LinkDTO> toDTOList(List<Link> links);
}
