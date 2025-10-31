package com.post.hub.iamservice.controller;

import com.post.hub.iamservice.model.constants.ApiLogMessage;
import com.post.hub.iamservice.model.dto.comment.CommentDTO;
import com.post.hub.iamservice.model.dto.comment.CommentSearchDTO;
import com.post.hub.iamservice.model.request.comment.CommentRequest;
import com.post.hub.iamservice.model.request.comment.CommentSearchRequest;
import com.post.hub.iamservice.model.request.comment.UpdateCommentRequest;
import com.post.hub.iamservice.model.response.IamResponse;
import com.post.hub.iamservice.model.response.PaginationResponse;
import com.post.hub.iamservice.service.CommentService;
import com.post.hub.iamservice.utils.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${endpoint.comments}")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @Operation(summary = "Create a new Comment", description = "Adds a new comment to a post")
    public ResponseEntity<IamResponse<CommentDTO>> createComment(
            @RequestBody @Valid CommentRequest request

    ) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        IamResponse<CommentDTO> response = commentService.createComment(request);
        return ResponseEntity.ok(response);
    }


    @PutMapping("${endpoint.id}")
    @Operation(summary = "Update a Comment", description = "Updates an existing comment")
    public ResponseEntity<IamResponse<CommentDTO>> updateComment(
            @PathVariable(name = "id") Integer commentId,
            @RequestBody @Valid UpdateCommentRequest request
    ) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        IamResponse<CommentDTO> response = commentService.updateComment(commentId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("${endpoint.id}")
    @Operation(summary = "Get Comment by ID", description = "Fetches a comment by its unique identifier")
    public ResponseEntity<IamResponse<CommentDTO>> getCommentById(
            @PathVariable(name = "id") Integer commentId
    ) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        IamResponse<CommentDTO> response = commentService.getCommentById(commentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all Comments", description = "Retrieves a paginated list of all comments")
    public ResponseEntity<IamResponse<PaginationResponse<CommentSearchDTO>>> getAllComments(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        Pageable pageable = PageRequest.of(page, limit);

        IamResponse<PaginationResponse<CommentSearchDTO>> response = commentService.findAllComments(pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("${endpoint.search}")
    @Operation(summary = "Search Comments", description = "Search for comments using filters and pagination")
    public ResponseEntity<IamResponse<PaginationResponse<CommentSearchDTO>>> searchComments(
            @RequestBody @Valid CommentSearchRequest request,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        Pageable pageable = PageRequest.of(page, limit);
        IamResponse<PaginationResponse<CommentSearchDTO>> response = commentService.searchComments(request, pageable);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("${endpoint.id}")
    @Operation(summary = "Delete a Comment", description = "Marks a comment as deleted without removing it from the database")
    public ResponseEntity<Void> deleteCommentById(
            @PathVariable(name = "id") Integer commentId
    ) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        commentService.softDelete(commentId);
        return ResponseEntity.noContent().build();
    }

}
