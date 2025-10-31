package com.post.hub.iamservice.service;

import com.post.hub.iamservice.model.dto.user.UserDTO;
import com.post.hub.iamservice.model.dto.user.UserSearchDTO;
import com.post.hub.iamservice.model.request.user.NewUserRequest;
import com.post.hub.iamservice.model.request.user.UpdateUserRequest;
import com.post.hub.iamservice.model.request.user.UserSearchRequest;
import com.post.hub.iamservice.model.response.IamResponse;
import com.post.hub.iamservice.model.response.PaginationResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {

    IamResponse<UserDTO> createUser(NewUserRequest request);

    IamResponse<UserDTO> updateUser(Integer postId, UpdateUserRequest request);

    IamResponse<UserDTO> getById(Integer userId);

    IamResponse<PaginationResponse<UserSearchDTO>> findAllUsers(Pageable pageable);

    IamResponse<PaginationResponse<UserSearchDTO>> searchUsers(UserSearchRequest request, Pageable pageable);

    void softDeleteUser(Integer userId);

}
