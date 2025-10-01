package com.post.hub.iamservice.model.dto.post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDTO implements Serializable {

    private Integer id;
    private String title;
    private String content;
    private Integer likes;
    private Boolean deleted;
    private String createdBy;
    private LocalDateTime created;

}
