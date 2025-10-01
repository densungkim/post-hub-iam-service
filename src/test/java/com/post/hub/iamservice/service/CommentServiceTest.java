package com.post.hub.iamservice.service;

import com.post.hub.iamservice.kafka.service.KafkaMessageService;
import com.post.hub.iamservice.mapper.CommentMapper;
import com.post.hub.iamservice.model.dto.comment.CommentDTO;
import com.post.hub.iamservice.model.entities.Comment;
import com.post.hub.iamservice.model.entities.Post;
import com.post.hub.iamservice.model.entities.User;
import com.post.hub.iamservice.model.exception.NotFoundException;
import com.post.hub.iamservice.model.request.comment.CommentRequest;
import com.post.hub.iamservice.repository.CommentRepository;
import com.post.hub.iamservice.repository.PostRepository;
import com.post.hub.iamservice.repository.UserRepository;
import com.post.hub.iamservice.service.impl.CommentServiceImpl;
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
public class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private ApiUtils apiUtils;

    @Mock
    private KafkaMessageService kafkaMessageService;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Comment tesComment;
    private CommentDTO testCommentDTO;
    private User testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("TestUser");

        testPost = new Post();
        testPost.setId(1);
        testPost.setTitle("TestPost");

        tesComment = new Comment();
        tesComment.setId(1);
        tesComment.setMessage("Test Comment");
        tesComment.setPost(testPost);
        tesComment.setUser(testUser);

        testCommentDTO = new CommentDTO();
        testCommentDTO.setId(1);
        testCommentDTO.setMessage("Test Comment");
    }

    @Test
    void getCommentById_CommentExists_ReturnsCommentDTO() {
        when(commentRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(tesComment));
        when(commentMapper.toDTO(tesComment)).thenReturn(testCommentDTO);

        CommentDTO result = commentService.getCommentById(1).getPayload();

        assertNotNull(result);
        assertEquals(testCommentDTO.getId(), result.getId());
        assertEquals(testCommentDTO.getMessage(), result.getMessage());

        verify(commentRepository, times(1)).findByIdAndDeletedFalse(1);
        verify(commentMapper, times(1)).toDTO(tesComment);
    }

    @Test
    void getCommentById_CommentNotFound_ThrowsException() {
        when(commentRepository.findByIdAndDeletedFalse(999)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> commentService.getCommentById(999));

        assertTrue(exception.getMessage().contains("not found"));

        verify(commentRepository, times(1)).findByIdAndDeletedFalse(999);
        verify(commentMapper, never()).toDTO(any(Comment.class));
    }

    @Test
    void createComment_OK() {
        CommentRequest request = new CommentRequest(1, "New comment");

        when(apiUtils.getUserIdFromAuthentication()).thenReturn(testUser.getId());
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(postRepository.findByIdAndDeletedFalse(testPost.getId())).thenReturn(Optional.of(testPost));
        when(commentMapper.createComment(request, testUser, testPost)).thenReturn(tesComment);
        when(commentRepository.save(any(Comment.class))).thenReturn(tesComment);
        when(commentMapper.toDTO(tesComment)).thenReturn(testCommentDTO);

        CommentDTO result = commentService.createComment(request).getPayload();

        assertNotNull(result);
        assertEquals(testCommentDTO.getMessage(), result.getMessage());

        verify(apiUtils, times(1)).getUserIdFromAuthentication();
        verify(userRepository, times(1)).findById(testUser.getId());
        verify(postRepository, times(1)).findByIdAndDeletedFalse(testPost.getId());
        verify(commentRepository, times(1)).save(any(Comment.class));
        verify(commentMapper, times(1)).toDTO(any(Comment.class));
        verify(kafkaMessageService, times(1)).sendCommentCreatedMessage(testUser.getId(), testPost.getId());
    }

    @Test
    void createComment_PostNotFound_ThrowsException() {
        CommentRequest request = new CommentRequest(1, "New comment");

        when(apiUtils.getUserIdFromAuthentication()).thenReturn(testUser.getId());
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(postRepository.findByIdAndDeletedFalse(testPost.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("not found");

        verify(apiUtils, times(1)).getUserIdFromAuthentication();
        verify(userRepository, times(1)).findById(testUser.getId());
        verify(postRepository, times(1)).findByIdAndDeletedFalse(testPost.getId());
        verify(commentRepository, never()).save(any(Comment.class));
        verify(commentMapper, never()).toDTO(any(Comment.class));
    }
}
