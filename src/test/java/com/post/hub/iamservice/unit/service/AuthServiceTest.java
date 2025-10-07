package com.post.hub.iamservice.unit.service;

import com.post.hub.iamservice.mapper.UserMapper;
import com.post.hub.iamservice.model.dto.user.UserProfileDTO;
import com.post.hub.iamservice.model.entities.RefreshToken;
import com.post.hub.iamservice.model.entities.Role;
import com.post.hub.iamservice.model.entities.User;
import com.post.hub.iamservice.model.enums.RegistrationStatus;
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
import com.post.hub.iamservice.service.RefreshTokenService;
import com.post.hub.iamservice.service.impl.AuthServiceImpl;
import com.post.hub.iamservice.service.model.IamServiceUserRole;
import com.post.hub.iamservice.utils.ApiUtils;
import com.post.hub.iamservice.utils.PasswordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@Tag("unit")
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private AccessValidator accessValidator;

    @Mock
    private ApiUtils apiUtils;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private UserProfileDTO testUserProfileDTO;
    private RefreshToken testRefreshToken;
    private Role userRole;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("TestUser");
        testUser.setEmail("test@gmail.com");
        testUser.setPassword("hashedPassword");
        testUser.setRegistrationStatus(RegistrationStatus.ACTIVE);
        testUser.setLastLogin(LocalDateTime.now());

        userRole = new Role();
        userRole.setId(10);
        userRole.setName("USER");
        testUser.setRoles(Collections.singleton(userRole));

        testRefreshToken = new RefreshToken();
        testRefreshToken.setToken("refresh_token_123");
        testRefreshToken.setUser(testUser);

        testUserProfileDTO = new UserProfileDTO(
                testUser.getId(),
                testUser.getUsername(),
                testUser.getEmail(),
                testUser.getRegistrationStatus(),
                testUser.getLastLogin(),
                "access_token_123",
                testRefreshToken.getToken(),
                Collections.emptyList()
        );
    }

    @Test
    void login_ValidCredentials_ReturnsUserProfile() {
        LoginRequest request = new LoginRequest("test@gmail.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findUserByEmailAndDeletedFalse(request.getEmail())).thenReturn(Optional.of(testUser));
        when(refreshTokenService.generateOrUpdateRefreshToken(testUser)).thenReturn(testRefreshToken);
        when(jwtTokenProvider.generateToken(testUser)).thenReturn("access_token_123");
        when(userMapper.toUserProfileDTO(testUser, "access_token_123", testRefreshToken.getToken()))
                .thenReturn(testUserProfileDTO);

        IamResponse<UserProfileDTO> result = authService.login(request);

        assertNotNull(result);
        assertEquals("access_token_123", result.getPayload().getToken());
        assertEquals("refresh_token_123", result.getPayload().getRefreshToken());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findUserByEmailAndDeletedFalse(request.getEmail());
        verify(refreshTokenService).generateOrUpdateRefreshToken(testUser);
        verify(jwtTokenProvider).generateToken(testUser);
        verify(userMapper).toUserProfileDTO(testUser, "access_token_123", testRefreshToken.getToken());
    }

    @Test
    void login_InvalidCredentials_ThrowsException() {
        LoginRequest request = new LoginRequest("test@gmail.com", "wrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        InvalidDataException exception = assertThrows(InvalidDataException.class, () -> authService.login(request));

        assertTrue(exception.getMessage().contains("Invalid"));

        verify(userRepository, never()).findUserByEmailAndDeletedFalse(request.getEmail());
        verify(refreshTokenService, never()).generateOrUpdateRefreshToken(any(User.class));
        verify(jwtTokenProvider, never()).generateToken(any(User.class));
    }


    @Test
    void login_UserNotFoundAfterAuth_ThrowsInvalidData() {
        LoginRequest request = new LoginRequest("missing@gmail.com", "ok");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findUserByEmailAndDeletedFalse(request.getEmail())).thenReturn(Optional.empty());

        assertThrows(InvalidDataException.class, () -> authService.login(request));

        verify(refreshTokenService, never()).generateOrUpdateRefreshToken(any());
        verify(jwtTokenProvider, never()).generateToken(any());
        verify(userMapper, never()).toUserProfileDTO(any(), anyString(), anyString());
    }

    @Test
    void refreshAccessToken_Valid_ReturnsNewAccessToken() {
        RefreshToken rt = new RefreshToken();
        rt.setToken("refresh_token_123");
        rt.setUser(testUser);

        when(refreshTokenService.validateAndRefreshToken("refresh_token_123")).thenReturn(rt);
        when(jwtTokenProvider.generateToken(testUser)).thenReturn("new_access_456");
        when(userMapper.toUserProfileDTO(testUser, "new_access_456", "refresh_token_123"))
                .thenReturn(new UserProfileDTO(
                        testUser.getId(), testUser.getUsername(), testUser.getEmail(),
                        testUser.getRegistrationStatus(), testUser.getLastLogin(),
                        "new_access_456", "refresh_token_123", Collections.emptyList()
                ));

        IamResponse<UserProfileDTO> result = authService.refreshAccessToken("refresh_token_123");

        assertNotNull(result);
        assertEquals("new_access_456", result.getPayload().getToken());
        assertEquals("refresh_token_123", result.getPayload().getRefreshToken());

        verify(refreshTokenService).validateAndRefreshToken("refresh_token_123");
        verify(jwtTokenProvider).generateToken(testUser);
        verify(userMapper).toUserProfileDTO(testUser, "new_access_456", "refresh_token_123");
    }

    @Test
    void registerUser_ValidRequest_CreatesUserSuccessfully() {
        RegistrationUserRequest request = new RegistrationUserRequest(
                "newUser",
                "newuser@gmail.com",
                "password123!",
                "password123!"
        );

        doNothing().when(accessValidator).validateNewUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getConfirmPassword()
        );

        when(roleRepository.findByName(IamServiceUserRole.USER.getRole())).thenReturn(Optional.of(userRole));
        when(userMapper.fromDto(request)).thenReturn(testUser);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(refreshTokenService.generateOrUpdateRefreshToken(testUser)).thenReturn(testRefreshToken);
        when(jwtTokenProvider.generateToken(testUser)).thenReturn("access_token_123");
        when(userMapper.toUserProfileDTO(testUser, "access_token_123", testRefreshToken.getToken()))
                .thenReturn(testUserProfileDTO);

        IamResponse<UserProfileDTO> result = authService.registerUser(request);

        assertNotNull(result);
        assertEquals("access_token_123", result.getPayload().getToken());
        assertEquals("refresh_token_123", result.getPayload().getRefreshToken());

        verify(accessValidator).validateNewUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getConfirmPassword()
        );
        verify(roleRepository).findByName(IamServiceUserRole.USER.getRole());
        verify(userRepository).save(any(User.class));
        verify(refreshTokenService).generateOrUpdateRefreshToken(testUser);
        verify(jwtTokenProvider).generateToken(testUser);
        verify(userMapper).toUserProfileDTO(testUser, "access_token_123", testRefreshToken.getToken());
    }

    @Test
    void registerUser_RoleMissing_ThrowsNotFound() {
        RegistrationUserRequest request = new RegistrationUserRequest("newUser", "newuser@gmail.com", "p", "p");

        doNothing().when(accessValidator).validateNewUser(anyString(), anyString(), anyString(), anyString());
        when(roleRepository.findByName(IamServiceUserRole.USER.getRole())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> authService.registerUser(request));

        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).generateOrUpdateRefreshToken(any());
    }

    @Test
    void registerUser_ValidationFails_ThrowsInvalidData() {
        RegistrationUserRequest request = new RegistrationUserRequest("bad", "bad@mail", "x", "y");

        doThrow(new InvalidDataException("validation fail")).when(accessValidator).validateNewUser(
                anyString(), anyString(), anyString(), anyString()
        );

        assertThrows(InvalidDataException.class, () -> authService.registerUser(request));

        verify(roleRepository, never()).findByName(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_HappyPath_EncodesAndSaves() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setPassword("StrongPass1!");
        req.setConfirmPassword("StrongPass1!");

        when(apiUtils.getUserIdFromAuthentication()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("StrongPass1!")).thenReturn("hashedNew");

        try (MockedStatic<PasswordUtils> mock = Mockito.mockStatic(PasswordUtils.class)) {
            mock.when(() -> PasswordUtils.isNotValidPassword("StrongPass1!")).thenReturn(false);

            IamResponse<String> result = authService.changePassword(req);

            assertNotNull(result);
            assertTrue(result.isSuccess());
            verify(userRepository).save(testUser);
            assertEquals("hashedNew", testUser.getPassword());
        }
    }

    @Test
    void changePassword_InvalidPassword_ThrowsInvalidPasswordException() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setPassword("weak");
        req.setConfirmPassword("weak");

        when(apiUtils.getUserIdFromAuthentication()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        try (MockedStatic<PasswordUtils> mock = Mockito.mockStatic(PasswordUtils.class)) {
            mock.when(() -> PasswordUtils.isNotValidPassword("weak")).thenReturn(true);

            assertThrows(InvalidPasswordException.class, () -> authService.changePassword(req));

            verify(userRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    @Test
    void changePassword_UserNotFound_ThrowsNotFound() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setPassword("StrongPass1!");
        req.setConfirmPassword("StrongPass1!");

        when(apiUtils.getUserIdFromAuthentication()).thenReturn(99);
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> authService.changePassword(req));
        verify(passwordEncoder, never()).encode(anyString());
    }
}
