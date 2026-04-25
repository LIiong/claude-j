package com.claudej.application.user.assembler;

import com.claudej.application.user.dto.UserDTO;
import com.claudej.domain.user.model.aggregate.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    @Mapping(target = "roles", expression = "java(convertRolesToStrings(user.getRoles()))")
    UserDTO toDTO(User user);

    /**
     * Domain 列表转 DTO 列表
     */
    List<UserDTO> toDTOList(List<User> users);

    /**
     * 将 Role 集合转换为字符串集合
     */
    default Set<String> convertRolesToStrings(Set<com.claudej.domain.user.model.valobj.Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        return roles.stream()
                .map(role -> role.name())
                .collect(Collectors.toSet());
    }
}
