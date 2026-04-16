package com.claudej.application.auth.assembler;

import com.claudej.application.auth.dto.AuthUserDTO;
import com.claudej.domain.auth.model.aggregate.AuthUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 认证用户Assembler
 */
@Mapper(componentModel = "spring")
public interface AuthUserAssembler {

    AuthUserAssembler INSTANCE = Mappers.getMapper(AuthUserAssembler.class);

    @Mapping(source = "userIdValue", target = "userId")
    @Mapping(target = "status", expression = "java(authUser.isActive() ? \"ACTIVE\" : authUser.getStatus().name())")
    AuthUserDTO toDTO(AuthUser authUser);
}
