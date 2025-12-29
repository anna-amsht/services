package com.innowise.orderservice.kafka;

import com.innowise.orderservice.dto.models.CreateOrderEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class OrderEventProducer {

    private static final String TOPIC = "create-order-topic";
    private final KafkaTemplate<String, CreateOrderEventDto> kafkaTemplate;

    public void sendCreateOrderEvent(CreateOrderEventDto event) {
        log.info("Sending CREATE_ORDER event for orderId: {}", event.getOrderId());
        kafkaTemplate.send(TOPIC, event.getOrderId().toString(), event);
    }
}
