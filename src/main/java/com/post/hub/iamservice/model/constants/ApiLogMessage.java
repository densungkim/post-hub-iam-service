package com.post.hub.iamservice.model.constants;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ApiLogMessage {
    POST_INFO_BY_ID("Receiving post with ID: {}"),
    NAME_OF_CURRENT_METHOD("Current method: {}"),
    KAFKA_DISABLED("Kafka is not enabled. Message will not be placed in iam_logs topic [message={}] "),
    KAFKA_SENDING("Sending message to Kafka: {}"),
    KAFKA_SENT("Kafka {} message sent. Topic: '{}', message='{}'"),
    PASSWORD_CHANGED_SUCCESSFULLY("Password changed successfully"),
    ;

    private final String value;
}
