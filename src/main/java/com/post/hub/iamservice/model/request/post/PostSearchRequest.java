package com.post.hub.iamservice.model.request.post;

import com.post.hub.iamservice.model.enums.PostSortField;
import lombok.Data;

import java.io.Serializable;

@Data
public class PostSearchRequest implements Serializable {

    private String title;
    private String content;
    private Integer likes;

    private Boolean deleted;
    private String keyword;
    private PostSortField sortField;

}
