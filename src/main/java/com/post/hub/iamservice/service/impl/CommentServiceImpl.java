package com.post.hub.iamservice.service.impl;

import com.post.hub.iamservice.kafka.service.KafkaMessageService;
import com.post.hub.iamservice.mapper.CommentMapper;
import com.post.hub.iamservice.model.constants.ApiErrorMessage;
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
import com.post.hub.iamservice.repository.criteria.CommentSearchCriteria;
import com.post.hub.iamservice.security.validation.AccessValidator;
import com.post.hub.iamservice.service.CommentService;
import com.post.hub.iamservice.utils.ApiUtils;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentMapper commentMapper;
    private final ApiUtils apiUtils;
    private final AccessValidator accessValidator;
    private final KafkaMessageService kafkaMessageService;

    @Override
    @Transactional
    public IamResponse<CommentDTO> createComment(@NotNull CommentRequest request) {
        Integer userId = apiUtils.getUserIdFromAuthentication();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ApiErrorMessage.USER_NOT_FOUND_BY_ID.getMessage(userId)));

        Post post = postRepository.findByIdAndDeletedFalse(request.getPostId())
                .orElseThrow(() -> new NotFoundException(ApiErrorMessage.POST_NOT_FOUND_BY_ID.getMessage(request.getPostId())));

        Comment comment = commentMapper.createComment(request, user, post);
        comment = commentRepository.save(comment);

        kafkaMessageService.sendCommentCreatedMessage(user.getId(), comment.getId());

        return IamResponse.createSuccessful(commentMapper.toDTO(comment));
    }

    @Override
    @Transactional
    public IamResponse<CommentDTO> updateComment(@NotNull Integer commentId, UpdateCommentRequest request) {
        Comment comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new NotFoundException(ApiErrorMessage.COMMENT_NOT_FOUND_BY_ID.getMessage(commentId)));

        accessValidator.validateAdminOrOwnerAccess(comment.getUser().getId());

        if (request.getPostId() != null) {
            Post post = postRepository.findById(request.getPostId())
                    .orElseThrow(() -> new NotFoundException(ApiErrorMessage.POST_NOT_FOUND_BY_ID.getMessage(request.getPostId())));
            comment.setPost(post);
        }

        commentMapper.updateComment(comment, request);
        comment = commentRepository.save(comment);

        kafkaMessageService.sendCommentUpdatedMessage(comment.getUser().getId(), comment.getId(), comment.getMessage());

        return IamResponse.createSuccessful(commentMapper.toDTO(comment));
    }

    @Override
    @Transactional(readOnly = true)
    public IamResponse<CommentDTO> getCommentById(@NotNull Integer commentId) {
        Comment comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new NotFoundException(ApiErrorMessage.COMMENT_NOT_FOUND_BY_ID.getMessage(commentId)));

        return IamResponse.createSuccessful(commentMapper.toDTO(comment));
    }

    @Override
    @Transactional(readOnly = true)
    public IamResponse<PaginationResponse<CommentSearchDTO>> findAllComments(Pageable pageable) {
        Page<CommentSearchDTO> comments = commentRepository.findAll(pageable)
                .map(commentMapper::toCommentSearchDTO);

        PaginationResponse<CommentSearchDTO> paginationResponse = new PaginationResponse<>(
                comments.getContent(),
                new PaginationResponse.Pagination(
                        comments.getTotalElements(),
                        pageable.getPageSize(),
                        comments.getNumber() + 1,
                        comments.getTotalPages()
                )
        );

        return IamResponse.createSuccessful(paginationResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public IamResponse<PaginationResponse<CommentSearchDTO>> searchComments(@NotNull CommentSearchRequest request, Pageable pageable) {
        Specification<Comment> specification = new CommentSearchCriteria(request);

        Page<CommentSearchDTO> commentsPage = commentRepository.findAll(specification, pageable)
                .map(commentMapper::toCommentSearchDTO);

        PaginationResponse<CommentSearchDTO> response = PaginationResponse.<CommentSearchDTO>builder()
                .content(commentsPage.getContent())
                .pagination(
                        PaginationResponse.Pagination.builder()
                                .total(commentsPage.getTotalElements())
                                .limit(pageable.getPageSize())
                                .page(commentsPage.getNumber() + 1)
                                .pages(commentsPage.getTotalPages())
                                .build()
                )
                .build();

        return IamResponse.createSuccessful(response);
    }

    @Override
    @Transactional
    public void softDelete(@NotNull Integer commentId) {
        Comment comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new NotFoundException(ApiErrorMessage.COMMENT_NOT_FOUND_BY_ID.getMessage(commentId)));

        accessValidator.validateAdminOrOwnerAccess(comment.getUser().getId());

        comment.setDeleted(true);
        commentRepository.save(comment);

        kafkaMessageService.sendCommentDeletedMessage(comment.getUser().getId(), comment.getId());
    }
}
