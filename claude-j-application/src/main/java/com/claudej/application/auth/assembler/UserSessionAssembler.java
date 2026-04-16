package com.claudej.application.auth.assembler;

import com.claudej.application.auth.dto.UserSessionDTO;
import com.claudej.domain.auth.model.entity.UserSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 用户会话Assembler
 */
@Mapper(componentModel = "spring")
public interface UserSessionAssembler {

    UserSessionAssembler INSTANCE = Mappers.getMapper(UserSessionAssembler.class);

    @Mapping(source = "sessionIdValue", target = "sessionId")
    @Mapping(source = "userIdValue", target = "userId")
    @Mapping(source = "deviceInfo.deviceType", target = "deviceType")
    @Mapping(source = "deviceInfo.os", target = "os")
    @Mapping(source = "deviceInfo.browser", target = "browser")
    UserSessionDTO toDTO(UserSession session);
}
