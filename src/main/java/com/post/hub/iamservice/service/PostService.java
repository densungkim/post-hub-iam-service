package com.post.hub.iamservice.service;

import com.post.hub.iamservice.model.dto.post.PostDTO;
import com.post.hub.iamservice.model.dto.post.PostSearchDTO;
import com.post.hub.iamservice.model.request.post.NewPostRequest;
import com.post.hub.iamservice.model.request.post.PostSearchRequest;
import com.post.hub.iamservice.model.request.post.UpdatePostRequest;
import com.post.hub.iamservice.model.response.IamResponse;
import com.post.hub.iamservice.model.response.PaginationResponse;
import org.springframework.data.domain.Pageable;


public interface PostService {

    IamResponse<PostDTO> createPost(NewPostRequest request);

    IamResponse<PostDTO> updatePost(Integer postId, UpdatePostRequest request);

    IamResponse<PostDTO> getById(Integer postId);

    IamResponse<PaginationResponse<PostSearchDTO>> findAllPosts(Pageable pageable);

    IamResponse<PaginationResponse<PostSearchDTO>> searchPosts(PostSearchRequest request, Pageable pageable);

    void softDeletePost(Integer postId);

}
