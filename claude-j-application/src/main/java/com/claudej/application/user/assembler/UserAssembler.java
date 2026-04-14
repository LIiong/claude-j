package com.claudej.application.user.assembler;

import com.claudej.application.user.dto.UserDTO;
import com.claudej.domain.user.model.aggregate.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * User 转换器
 */
@Mapper(componentModel = "spring")
public interface UserAssembler {

    /**
     * Domain 转 DTO
     */
    @Mapping(target = "userId", expression = "java(user.getUserIdValue())")
    @Mapping(target = "username", expression = "java(user.getUsername().getValue())")
    @Mapping(target = "email", expression = "java(user.getEmail() != null ? user.getEmail().getValue() : null)")
    @Mapping(target = "phone", expression = "java(user.getPhone() != null ? user.getPhone().getValue() : null)")
    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    @Mapping(target = "inviteCode", expression = "java(user.getInviteCodeValue())")
    @Mapping(target = "inviterId", expression = "java(user.getInviterIdValue())")
    UserDTO toDTO(User user);

    /**
     * Domain 列表转 DTO 列表
     */
    List<UserDTO> toDTOList(List<User> users);
}
