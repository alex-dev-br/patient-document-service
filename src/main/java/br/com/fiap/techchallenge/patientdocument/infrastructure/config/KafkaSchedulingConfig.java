package br.com.fiap.techchallenge.patientdocument.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(
        name = "app.messaging.kafka.enabled",
        havingValue = "true"
)
public class KafkaSchedulingConfig {
}
