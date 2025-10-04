package com.post.hub.iamservice.service;

import com.post.hub.iamservice.kafka.service.KafkaMessageService;
import com.post.hub.iamservice.mapper.UserMapper;
import com.post.hub.iamservice.model.dto.user.UserDTO;
import com.post.hub.iamservice.model.dto.user.UserSearchDTO;
import com.post.hub.iamservice.model.entities.Role;
import com.post.hub.iamservice.model.entities.User;
import com.post.hub.iamservice.model.exception.DataExistException;
import com.post.hub.iamservice.model.exception.NotFoundException;
import com.post.hub.iamservice.model.request.user.NewUserRequest;
import com.post.hub.iamservice.model.request.user.UpdateUserRequest;
import com.post.hub.iamservice.model.request.user.UserSearchRequest;
import com.post.hub.iamservice.model.response.IamResponse;
import com.post.hub.iamservice.model.response.PaginationResponse;
import com.post.hub.iamservice.repository.RoleRepository;
import com.post.hub.iamservice.repository.UserRepository;
import com.post.hub.iamservice.security.validation.AccessValidator;
import com.post.hub.iamservice.service.impl.UserServiceImpl;
import com.post.hub.iamservice.service.model.IamServiceUserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private AccessValidator accessValidator;

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
        superAdminRole.setId(100);
        superAdminRole.setName(IamServiceUserRole.SUPER_ADMIN.getRole());

        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("TestUser");
        testUser.setEmail("testuser@gmail.com");
        testUser.setPassword("encodedPassword");
        testUser.setRoles(Set.of(superAdminRole));
        testUser.setLastLogin(LocalDateTime.now());

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

        verify(userRepository).findByIdAndDeletedFalse(1);
        verify(userMapper).toDTO(testUser);
    }

    @Test
    void getById_UserNotFound_ThrowsException() {
        when(userRepository.findByIdAndDeletedFalse(999)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getById(999));

        verify(userMapper, never()).toDTO(testUser);
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

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository).existsByUsername(request.getUsername());
        verify(userRepository).save(any(User.class));
        verify(userMapper).toDTO(newUser);
        verify(userMapper).createUser(request);
        verify(kafkaMessageService).sendUserCreatedMessage(newUser.getId(), newUser.getUsername());
    }

    @Test
    void createUser_UsernameAlreadyExists_ThrowsException() {
        NewUserRequest request = new NewUserRequest("NewUser", "password", "mail@mail.com");
        when(userRepository.existsByUsername("NewUser")).thenReturn(true);

        assertThrows(DataExistException.class, () -> userService.createUser(request));

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_EmailAlreadyExists_ThrowsException() {
        NewUserRequest request = new NewUserRequest("NewUser", "password", "mail@mail.com");
        when(userRepository.existsByUsername("NewUser")).thenReturn(false);
        when(userRepository.existsByEmail("mail@mail.com")).thenReturn(true);

        assertThrows(DataExistException.class, () -> userService.createUser(request));

        verify(userRepository).existsByUsername(request.getUsername());
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_RoleNotFound_ThrowsNotFound() {
        NewUserRequest request = new NewUserRequest("NewUser", "password", "mail@mail.com");
        when(userRepository.existsByUsername("NewUser")).thenReturn(false);
        when(userRepository.existsByEmail("mail@mail.com")).thenReturn(false);
        when(roleRepository.findByName(IamServiceUserRole.USER.getRole())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.createUser(request));

        verify(userRepository, never()).save(any());
        verify(kafkaMessageService, never()).sendUserCreatedMessage(any(), anyString());
    }

    @Test
    void updateUser_OK() {
        UpdateUserRequest req = new UpdateUserRequest("updName", "upd@mail.com");

        when(userRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testUser));
        doNothing().when(accessValidator).validateAdminOrOwnerAccess(1);
        when(userRepository.existsByUsername("updName")).thenReturn(false);
        when(userRepository.existsByEmail("upd@mail.com")).thenReturn(false);

        doAnswer(inv -> {
            User u = inv.getArgument(0);
            UpdateUserRequest r = inv.getArgument(1);
            u.setUsername(r.getUsername());
            u.setEmail(r.getEmail());
            return null;
        }).when(userMapper).updateUser(any(User.class), any(UpdateUserRequest.class));

        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO dto = new UserDTO();
        dto.setId(1);
        dto.setUsername("updName");
        dto.setEmail("upd@mail.com");
        when(userMapper.toDTO(any(User.class))).thenReturn(dto);

        IamResponse<UserDTO> resp = userService.updateUser(1, req);

        assertTrue(resp.isSuccess());
        assertEquals("updName", resp.getPayload().getUsername());

        verify(userRepository).findByIdAndDeletedFalse(1);
        verify(accessValidator).validateAdminOrOwnerAccess(1);
        verify(userRepository).existsByUsername("updName");
        verify(userRepository).existsByEmail("upd@mail.com");
        verify(userRepository).save(any(User.class));
        verify(kafkaMessageService).sendUserUpdatedMessage(1, "updName");
    }

    @Test
    void updateUser_NotFound_Throws() {
        when(userRepository.findByIdAndDeletedFalse(9)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.updateUser(9, new UpdateUserRequest("a", "b")));
        verify(userRepository, never()).save(any());
        verify(kafkaMessageService, never()).sendUserUpdatedMessage(anyInt(), anyString());
    }

    @Test
    void updateUser_UsernameExists_Throws() {
        when(userRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testUser));
        doNothing().when(accessValidator).validateAdminOrOwnerAccess(1);
        when(userRepository.existsByUsername("dup")).thenReturn(true);

        assertThrows(DataExistException.class, () -> userService.updateUser(1, new UpdateUserRequest("dup", "x")));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_EmailExists_Throws() {
        when(userRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testUser));
        doNothing().when(accessValidator).validateAdminOrOwnerAccess(1);
        when(userRepository.existsByUsername("ok")).thenReturn(false);
        when(userRepository.existsByEmail("dup@mail.com")).thenReturn(true);

        assertThrows(DataExistException.class, () -> userService.updateUser(1, new UpdateUserRequest("ok", "dup@mail.com")));

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_AccessDenied_Propagates() {
        when(userRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testUser));
        doThrow(new AccessDeniedException("nope")).when(accessValidator).validateAdminOrOwnerAccess(1);

        assertThrows(AccessDeniedException.class, () -> userService.updateUser(1, new UpdateUserRequest("a", "b")));

        verify(userRepository, never()).save(any());
        verify(kafkaMessageService, never()).sendUserUpdatedMessage(anyInt(), anyString());
    }

    @Test
    void softDeleteUser_OK_setsDeletedTrue_andSendsEvent() {
        when(userRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testUser));
        doNothing().when(accessValidator).validateAdminOrOwnerAccess(1);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.softDeleteUser(1);

        assertTrue(testUser.getDeleted());
        verify(userRepository).save(testUser);
        verify(kafkaMessageService).sendUserDeletedMessage(1, "TestUser");
    }

    @Test
    void softDeleteUser_NotFound_Throws() {
        when(userRepository.findByIdAndDeletedFalse(99)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.softDeleteUser(99));
        verify(userRepository, never()).save(any());
        verify(kafkaMessageService, never()).sendUserDeletedMessage(anyInt(), anyString());
    }

    @Test
    void softDeleteUser_AccessDenied_Propagates() {
        when(userRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testUser));
        doThrow(new AccessDeniedException("nope")).when(accessValidator).validateAdminOrOwnerAccess(1);

        assertThrows(AccessDeniedException.class, () -> userService.softDeleteUser(1));

        verify(userRepository, never()).save(any());
        verify(kafkaMessageService, never()).sendUserDeletedMessage(anyInt(), anyString());
    }

    @Test
    void findAllUsers_mapsList_andPagination() {
        User u1 = new User();
        u1.setId(11);
        User u2 = new User();
        u2.setId(12);
        Page<User> page = new PageImpl<>(List.of(u1, u2), PageRequest.of(0, 2), 5);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        UserSearchDTO d1 = new UserSearchDTO();
        d1.setId(11);
        UserSearchDTO d2 = new UserSearchDTO();
        d2.setId(12);
        when(userMapper.toUserSearchDto(u1)).thenReturn(d1);
        when(userMapper.toUserSearchDto(u2)).thenReturn(d2);

        IamResponse<PaginationResponse<UserSearchDTO>> resp =
                userService.findAllUsers(PageRequest.of(0, 2));

        assertTrue(resp.isSuccess());
        assertEquals(2, resp.getPayload().getContent().size());
        assertEquals(5, resp.getPayload().getPagination().getTotal());
        assertEquals(2, resp.getPayload().getPagination().getLimit());
        assertEquals(1, resp.getPayload().getPagination().getPage());

        verify(userRepository).findAll(any(Pageable.class));
    }

    @Test
    void searchUsers_usesSpecification_andMapsDTOs() {
        UserSearchRequest req = new UserSearchRequest();
        Pageable pageable = PageRequest.of(1, 3);

        User u1 = new User();
        u1.setId(21);
        User u2 = new User();
        u2.setId(22);
        Page<User> page = new PageImpl<>(List.of(u1, u2), pageable, 7);
        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        UserSearchDTO d1 = new UserSearchDTO();
        d1.setId(21);
        UserSearchDTO d2 = new UserSearchDTO();
        d2.setId(22);
        when(userMapper.toUserSearchDto(u1)).thenReturn(d1);
        when(userMapper.toUserSearchDto(u2)).thenReturn(d2);

        IamResponse<PaginationResponse<UserSearchDTO>> resp =
                userService.searchUsers(req, pageable);

        assertTrue(resp.isSuccess());
        assertEquals(2, resp.getPayload().getContent().size());
        assertEquals(7, resp.getPayload().getPagination().getTotal());
        assertEquals(3, resp.getPayload().getPagination().getLimit());
        assertEquals(2, resp.getPayload().getPagination().getPage());

        verify(userRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void loadUserByUsername_updatesLastLogin_andMapsAuthorities() {
        Role admin = new Role();
        admin.setName("ADMIN");
        User dbUser = new User();
        dbUser.setId(5);
        dbUser.setEmail("admin@mail.com");
        dbUser.setPassword("ENC");
        dbUser.setRoles(Set.of(admin, superAdminRole));

        when(userRepository.findUserByEmail("admin@mail.com")).thenReturn(Optional.of(dbUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDetails ud = userService.loadUserByUsername("admin@mail.com");

        assertEquals("admin@mail.com", ud.getUsername());
        assertEquals("ENC", ud.getPassword());
        assertThat(ud.getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("ADMIN", "USER");

        verify(userRepository).save(dbUser);
        assertNotNull(dbUser.getLastLogin());
    }
}
