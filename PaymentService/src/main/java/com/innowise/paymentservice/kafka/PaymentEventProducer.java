package com.innowise.paymentservice.kafka;

import com.innowise.paymentservice.dto.models.CreatePaymentEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private static final String TOPIC = "create-payment-topic";
    private final KafkaTemplate<String, CreatePaymentEventDto> kafkaTemplate;

    public void sendCreatePaymentEvent(CreatePaymentEventDto event) {
        log.info("Sending CREATE_PAYMENT event for paymentId: {}, orderId: {}", 
                event.getPaymentId(), event.getOrderId());
        kafkaTemplate.send(TOPIC, event.getPaymentId().toString(), event);
    }
}
