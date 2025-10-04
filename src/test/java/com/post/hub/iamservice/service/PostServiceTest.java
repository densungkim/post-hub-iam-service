package com.post.hub.iamservice.service;

import com.post.hub.iamservice.kafka.service.KafkaMessageService;
import com.post.hub.iamservice.mapper.PostMapper;
import com.post.hub.iamservice.model.dto.post.PostDTO;
import com.post.hub.iamservice.model.dto.post.PostSearchDTO;
import com.post.hub.iamservice.model.entities.Post;
import com.post.hub.iamservice.model.entities.User;
import com.post.hub.iamservice.model.exception.DataExistException;
import com.post.hub.iamservice.model.exception.NotFoundException;
import com.post.hub.iamservice.model.request.post.NewPostRequest;
import com.post.hub.iamservice.model.request.post.PostSearchRequest;
import com.post.hub.iamservice.model.request.post.UpdatePostRequest;
import com.post.hub.iamservice.model.response.IamResponse;
import com.post.hub.iamservice.model.response.PaginationResponse;
import com.post.hub.iamservice.repository.PostRepository;
import com.post.hub.iamservice.repository.UserRepository;
import com.post.hub.iamservice.security.validation.AccessValidator;
import com.post.hub.iamservice.service.impl.PostServiceImpl;
import com.post.hub.iamservice.utils.ApiUtils;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostMapper postMapper;

    @Mock
    private ApiUtils apiUtils;

    @Mock
    private KafkaMessageService kafkaMessageService;

    @Mock
    private AccessValidator accessValidator;

    @InjectMocks
    private PostServiceImpl postService;

    private Post testPost;
    private PostDTO testPostDTO;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("TestUser");
        testUser.setLastLogin(LocalDateTime.now());

        testPost = new Post();
        testPost.setId(1);
        testPost.setTitle("Test Post");
        testPost.setContent("Test Content");
        testPost.setUser(testUser);

        testPostDTO = new PostDTO();
        testPostDTO.setId(1);
        testPostDTO.setTitle("Test Post");
        testPostDTO.setContent("Test Content");
    }

    @Test
    void getById_PostExists_ReturnsDTO() {
        when(postRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testPost));
        when(postMapper.toDTO(testPost)).thenReturn(testPostDTO);

        PostDTO dto = postService.getById(1).getPayload();

        assertNotNull(dto);
        assertEquals(1, dto.getId());
        assertEquals("Test Post", dto.getTitle());
        verify(postRepository).findByIdAndDeletedFalse(1);
        verify(postMapper).toDTO(testPost);
    }

    @Test
    void getById_NotFound_Throws() {
        when(postRepository.findByIdAndDeletedFalse(999)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> postService.getById(999));
        verify(postMapper, never()).toDTO(any());
    }

    @Test
    void createPost_OK() {
        NewPostRequest req = new NewPostRequest("New Title", "New Content", 100);

        when(postRepository.existsByTitle("New Title")).thenReturn(false);
        when(apiUtils.getUserIdFromAuthentication()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(postMapper.createPost(req, testUser, testUser.getUsername())).thenReturn(testPost);
        when(postRepository.save(any(Post.class))).thenReturn(testPost);
        when(postMapper.toDTO(testPost)).thenReturn(testPostDTO);

        PostDTO dto = postService.createPost(req).getPayload();

        assertNotNull(dto);
        assertEquals(1, dto.getId());
        verify(postRepository).existsByTitle("New Title");
        verify(userRepository).findById(1);
        verify(postRepository).save(any(Post.class));
        verify(kafkaMessageService).sendPostCreatedMessage(1, 1);
    }

    @Test
    void createPost_TitleExists_ThrowsDataExist() {
        NewPostRequest req = new NewPostRequest("New Title", "New Content", 100);
        when(postRepository.existsByTitle("New Title")).thenReturn(true);

        assertThrows(DataExistException.class, () -> postService.createPost(req));

        verify(userRepository, never()).findById(anyInt());
        verify(postRepository, never()).save(any());
        verify(kafkaMessageService, never()).sendPostCreatedMessage(anyInt(), anyInt());
    }

    @Test
    void createPost_UserNotFound_ThrowsUsernameNotFound() {
        NewPostRequest req = new NewPostRequest("T", "C", 0);
        when(postRepository.existsByTitle("T")).thenReturn(false);
        when(apiUtils.getUserIdFromAuthentication()).thenReturn(77);
        when(userRepository.findById(77)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> postService.createPost(req));

        verify(postRepository, never()).save(any());
        verify(kafkaMessageService, never()).sendPostCreatedMessage(anyInt(), anyInt());
    }

    @Test
    void updatePost_OK() {
        UpdatePostRequest req = new UpdatePostRequest("Upd", "Body", 5);

        when(postRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testPost));
        doNothing().when(accessValidator).validateAdminOrOwnerAccess(testUser.getId());
        when(postRepository.existsByTitle("Upd")).thenReturn(false);
        doNothing().when(postMapper).updatePost(testPost, req);
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        PostDTO updated = new PostDTO();
        updated.setId(1);
        updated.setTitle("Upd");
        updated.setContent("Body");
        when(postMapper.toDTO(testPost)).thenReturn(updated);

        IamResponse<PostDTO> resp = postService.updatePost(1, req);

        assertTrue(resp.isSuccess());
        assertEquals("Upd", resp.getPayload().getTitle());
        verify(postRepository).findByIdAndDeletedFalse(1);
        verify(accessValidator).validateAdminOrOwnerAccess(testPost.getUser().getId());
        verify(postRepository).existsByTitle("Upd");
        verify(postMapper).updatePost(testPost, req);
        verify(postRepository).save(testPost);
        verify(kafkaMessageService).sendPostUpdatedMessage(1, 1);
    }

    @Test
    void updatePost_NotFound_Throws() {
        UpdatePostRequest req = new UpdatePostRequest("X", "Y", 1);
        when(postRepository.findByIdAndDeletedFalse(9)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> postService.updatePost(9, req));

        verify(accessValidator, never()).validateAdminOrOwnerAccess(anyInt());
        verify(postRepository, never()).save(any());
        verify(kafkaMessageService, never()).sendPostUpdatedMessage(anyInt(), anyInt());
    }

    @Test
    void updatePost_TitleDuplicate_ThrowsDataExist() {
        UpdatePostRequest req = new UpdatePostRequest("Dup", "Y", 1);
        when(postRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testPost));
        doNothing().when(accessValidator).validateAdminOrOwnerAccess(testUser.getId());
        when(postRepository.existsByTitle("Dup")).thenReturn(true);

        assertThrows(DataExistException.class, () -> postService.updatePost(1, req));

        verify(postRepository, never()).save(any());
        verify(kafkaMessageService, never()).sendPostUpdatedMessage(anyInt(), anyInt());
    }

    @Test
    void updatePost_AccessDenied_Propagates() {
        UpdatePostRequest req = new UpdatePostRequest("Upd", "B", 1);
        when(postRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testPost));
        doThrow(new AccessDeniedException("nope"))
                .when(accessValidator).validateAdminOrOwnerAccess(testUser.getId());

        assertThrows(AccessDeniedException.class, () -> postService.updatePost(1, req));

        verify(postRepository, never()).existsByTitle(anyString());
        verify(postRepository, never()).save(any());
        verify(kafkaMessageService, never()).sendPostUpdatedMessage(anyInt(), anyInt());
    }

    @Test
    void softDeletePost_OK_setsDeletedTrue_andSendsEvent() {
        when(postRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testPost));
        doNothing().when(accessValidator).validateAdminOrOwnerAccess(testUser.getId());
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        postService.softDeletePost(1);

        assertTrue(testPost.getDeleted());
        verify(postRepository).save(testPost);
        verify(kafkaMessageService).sendPostDeletedMessage(1, 1);
    }

    @Test
    void softDeletePost_NotFound_Throws() {
        when(postRepository.findByIdAndDeletedFalse(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> postService.softDeletePost(99));

        verify(accessValidator, never()).validateAdminOrOwnerAccess(anyInt());
        verify(postRepository, never()).save(any());
        verify(kafkaMessageService, never()).sendPostDeletedMessage(anyInt(), anyInt());
    }

    @Test
    void softDeletePost_AccessDenied_Propagates() {
        when(postRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testPost));
        doThrow(new AccessDeniedException("nope"))
                .when(accessValidator).validateAdminOrOwnerAccess(testUser.getId());

        assertThrows(AccessDeniedException.class, () -> postService.softDeletePost(1));

        verify(postRepository, never()).save(any());
        verify(kafkaMessageService, never()).sendPostDeletedMessage(anyInt(), anyInt());
    }

    @Test
    void findAllPosts_mapsList_andPagination() {
        Post p1 = new Post();
        p1.setId(11);
        Post p2 = new Post();
        p2.setId(12);

        Page<Post> page = new PageImpl<>(List.of(p1, p2), PageRequest.of(0, 2), 5);
        when(postRepository.findAll(any(Pageable.class))).thenReturn(page);

        PostSearchDTO d1 = new PostSearchDTO();
        d1.setId(11);
        PostSearchDTO d2 = new PostSearchDTO();
        d2.setId(12);
        when(postMapper.toPostSearchDTO(p1)).thenReturn(d1);
        when(postMapper.toPostSearchDTO(p2)).thenReturn(d2);

        IamResponse<PaginationResponse<PostSearchDTO>> resp =
                postService.findAllPosts(PageRequest.of(0, 2));

        assertTrue(resp.isSuccess());
        assertEquals(2, resp.getPayload().getContent().size());
        assertEquals(5, resp.getPayload().getPagination().getTotal());
        assertEquals(2, resp.getPayload().getPagination().getLimit());
        assertEquals(1, resp.getPayload().getPagination().getPage()); // number + 1

        verify(postRepository).findAll(any(Pageable.class));
    }

    @Test
    void searchPosts_usesSpecification_andMapsDTOs() {
        PostSearchRequest req = new PostSearchRequest();
        Pageable pageable = PageRequest.of(1, 3);

        Post p1 = new Post();
        p1.setId(21);
        Post p2 = new Post();
        p2.setId(22);
        Page<Post> page = new PageImpl<>(List.of(p1, p2), pageable, 8);
        when(postRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PostSearchDTO d1 = new PostSearchDTO();
        d1.setId(21);
        PostSearchDTO d2 = new PostSearchDTO();
        d2.setId(22);
        when(postMapper.toPostSearchDTO(p1)).thenReturn(d1);
        when(postMapper.toPostSearchDTO(p2)).thenReturn(d2);

        IamResponse<PaginationResponse<PostSearchDTO>> resp =
                postService.searchPosts(req, pageable);

        assertTrue(resp.isSuccess());
        assertEquals(2, resp.getPayload().getContent().size());
        assertEquals(8, resp.getPayload().getPagination().getTotal());
        assertEquals(3, resp.getPayload().getPagination().getLimit());
        assertEquals(2, resp.getPayload().getPagination().getPage());

        verify(postRepository).findAll(any(Specification.class), eq(pageable));
    }
}
