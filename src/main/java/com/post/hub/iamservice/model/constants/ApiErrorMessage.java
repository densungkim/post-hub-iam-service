package com.post.hub.iamservice.model.constants;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ApiErrorMessage {
    POST_NOT_FOUND_BY_ID("Post with ID: %s was not found"),
    POST_ALREADY_EXISTS("Post with Title: %s already exists"),
    USER_NOT_FOUND_BY_ID("User with ID: %s was not found"),
    USERNAME_ALREADY_EXISTS("Username: %s already exists"),
    USERNAME_NOT_FOUND("Username: %s was not found"),
    EMAIL_ALREADY_EXISTS("Email: %s already exists"),
    EMAIL_NOT_FOUND("Email: %s was not found"),
    USER_ROLE_NOT_FOUND("Role was not found"),
    COMMENT_NOT_FOUND_BY_ID("Comment with ID: %s was not found"),

    INVALID_TOKEN_SIGNATURE("Invalid token signature"),
    ERROR_DURING_JWT_PROCESSING("An unexpected error occurred during JWT processing"),
    TOKEN_EXPIRED("Token expired."),
    UNEXPECTED_ERROR_OCCURRED("An unexpected error occurred. Please try again later."),

    AUTHENTICATION_FAILED_FOR_USER("Authentication failed for user: {}. "),
    INVALID_USER_OR_PASSWORD("Invalid email or password. Try again"),
    INVALID_USER_REGISTRATION_STATUS("Invalid user registration status: %s. "),
    NOT_FOUND_REFRESH_TOKEN("Refresh token not found."),

    MISMATCH_PASSWORDS("Password does not match"),
    INVALID_PASSWORD("Invalid password. It must have: "
            + "length at least " + ApiConstants.REQUIRED_MIN_PASSWORD_LENGTH + ", including "
            + ApiConstants.REQUIRED_MIN_LETTERS_NUMBER_EVERY_CASE_IN_PASSWORD + " letter(s) in upper and lower cases, "
            + ApiConstants.REQUIRED_MIN_CHARACTERS_NUMBER_IN_PASSWORD + " character(s), "
            + ApiConstants.REQUIRED_MIN_DIGITS_NUMBER_IN_PASSWORD + " digit(s). "),
    HAVE_NO_ACCESS("You don't have the necessary permissions"),
    KAFKA_SEND_FAILED("Kafka message didn't send."),

    UNSUPPORTED_OAUTH2_PROVIDER("Unsupported OAuth2 provider: %s"),
    ;

    private final String message;

    public String getMessage(Object... args) {
        return String.format(message, args);
    }

}
