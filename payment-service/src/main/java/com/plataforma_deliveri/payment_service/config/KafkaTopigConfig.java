package com.plataforma_deliveri.payment_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopigConfig {

    public static final String PAYMENT_EVENTS_TOPIC = "payment-events";

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(PAYMENT_EVENTS_TOPIC)
            .partitions(1)
            .replicas(1)
            .build();
    }
}
