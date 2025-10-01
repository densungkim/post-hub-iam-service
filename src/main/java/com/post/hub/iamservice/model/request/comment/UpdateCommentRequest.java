package com.post.hub.iamservice.model.request.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommentRequest {

    @NotNull(message = "Post ID cannot be null")
    private Integer postId;

    @NotBlank(message = "Message cannot be empty")
    private String message;

}
