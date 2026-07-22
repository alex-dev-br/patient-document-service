package br.com.fiap.techchallenge.patientdocument.infrastructure.config;

import br.com.fiap.techchallenge.patientdocument.application.exception.ResourceNotFoundException;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        name = "app.messaging.kafka.enabled",
        havingValue = "true"
)
public class KafkaErrorHandlingConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate,
            @Value(
                    "${app.messaging.kafka.topics.processed-response-dlt}"
            )
            String legacyDeadLetterTopic,
            @Value(
                    "${app.messaging.kafka.topics.processing-result}"
            )
            String processingResultTopic,
            @Value(
                    "${app.messaging.kafka.topics.processing-result-dlt}"
            )
            String processingResultDeadLetterTopic,
            @Value(
                    "${app.messaging.kafka.listener-retry-interval}"
            )
            long retryInterval,
            @Value(
                    "${app.messaging.kafka.listener-max-retries}"
            )
            long maxRetries
    ) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(
                                        resolveDeadLetterTopic(
                                                record.topic(),
                                                processingResultTopic,
                                                processingResultDeadLetterTopic,
                                                legacyDeadLetterTopic
                                        ),
                                        record.partition()
                                )
                );

        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(
                        recoverer,
                        new FixedBackOff(
                                retryInterval,
                                maxRetries
                        )
                );

        /*
         * Erros de contrato, correlação, autorização de
         * paciente ou recurso inexistente são definitivos.
         * Repetir a mesma mensagem não corrigiria os dados.
         */
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                ResourceNotFoundException.class
        );

        return errorHandler;
    }

    private String resolveDeadLetterTopic(
            String sourceTopic,
            String processingResultTopic,
            String processingResultDeadLetterTopic,
            String legacyDeadLetterTopic
    ) {
        if (processingResultTopic.equals(sourceTopic)) {
            return processingResultDeadLetterTopic;
        }

        return legacyDeadLetterTopic;
    }
}
