package com.post.hub.iamservice.service.impl;

import com.post.hub.iamservice.kafka.service.KafkaMessageService;
import com.post.hub.iamservice.mapper.UserMapper;
import com.post.hub.iamservice.model.constants.ApiErrorMessage;
import com.post.hub.iamservice.model.dto.user.UserDTO;
import com.post.hub.iamservice.model.dto.user.UserSearchDTO;
import com.post.hub.iamservice.model.entities.Role;
import com.post.hub.iamservice.model.entities.User;
import com.post.hub.iamservice.security.validation.AccessValidator;
import com.post.hub.iamservice.service.model.IamServiceUserRole;
import com.post.hub.iamservice.model.exception.DataExistException;
import com.post.hub.iamservice.model.exception.NotFoundException;
import com.post.hub.iamservice.model.request.user.NewUserRequest;
import com.post.hub.iamservice.model.request.user.UpdateUserRequest;
import com.post.hub.iamservice.model.request.user.UserSearchRequest;
import com.post.hub.iamservice.model.response.IamResponse;
import com.post.hub.iamservice.model.response.PaginationResponse;
import com.post.hub.iamservice.repository.RoleRepository;
import com.post.hub.iamservice.repository.UserRepository;
import com.post.hub.iamservice.repository.criteria.UserSearchCriteria;
import com.post.hub.iamservice.service.UserService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final AccessValidator accessValidator;
    private final KafkaMessageService kafkaMessageService;

    @Override
    @Transactional
    public IamResponse<UserDTO> createUser(@NotNull NewUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DataExistException(ApiErrorMessage.USERNAME_ALREADY_EXISTS.getMessage(request.getUsername()));
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DataExistException(ApiErrorMessage.EMAIL_ALREADY_EXISTS.getMessage(request.getEmail()));
        }

        String targetRole = IamServiceUserRole.USER.getRole();
        Role userRole = roleRepository.findByName(targetRole)
                .orElseThrow(() -> new NotFoundException(ApiErrorMessage.USER_NOT_FOUND_BY_ID.getMessage(targetRole)));

        User user = userMapper.createUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        UserDTO userDTO = userMapper.toDTO(savedUser);

        kafkaMessageService.sendUserCreatedMessage(user.getId(), user.getUsername());

        return IamResponse.createSuccessful(userDTO);
    }

    @Override
    @Transactional
    public IamResponse<UserDTO> updateUser(@NotNull Integer userId, @NotNull UpdateUserRequest request) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new NotFoundException(ApiErrorMessage.USER_NOT_FOUND_BY_ID.getMessage(userId)));

        accessValidator.validateAdminOrOwnerAccess(userId);

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DataExistException(ApiErrorMessage.USERNAME_ALREADY_EXISTS.getMessage(request.getUsername()));
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DataExistException(ApiErrorMessage.EMAIL_ALREADY_EXISTS.getMessage(request.getEmail()));
        }

        userMapper.updateUser(user, request);
        user.setUpdated(LocalDateTime.now());
        user = userRepository.save(user);

        UserDTO userDTO = userMapper.toDTO(user);

        kafkaMessageService.sendUserUpdatedMessage(user.getId(), user.getUsername());

        return IamResponse.createSuccessful(userDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public IamResponse<UserDTO> getById(@NotNull Integer userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new NotFoundException(ApiErrorMessage.USER_NOT_FOUND_BY_ID.getMessage(userId)));

        UserDTO userDto = userMapper.toDTO(user);
        return IamResponse.createSuccessful(userDto);
    }

    @Override
    @Transactional(readOnly = true)
    public IamResponse<PaginationResponse<UserSearchDTO>> findAllUsers(Pageable pageable) {
        Page<UserSearchDTO> users = userRepository.findAll(pageable)
                .map(userMapper::toUserSearchDto);

        PaginationResponse<UserSearchDTO> paginationResponse = new PaginationResponse<>(
                users.getContent(),
                new PaginationResponse.Pagination(
                        users.getTotalElements(),
                        pageable.getPageSize(),
                        users.getNumber() + 1,
                        users.getTotalPages()
                )
        );

        return IamResponse.createSuccessful(paginationResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public IamResponse<PaginationResponse<UserSearchDTO>> searchUsers(UserSearchRequest request, Pageable pageable) {
        Specification<User> specification = new UserSearchCriteria(request);

        Page<UserSearchDTO> usersPage = userRepository.findAll(specification, pageable)
                .map(userMapper::toUserSearchDto);

        PaginationResponse<UserSearchDTO> response = PaginationResponse.<UserSearchDTO>builder()
                .content(usersPage.getContent())
                .pagination(
                        PaginationResponse.Pagination.builder()
                                .total(usersPage.getTotalElements())
                                .limit(pageable.getPageSize())
                                .page(usersPage.getNumber() + 1)
                                .pages(usersPage.getTotalPages())
                                .build()
                )
                .build();

        return IamResponse.createSuccessful(response);
    }

    @Override
    @Transactional
    public void softDeleteUser(Integer userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new NotFoundException(ApiErrorMessage.USER_NOT_FOUND_BY_ID.getMessage(userId)));

        accessValidator.validateAdminOrOwnerAccess(userId);

        user.setDeleted(true);
        userRepository.save(user);

        kafkaMessageService.sendUserDeletedMessage(userId, user.getUsername());
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return getUserDetails(email, userRepository);
    }

    static UserDetails getUserDetails(String email, UserRepository userRepository) {
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new NotFoundException(ApiErrorMessage.EMAIL_NOT_FOUND.getMessage(email)));

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList())
        );
    }

}
