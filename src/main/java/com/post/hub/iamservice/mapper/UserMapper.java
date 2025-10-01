package com.post.hub.iamservice.mapper;

import com.post.hub.iamservice.model.dto.role.RoleDTO;
import com.post.hub.iamservice.model.dto.user.UserDTO;
import com.post.hub.iamservice.model.dto.user.UserProfileDTO;
import com.post.hub.iamservice.model.dto.user.UserSearchDTO;
import com.post.hub.iamservice.model.entities.Role;
import com.post.hub.iamservice.model.entities.User;
import com.post.hub.iamservice.model.enums.RegistrationStatus;
import com.post.hub.iamservice.model.request.user.NewUserRequest;
import com.post.hub.iamservice.model.request.user.RegistrationUserRequest;
import com.post.hub.iamservice.model.request.user.UpdateUserRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Collection;
import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        imports = {RegistrationStatus.class, Object.class}
)
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(mapRoles(user.getRoles()))")
    UserDTO toDTO(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "registrationStatus", expression = "java(RegistrationStatus.ACTIVE)")
    User createUser(NewUserRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created", ignore = true)
    void updateUser(@MappingTarget User user, UpdateUserRequest request);

    @Mapping(source = "deleted", target = "isDeleted")
    @Mapping(target = "roles", expression = "java(mapRoles(user.getRoles()))")
    UserSearchDTO toUserSearchDto(User user);

    @Mapping(target = "roles", expression = "java(mapRoles(user.getRoles()))")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "token", source = "token")
    @Mapping(target = "refreshToken", source = "refreshToken")
    UserProfileDTO toUserProfileDTO(User user, String token, String refreshToken);

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "registrationStatus", expression = "java(RegistrationStatus.ACTIVE)")
    User fromDto(RegistrationUserRequest request);

    default List<RoleDTO> mapRoles(Collection<Role> roles) {
        return roles
                .stream()
                .map(role -> new RoleDTO(role.getId(), role.getName()))
                .toList();
    }

}
