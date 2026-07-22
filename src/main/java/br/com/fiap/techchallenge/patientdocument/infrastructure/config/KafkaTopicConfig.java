package br.com.fiap.techchallenge.patientdocument.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        name = "app.messaging.kafka.enabled",
        havingValue = "true"
)
public class KafkaTopicConfig {

    @Bean
    public NewTopic documentProcessingRequestedTopic(
            @Value(
                    "${app.messaging.kafka.topics.processing-requested}"
            )
            String topic
    ) {
        return createTopic(topic);
    }

    @Bean
    public NewTopic documentProcessedResponseTopic(
            @Value(
                    "${app.messaging.kafka.topics.processed-response}"
            )
            String topic
    ) {
        return createTopic(topic);
    }

    @Bean
    public NewTopic documentProcessedResponseDltTopic(
            @Value(
                    "${app.messaging.kafka.topics.processed-response-dlt}"
            )
            String topic
    ) {
        return createTopic(topic);
    }

    @Bean
    public NewTopic documentProcessingResultTopic(
            @Value(
                    "${app.messaging.kafka.topics.processing-result}"
            )
            String topic
    ) {
        return createTopic(topic);
    }

    @Bean
    public NewTopic documentProcessingResultDltTopic(
            @Value(
                    "${app.messaging.kafka.topics.processing-result-dlt}"
            )
            String topic
    ) {
        return createTopic(topic);
    }

    private NewTopic createTopic(String topic) {
        return TopicBuilder
                .name(topic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
