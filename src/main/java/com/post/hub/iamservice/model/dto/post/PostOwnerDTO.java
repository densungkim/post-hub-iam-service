package com.post.hub.iamservice.model.dto.post;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostOwnerDTO implements Serializable {

    private Integer id;
    private String username;
    private String email;

}
