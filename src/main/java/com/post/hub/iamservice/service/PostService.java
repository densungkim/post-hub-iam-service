package com.post.hub.iamservice.service;

import com.post.hub.iamservice.model.dto.post.PostDTO;
import com.post.hub.iamservice.model.dto.post.PostSearchDTO;
import com.post.hub.iamservice.model.request.post.NewPostRequest;
import com.post.hub.iamservice.model.request.post.PostSearchRequest;
import com.post.hub.iamservice.model.request.post.UpdatePostRequest;
import com.post.hub.iamservice.model.response.IamResponse;
import com.post.hub.iamservice.model.response.PaginationResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;


public interface PostService {

    IamResponse<PostDTO> createPost(@NotNull NewPostRequest request);

    IamResponse<PostDTO> updatePost(@NotNull Integer postId, @NotNull UpdatePostRequest request);

    IamResponse<PostDTO> getById(@NotNull Integer postId);

    IamResponse<PaginationResponse<PostSearchDTO>> findAllPosts(Pageable pageable);

    IamResponse<PaginationResponse<PostSearchDTO>> searchPosts(@NotNull PostSearchRequest request, Pageable pageable);

    void softDeletePost(@NotNull Integer postId);

}
