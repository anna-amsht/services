package com.innowise.paymentservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic createOrderTopic() {
        return TopicBuilder.name("create-order-topic")
                .partitions(1)
                .replicas(2)
                .config("min.insync.replicas", "1")
                .build();
    }

    @Bean
    public NewTopic createPaymentTopic() {
        return TopicBuilder.name("create-payment-topic")
                .partitions(1)
                .replicas(2)
                .config("min.insync.replicas", "1")
                .build();
    }
}
