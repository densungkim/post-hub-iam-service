package com.post.hub.iamservice.model.dto.user;

import com.post.hub.iamservice.model.dto.role.RoleDTO;
import com.post.hub.iamservice.model.enums.RegistrationStatus;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserSearchDTO implements Serializable {

    private Integer id;
    private String username;
    private String email;
    private RegistrationStatus registrationStatus;
    private LocalDateTime created;
    private Boolean isDeleted;

    private List<RoleDTO> roles;

}