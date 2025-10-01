package com.post.hub.iamservice.controller;

import com.post.hub.iamservice.model.constants.ApiLogMessage;
import com.post.hub.iamservice.model.dto.post.PostDTO;
import com.post.hub.iamservice.model.dto.post.PostSearchDTO;
import com.post.hub.iamservice.model.request.post.NewPostRequest;
import com.post.hub.iamservice.model.request.post.PostSearchRequest;
import com.post.hub.iamservice.model.request.post.UpdatePostRequest;
import com.post.hub.iamservice.model.response.IamResponse;
import com.post.hub.iamservice.model.response.PaginationResponse;
import com.post.hub.iamservice.service.PostService;
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

import java.security.Principal;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${endpoint.posts}")
public class PostController {

    private final PostService postService;

    @PostMapping
    @Operation(summary = "Create a new Post", description = "Adds a new post to the system")
    public ResponseEntity<IamResponse<PostDTO>> createPost(
            @RequestBody @Valid NewPostRequest request,
            Principal principal
    ) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        System.out.println("PRINCIPAL: " + principal.getName());
        IamResponse<PostDTO> response = postService.createPost(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("${endpoint.id}")
    @Operation(summary = "Update a Post", description = "Updates an existing post by its ID")
    public ResponseEntity<IamResponse<PostDTO>> updatePostById(
            @PathVariable(name = "id") Integer postId,
            @RequestBody @Valid UpdatePostRequest request
    ) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        IamResponse<PostDTO> response = postService.updatePost(postId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("${endpoint.id}")
    @Operation(summary = "Get Post by ID", description = "Retrieves a post by its unique identifier")
    public ResponseEntity<IamResponse<PostDTO>> getPostById(
            @PathVariable(name = "id") Integer postId
    ) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        IamResponse<PostDTO> response = postService.getById(postId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all Posts", description = "Retrieves a paginated list of all posts")
    public ResponseEntity<IamResponse<PaginationResponse<PostSearchDTO>>> getAllPosts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        Pageable pageable = PageRequest.of(page, limit);
        IamResponse<PaginationResponse<PostSearchDTO>> response = postService.findAllPosts(pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("${endpoint.search}")
    @Operation(summary = "Search Posts", description = "Searches for posts based on filters and pagination")
    public ResponseEntity<IamResponse<PaginationResponse<PostSearchDTO>>> searchPosts(
            @RequestBody @Valid PostSearchRequest request,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        Pageable pageable = PageRequest.of(page, limit);
        IamResponse<PaginationResponse<PostSearchDTO>> response = postService.searchPosts(request, pageable);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("${endpoint.id}")
    @Operation(summary = "Delete a Post", description = "Marks a post as deleted without removing it from the database")
    public ResponseEntity<Void> softDeletePostById(
            @PathVariable(name = "id") Integer postId
    ) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        postService.softDeletePost(postId);
        return ResponseEntity.noContent().build();
    }

}
