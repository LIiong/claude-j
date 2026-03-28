package com.claudej.application.shortlink.assembler;

import com.claudej.application.shortlink.dto.ShortLinkDTO;
import com.claudej.domain.shortlink.model.aggregate.ShortLink;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShortLinkAssembler {

    @Mapping(target = "shortCode", expression = "java(shortLink.getShortCode() != null ? shortLink.getShortCode().getValue() : null)")
    @Mapping(target = "originalUrl", expression = "java(shortLink.getOriginalUrlValue())")
    ShortLinkDTO toDTO(ShortLink shortLink);
}
