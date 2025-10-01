package com.post.hub.iamservice.service;

import com.post.hub.iamservice.model.dto.user.UserProfileDTO;
import com.post.hub.iamservice.model.request.user.ChangePasswordRequest;
import com.post.hub.iamservice.model.request.user.LoginRequest;
import com.post.hub.iamservice.model.request.user.RegistrationUserRequest;
import com.post.hub.iamservice.model.response.IamResponse;

public interface AuthService {

    IamResponse<UserProfileDTO> login(LoginRequest request);

    IamResponse<UserProfileDTO> refreshAccessToken(String refreshToken);

    IamResponse<UserProfileDTO> registerUser(RegistrationUserRequest request);

    IamResponse<String> changePassword(ChangePasswordRequest request);

}
