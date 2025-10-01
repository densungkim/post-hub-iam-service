package com.post.hub.iamservice.service;

import com.post.hub.iamservice.model.dto.comment.CommentDTO;
import com.post.hub.iamservice.model.dto.comment.CommentSearchDTO;
import com.post.hub.iamservice.model.request.comment.CommentRequest;
import com.post.hub.iamservice.model.request.comment.CommentSearchRequest;
import com.post.hub.iamservice.model.request.comment.UpdateCommentRequest;
import com.post.hub.iamservice.model.response.IamResponse;
import com.post.hub.iamservice.model.response.PaginationResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    IamResponse<CommentDTO> createComment(@NotNull CommentRequest request);

    IamResponse<CommentDTO> updateComment(@NotNull Integer commentId, @NotNull UpdateCommentRequest request);

    IamResponse<CommentDTO> getCommentById(@NotNull Integer commentId);

    IamResponse<PaginationResponse<CommentSearchDTO>> findAllComments(Pageable pageable);

    IamResponse<PaginationResponse<CommentSearchDTO>> searchComments(@NotNull CommentSearchRequest request, Pageable pageable);

    void softDelete(@NotNull Integer commentId);

}
