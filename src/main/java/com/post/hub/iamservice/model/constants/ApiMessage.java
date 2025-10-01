package com.post.hub.iamservice.model.constants;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ApiMessage {
    TOKEN_CREATED_OR_UPDATED("User's token has been created or updated"),
    REGISTRATION_SUCCESSFUL("Registration successful. Please check your email to confirm your account"),
    ;

    private final String message;

}
