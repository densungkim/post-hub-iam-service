package com.post.hub.iamservice.service;

import com.post.hub.iamservice.kafka.service.KafkaMessageService;
import com.post.hub.iamservice.mapper.UserMapper;
import com.post.hub.iamservice.model.dto.user.UserDTO;
import com.post.hub.iamservice.model.entities.Role;
import com.post.hub.iamservice.model.entities.User;
import com.post.hub.iamservice.model.exception.DataExistException;
import com.post.hub.iamservice.model.exception.NotFoundException;
import com.post.hub.iamservice.model.request.user.NewUserRequest;
import com.post.hub.iamservice.repository.RoleRepository;
import com.post.hub.iamservice.repository.UserRepository;
import com.post.hub.iamservice.service.impl.UserServiceImpl;
import com.post.hub.iamservice.service.model.IamServiceUserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private KafkaMessageService kafkaMessageService;

    private User testUser;
    private UserDTO testUserDTO;
    private Role superAdminRole;

    @BeforeEach
    void setUp() {
        superAdminRole = new Role();
        superAdminRole.setName(IamServiceUserRole.SUPER_ADMIN.getRole());

        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("TestUser");
        testUser.setEmail("testuser@gmail.com");
        testUser.setPassword("encodedPassword");
        testUser.setRoles(Set.of(superAdminRole));

        testUserDTO = new UserDTO();
        testUserDTO.setId(1);
        testUserDTO.setUsername("TestUser");
        testUserDTO.setEmail("testuser@gmail.com");
    }

    @Test
    void getById_UserExists_ReturnsUserDTO() {
        when(userRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testUser));
        when(userMapper.toDTO(testUser)).thenReturn(testUserDTO);

        UserDTO result = userService.getById(1).getPayload();

        assertNotNull(result);
        assertEquals(testUserDTO.getId(), result.getId());
        assertEquals(testUserDTO.getUsername(), result.getUsername());

        verify(userRepository, times(1)).findByIdAndDeletedFalse(1);
        verify(userMapper, times(1)).toDTO(testUser);
    }

    @Test
    void getById_UserNotFound_ThrowsException() {
        when(userRepository.findByIdAndDeletedFalse(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(999))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("not found");

        verify(userRepository, times(1)).findByIdAndDeletedFalse(999);
        verify(userMapper, times(0)).toDTO(testUser);
    }

    @Test
    void createUser_AsSuperAdmin_CreatesUserSuccessfully() {
        NewUserRequest request = new NewUserRequest("NewUser", "password123!", "newuser@gmail.com");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(roleRepository.findByName(IamServiceUserRole.USER.getRole())).thenReturn(Optional.of(superAdminRole));

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPassword("encodedPassword");
        newUser.setRoles(Collections.singleton(superAdminRole));

        when(userMapper.createUser(request)).thenReturn(newUser);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(userMapper.toDTO(newUser)).thenReturn(testUserDTO);

        UserDTO result = userService.createUser(request).getPayload();

        assertNotNull(request);
        assertEquals(testUserDTO.getId(), result.getId());
        assertEquals(testUserDTO.getUsername(), result.getUsername());

        verify(userRepository, times(1)).existsByEmail(request.getEmail());
        verify(userRepository, times(1)).existsByUsername(request.getUsername());
        verify(userRepository, times(1)).save(any(User.class));
        verify(userMapper, times(1)).toDTO(newUser);
        verify(kafkaMessageService, times(1)).sendUserCreatedMessage(newUser.getId(), newUser.getUsername());
    }

    @Test
    void createUser_EmailAlreadyExists_ThrowsException() {
        NewUserRequest request = new NewUserRequest("NewUser", "password123!", "newuser@gmail.com");

        when(userRepository.existsByUsername(request.getUsername())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DataExistException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, times(1)).existsByUsername(request.getUsername());
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}
