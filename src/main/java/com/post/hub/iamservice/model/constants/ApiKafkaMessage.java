package com.post.hub.iamservice.model.constants;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ApiKafkaMessage {

    USER_CREATED("User '%s' was successfully created with ID: %s"),
    USER_UPDATED("User '%s' was successfully updated"),
    USER_DELETED("User '%s' was successfully deleted"),
    POST_CREATED("Post created by userId: %s, postId: %s"),
    POST_UPDATED("Post with ID: '%s' was successfully updated"),
    POST_DELETED("Post with ID: '%s' was deleted by userId: '%s'"),
    COMMENT_CREATED("Comment created by userId: %s, commentId: %s"),
    COMMENT_UPDATED("Comment: '%s' was updated by userId: %s, commentId: %s"),
    COMMENT_DELETED("Comment with ID: '%s' was deleted by userId: %s"),
    ;

    private final String value;

    public String getMessage(Object... args) {
        return String.format(value, args);
    }
}
