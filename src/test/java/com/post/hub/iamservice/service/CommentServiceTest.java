package com.post.hub.iamservice.service;

import com.post.hub.iamservice.kafka.service.KafkaMessageService;
import com.post.hub.iamservice.mapper.CommentMapper;
import com.post.hub.iamservice.model.dto.comment.CommentDTO;
import com.post.hub.iamservice.model.dto.comment.CommentSearchDTO;
import com.post.hub.iamservice.model.entities.Comment;
import com.post.hub.iamservice.model.entities.Post;
import com.post.hub.iamservice.model.entities.User;
import com.post.hub.iamservice.model.exception.NotFoundException;
import com.post.hub.iamservice.model.request.comment.CommentRequest;
import com.post.hub.iamservice.model.request.comment.CommentSearchRequest;
import com.post.hub.iamservice.model.request.comment.UpdateCommentRequest;
import com.post.hub.iamservice.model.response.IamResponse;
import com.post.hub.iamservice.model.response.PaginationResponse;
import com.post.hub.iamservice.repository.CommentRepository;
import com.post.hub.iamservice.repository.PostRepository;
import com.post.hub.iamservice.repository.UserRepository;
import com.post.hub.iamservice.security.validation.AccessValidator;
import com.post.hub.iamservice.service.impl.CommentServiceImpl;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    private AccessValidator accessValidator;

    @Mock
    private KafkaMessageService kafkaMessageService;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Comment testComment;
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

        testComment = new Comment();
        testComment.setId(1);
        testComment.setMessage("Test Comment");
        testComment.setPost(testPost);
        testComment.setUser(testUser);

        testCommentDTO = new CommentDTO();
        testCommentDTO.setId(1);
        testCommentDTO.setMessage("Test Comment");
        testCommentDTO.setPostId(testPost.getId());
    }

    @Test
    void getCommentById_CommentExists_ReturnsCommentDTO() {
        when(commentRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testComment));
        when(commentMapper.toDTO(testComment)).thenReturn(testCommentDTO);

        CommentDTO result = commentService.getCommentById(1).getPayload();

        assertNotNull(result);
        assertEquals(testCommentDTO.getId(), result.getId());
        assertEquals(testCommentDTO.getMessage(), result.getMessage());

        verify(commentRepository).findByIdAndDeletedFalse(1);
        verify(commentMapper).toDTO(testComment);
    }

    @Test
    void getCommentById_CommentNotFound_ThrowsException() {
        when(commentRepository.findByIdAndDeletedFalse(999)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> commentService.getCommentById(999));

        assertTrue(exception.getMessage().contains("not found"));

        verify(commentRepository).findByIdAndDeletedFalse(999);
        verify(commentMapper, never()).toDTO(any(Comment.class));
    }

    @Test
    void createComment_OK() {
        CommentRequest request = new CommentRequest(1, "New comment");

        when(apiUtils.getUserIdFromAuthentication()).thenReturn(testUser.getId());
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(postRepository.findByIdAndDeletedFalse(testPost.getId())).thenReturn(Optional.of(testPost));
        when(commentMapper.createComment(request, testUser, testPost)).thenReturn(testComment);
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);
        when(commentMapper.toDTO(testComment)).thenReturn(testCommentDTO);

        CommentDTO result = commentService.createComment(request).getPayload();

        assertNotNull(result);
        assertEquals(testCommentDTO.getMessage(), result.getMessage());

        verify(apiUtils).getUserIdFromAuthentication();
        verify(userRepository).findById(testUser.getId());
        verify(postRepository).findByIdAndDeletedFalse(testPost.getId());
        verify(commentRepository).save(any(Comment.class));
        verify(commentMapper).toDTO(any(Comment.class));
        verify(kafkaMessageService).sendCommentCreatedMessage(testUser.getId(), testPost.getId());
    }

    @Test
    void createComment_UserNotFound_ThrowsException() {
        CommentRequest request = new CommentRequest(1, "New comment");

        when(apiUtils.getUserIdFromAuthentication()).thenReturn(123);
        when(userRepository.findById(123)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(request))
                .isInstanceOf(NotFoundException.class);

        verify(postRepository, never()).findByIdAndDeletedFalse(anyInt());
        verify(commentRepository, never()).save(any());
        verify(kafkaMessageService, never()).sendCommentCreatedMessage(anyInt(), anyInt());
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

        verify(apiUtils).getUserIdFromAuthentication();
        verify(userRepository).findById(testUser.getId());
        verify(postRepository).findByIdAndDeletedFalse(testPost.getId());
        verify(commentRepository, never()).save(any(Comment.class));
        verify(commentMapper, never()).toDTO(any(Comment.class));
    }

    @Test
    void updateComment_OK_updatesMessageOnly_whenPostIdNull() {
        UpdateCommentRequest req = new UpdateCommentRequest(null, "Updated msg");

        when(commentRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testComment));
        doNothing().when(accessValidator).validateAdminOrOwnerAccess(testUser.getId());
        doAnswer(inv -> {
            Comment c = inv.getArgument(0);
            UpdateCommentRequest r = inv.getArgument(1);
            c.setMessage(r.getMessage());
            return null;
        }).when(commentMapper).updateComment(any(Comment.class), any(UpdateCommentRequest.class));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        CommentDTO dto = new CommentDTO();
        dto.setId(1);
        dto.setMessage("Updated msg");
        when(commentMapper.toDTO(testComment)).thenReturn(dto);

        IamResponse<CommentDTO> resp = commentService.updateComment(1, req);
        assertTrue(resp.isSuccess());
        assertEquals("Updated msg", resp.getPayload().getMessage());

        verify(postRepository, never()).findById(anyInt());
        verify(kafkaMessageService).sendCommentUpdatedMessage(testUser.getId(), testComment.getId(), "Updated msg");
    }

    @Test
    void updateComment_OK_changesPost_whenPostIdProvided() {
        UpdateCommentRequest req = new UpdateCommentRequest(2, "Keep msg");
        Post newPost = new Post();
        newPost.setId(2);
        newPost.setTitle("Another");

        when(commentRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testComment));
        doNothing().when(accessValidator).validateAdminOrOwnerAccess(testUser.getId());
        when(postRepository.findById(2)).thenReturn(Optional.of(newPost));
        doNothing().when(commentMapper).updateComment(any(Comment.class), any(UpdateCommentRequest.class));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        when(commentMapper.toDTO(testComment))
                .thenReturn(new CommentDTO(1, testComment.getMessage(), null, 2, null, null));

        IamResponse<CommentDTO> resp = commentService.updateComment(1, req);
        assertTrue(resp.isSuccess());
        assertEquals(2, resp.getPayload().getPostId());

        assertEquals(2, testComment.getPost().getId());
        verify(kafkaMessageService).sendCommentUpdatedMessage(eq(testUser.getId()), eq(testComment.getId()), anyString());
    }


    @Test
    void updateComment_CommentNotFound_Throws() {
        when(commentRepository.findByIdAndDeletedFalse(7)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.updateComment(7, new UpdateCommentRequest(null, "x")))
                .isInstanceOf(NotFoundException.class);

        verify(accessValidator, never()).validateAdminOrOwnerAccess(anyInt());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void updateComment_PostNotFound_Throws() {
        UpdateCommentRequest req = new UpdateCommentRequest(9, "x");

        when(commentRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testComment));
        doNothing().when(accessValidator).validateAdminOrOwnerAccess(testUser.getId());
        when(postRepository.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.updateComment(1, req))
                .isInstanceOf(NotFoundException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    void updateComment_AccessDenied_Propagates() {
        when(commentRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testComment));
        doThrow(new AccessDeniedException("forbidden"))
                .when(accessValidator).validateAdminOrOwnerAccess(testUser.getId());

        assertThatThrownBy(() -> commentService.updateComment(1, new UpdateCommentRequest(null, "x")))
                .isInstanceOf(AccessDeniedException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    void softDelete_OK_setsDeletedTrue_andSendsEvent() {
        when(commentRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testComment));
        doNothing().when(accessValidator).validateAdminOrOwnerAccess(testUser.getId());
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        commentService.softDelete(1);

        assertTrue(testComment.getDeleted(), "flag deleted must be true");
        verify(commentRepository).save(testComment);
        verify(kafkaMessageService).sendCommentDeletedMessage(testUser.getId(), testComment.getId());
    }

    @Test
    void softDelete_NotFound_Throws() {
        when(commentRepository.findByIdAndDeletedFalse(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.softDelete(99))
                .isInstanceOf(NotFoundException.class);

        verify(accessValidator, never()).validateAdminOrOwnerAccess(anyInt());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void softDelete_AccessDenied_Propagates() {
        when(commentRepository.findByIdAndDeletedFalse(1)).thenReturn(Optional.of(testComment));
        doThrow(new AccessDeniedException("nope"))
                .when(accessValidator).validateAdminOrOwnerAccess(testUser.getId());

        assertThatThrownBy(() -> commentService.softDelete(1))
                .isInstanceOf(AccessDeniedException.class);

        verify(commentRepository, never()).save(any());
        verify(kafkaMessageService, never()).sendCommentDeletedMessage(anyInt(), anyInt());
    }

    @Test
    void findAllComments_MapsPageToDTOAndPagination() {
        Comment c1 = new Comment();
        c1.setId(11);
        Comment c2 = new Comment();
        c2.setId(12);

        Page<Comment> page = new PageImpl<>(List.of(c1, c2), PageRequest.of(0, 2), 5);
        when(commentRepository.findAll(any(Pageable.class))).thenReturn(page);

        CommentSearchDTO d1 = new CommentSearchDTO();
        d1.setId(11);
        CommentSearchDTO d2 = new CommentSearchDTO();
        d2.setId(12);
        when(commentMapper.toCommentSearchDTO(c1)).thenReturn(d1);
        when(commentMapper.toCommentSearchDTO(c2)).thenReturn(d2);

        IamResponse<PaginationResponse<CommentSearchDTO>> resp =
                commentService.findAllComments(PageRequest.of(0, 2));

        assertTrue(resp.isSuccess());
        assertThat(resp.getPayload().getContent()).extracting("id").containsExactly(11, 12);
        assertEquals(5, resp.getPayload().getPagination().getTotal());
        assertEquals(2, resp.getPayload().getPagination().getLimit());
        assertEquals(1, resp.getPayload().getPagination().getPage());
    }

    @Test
    void searchComments_UsesSpecificationAndMapsDTOs() {
        CommentSearchRequest req = new CommentSearchRequest();
        Pageable pageable = PageRequest.of(1, 3);

        Comment c1 = new Comment();
        c1.setId(21);
        Comment c2 = new Comment();
        c2.setId(22);
        Page<Comment> page = new PageImpl<>(List.of(c1, c2), pageable, 8);
        when(commentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        CommentSearchDTO d1 = new CommentSearchDTO();
        d1.setId(21);
        CommentSearchDTO d2 = new CommentSearchDTO();
        d2.setId(22);
        when(commentMapper.toCommentSearchDTO(c1)).thenReturn(d1);
        when(commentMapper.toCommentSearchDTO(c2)).thenReturn(d2);

        IamResponse<PaginationResponse<CommentSearchDTO>> resp = commentService.searchComments(req, pageable);

        assertTrue(resp.isSuccess());
        assertThat(resp.getPayload().getContent()).extracting("id").containsExactly(21, 22);
        assertEquals(8, resp.getPayload().getPagination().getTotal());
        assertEquals(3, resp.getPayload().getPagination().getLimit());
        assertEquals(2, resp.getPayload().getPagination().getPage());

        verify(commentRepository).findAll(any(Specification.class), eq(pageable));

    }
}
