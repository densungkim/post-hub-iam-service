package com.post.hub.iamservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.post.hub.iamservice.kafka.model.utils.PostHubService;
import com.post.hub.iamservice.kafka.model.utils.UtilMessage;
import com.post.hub.iamservice.model.constants.ApiErrorMessage;
import com.post.hub.iamservice.model.constants.ApiLogMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Component
@Validated
@RequiredArgsConstructor
public class MessageProducer {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value(value = "${additional.kafka.topic.iam.service.logs}")
    private String logsOutTopic;

    @Value(value = "${kafka.enabled}")
    private boolean isKafkaEnabled;

    public void sendLogs(@NotNull @Valid UtilMessage message) {
        if (!isKafkaEnabled) {
            log.trace(ApiLogMessage.KAFKA_DISABLED.getValue(), message);
            return;
        }
        try {
            message.setService(PostHubService.IAM_SERVICE);
            String messageJson = objectMapper.writeValueAsString(message);
            log.debug(ApiLogMessage.KAFKA_SENDING.getValue(), messageJson);

            kafkaTemplate.send(logsOutTopic, messageJson).whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug(ApiLogMessage.KAFKA_SENT.getValue(), message.getActionType(), logsOutTopic, messageJson);
                } else {
                    log.error(ApiErrorMessage.KAFKA_SEND_FAILED.getMessage(), ex);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message to JSON. Message: {}", message, e);
        }
    }
}
