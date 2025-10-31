package com.post.hub.iamservice.service;

import com.post.hub.iamservice.model.dto.comment.CommentDTO;
import com.post.hub.iamservice.model.dto.comment.CommentSearchDTO;
import com.post.hub.iamservice.model.request.comment.CommentRequest;
import com.post.hub.iamservice.model.request.comment.CommentSearchRequest;
import com.post.hub.iamservice.model.request.comment.UpdateCommentRequest;
import com.post.hub.iamservice.model.response.IamResponse;
import com.post.hub.iamservice.model.response.PaginationResponse;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    IamResponse<CommentDTO> createComment(CommentRequest request);

    IamResponse<CommentDTO> updateComment(Integer commentId, UpdateCommentRequest request);

    IamResponse<CommentDTO> getCommentById(Integer commentId);

    IamResponse<PaginationResponse<CommentSearchDTO>> findAllComments(Pageable pageable);

    IamResponse<PaginationResponse<CommentSearchDTO>> searchComments(CommentSearchRequest request, Pageable pageable);

    void softDelete(Integer commentId);

}
