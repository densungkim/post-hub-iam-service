package com.post.hub.iamservice;// например рядом с твоим BaseIntegrationTest

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.test.context.ActiveProfiles;

@Configuration
@ActiveProfiles("test")
public class KafkaTestTopicsConfig {

    @Bean
    public NewTopic iamLogsTopic() {
        return TopicBuilder.name("iam_topic_for_test")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
