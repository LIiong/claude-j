package com.claudej.application.auth.assembler;

import com.claudej.application.auth.dto.LoginLogDTO;
import com.claudej.domain.auth.model.entity.LoginLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 登录日志Assembler
 */
@Mapper(componentModel = "spring")
public interface LoginLogAssembler {

    LoginLogAssembler INSTANCE = Mappers.getMapper(LoginLogAssembler.class);

    @Mapping(source = "userIdValue", target = "userId")
    @Mapping(source = "loginType", target = "loginType")
    @Mapping(target = "deviceType", expression = "java(loginLog.getDeviceInfo() != null ? loginLog.getDeviceInfo().getDeviceType() : null)")
    LoginLogDTO toDTO(LoginLog loginLog);
}
