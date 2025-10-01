package com.post.hub.iamservice.service;

import com.post.hub.iamservice.kafka.service.KafkaMessageService;
import com.post.hub.iamservice.mapper.PostMapper;
import com.post.hub.iamservice.model.dto.post.PostDTO;
import com.post.hub.iamservice.model.entities.Post;
import com.post.hub.iamservice.model.entities.User;
import com.post.hub.iamservice.model.exception.DataExistException;
import com.post.hub.iamservice.model.exception.NotFoundException;
import com.post.hub.iamservice.model.request.post.NewPostRequest;
import com.post.hub.iamservice.repository.PostRepository;
import com.post.hub.iamservice.repository.UserRepository;
import com.post.hub.iamservice.service.impl.PostServiceImpl;
import com.post.hub.iamservice.utils.ApiUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class PostServiceTest {

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
    void getById_PostExists_ReturnsPostDTO() {
        when(postRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testPost));
        when(postMapper.toDTO(testPost)).thenReturn(testPostDTO);

        PostDTO result = postService.getById(1).getPayload();

        assertNotNull(result);
        assertEquals(testPostDTO.getId(), result.getId());
        assertEquals(testPostDTO.getTitle(), result.getTitle());

        verify(postRepository, times(1)).findByIdAndDeletedFalse(1);
        verify(postMapper, times(1)).toDTO(testPost);
    }

    @Test
    void getByID_PostNotFound_ThrowsException() {
        when(postRepository.findByIdAndDeletedFalse(999)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> postService.getById(999));

        assertTrue(exception.getMessage().contains("not found"));

        verify(postRepository, times(1)).findByIdAndDeletedFalse(999);
        verify(postMapper, never()).toDTO(any(Post.class));
    }

    @Test
    void createPost_OK() {
        NewPostRequest request = new NewPostRequest("New Title", "New Content", 100);

        when(apiUtils.getUserIdFromAuthentication()).thenReturn(testUser.getId());
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(postMapper.createPost(request, testUser, testUser.getUsername())).thenReturn(testPost);
        when(postRepository.save(any(Post.class))).thenReturn(testPost);
        when(postMapper.toDTO(testPost)).thenReturn(testPostDTO);

        PostDTO result = postService.createPost(request).getPayload();

        assertNotNull(result);
        assertEquals(testPostDTO.getId(), result.getId());
        assertEquals(testPostDTO.getContent(), result.getContent());

        verify(apiUtils, times(1)).getUserIdFromAuthentication();
        verify(userRepository, times(1)).findById(testUser.getId());
        verify(postRepository, times(1)).save(any(Post.class));
        verify(postMapper, times(1)).toDTO(any(Post.class));
        verify(kafkaMessageService, times(1)).sendPostCreatedMessage(testUser.getId(), testPost.getId());
    }

    @Test
    void createPost_TitleAlreadyExists_ThrowsException() {
        NewPostRequest request = new NewPostRequest("New Title", "New Content", 100);

        when(postRepository.existsByTitle(request.getTitle())).thenReturn(true);

        assertThatThrownBy(() -> postService.createPost(request))
                .isInstanceOf(DataExistException.class)
                .hasMessageContaining("already exists");
    }
}
