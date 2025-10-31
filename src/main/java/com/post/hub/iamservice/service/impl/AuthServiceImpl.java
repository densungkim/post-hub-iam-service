package com.post.hub.iamservice.service.impl;

import com.post.hub.iamservice.mapper.UserMapper;
import com.post.hub.iamservice.model.constants.ApiErrorMessage;
import com.post.hub.iamservice.model.constants.ApiLogMessage;
import com.post.hub.iamservice.model.dto.user.UserProfileDTO;
import com.post.hub.iamservice.model.entities.RefreshToken;
import com.post.hub.iamservice.model.entities.Role;
import com.post.hub.iamservice.model.entities.User;
import com.post.hub.iamservice.model.exception.InvalidDataException;
import com.post.hub.iamservice.model.exception.InvalidPasswordException;
import com.post.hub.iamservice.model.exception.NotFoundException;
import com.post.hub.iamservice.model.request.user.ChangePasswordRequest;
import com.post.hub.iamservice.model.request.user.LoginRequest;
import com.post.hub.iamservice.model.request.user.RegistrationUserRequest;
import com.post.hub.iamservice.model.response.IamResponse;
import com.post.hub.iamservice.repository.RoleRepository;
import com.post.hub.iamservice.repository.UserRepository;
import com.post.hub.iamservice.security.JwtTokenProvider;
import com.post.hub.iamservice.security.validation.AccessValidator;
import com.post.hub.iamservice.service.AuthService;
import com.post.hub.iamservice.service.RefreshTokenService;
import com.post.hub.iamservice.service.model.IamServiceUserRole;
import com.post.hub.iamservice.utils.ApiUtils;
import com.post.hub.iamservice.utils.PasswordUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessValidator accessValidator;
    private final ApiUtils apiUtils;

    @Override
    @Transactional
    public IamResponse<UserProfileDTO> login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new InvalidDataException(ApiErrorMessage.INVALID_USER_OR_PASSWORD.getMessage());
        }

        User user = userRepository.findUserByEmailAndDeletedFalse(request.getEmail())
                .orElseThrow(() -> new InvalidDataException(ApiErrorMessage.INVALID_USER_OR_PASSWORD.getMessage()));

        RefreshToken refreshToken = refreshTokenService.generateOrUpdateRefreshToken(user);
        String token = jwtTokenProvider.generateToken(user);
        UserProfileDTO userProfileDTO = userMapper.toUserProfileDTO(user, token, refreshToken.getToken());
        userProfileDTO.setToken(token);

        return IamResponse.createSuccessfulWithNewToken(userProfileDTO);
    }

    @Override
    @Transactional
    public IamResponse<UserProfileDTO> refreshAccessToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenService.validateAndRefreshToken(refreshTokenValue);
        User user = refreshToken.getUser();

        String accessToken = jwtTokenProvider.generateToken(user);

        return IamResponse.createSuccessfulWithNewToken(
                userMapper.toUserProfileDTO(user, accessToken, refreshToken.getToken())
        );
    }


    @Override
    @Transactional
    public IamResponse<UserProfileDTO> registerUser(RegistrationUserRequest request) {
        accessValidator.validateNewUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getConfirmPassword()
        );

        Role userRole = roleRepository.findByName(IamServiceUserRole.USER.getRole())
                .orElseThrow(() -> new NotFoundException(ApiErrorMessage.USER_ROLE_NOT_FOUND.getMessage()));

        User newUser = userMapper.fromDto(request);
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        newUser.setRoles(roles);
        userRepository.save(newUser);

        RefreshToken refreshToken = refreshTokenService.generateOrUpdateRefreshToken(newUser);
        String token = jwtTokenProvider.generateToken(newUser);
        UserProfileDTO userProfileDTO = userMapper.toUserProfileDTO(newUser, token, refreshToken.getToken());
        userProfileDTO.setToken(token);

        return IamResponse.createSuccessfulWithNewToken(userProfileDTO);
    }

    @Override
    @Transactional
    public IamResponse<String> changePassword(ChangePasswordRequest request) {
        Integer userId = apiUtils.getUserIdFromAuthentication();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ApiErrorMessage.USER_NOT_FOUND_BY_ID.getMessage(userId)));

        if (PasswordUtils.isNotValidPassword(request.getPassword())) {
            throw new InvalidPasswordException(ApiErrorMessage.INVALID_PASSWORD.getMessage());
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        return IamResponse.createSuccessful(ApiLogMessage.PASSWORD_CHANGED_SUCCESSFULLY.getValue());
    }

}
