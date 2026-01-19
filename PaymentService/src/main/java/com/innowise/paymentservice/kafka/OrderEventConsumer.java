package com.innowise.paymentservice.kafka;

import com.innowise.paymentservice.dto.models.CreateOrderEventDto;
import com.innowise.paymentservice.dto.models.PaymentDto;
import com.innowise.paymentservice.service.interfaces.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "create-order-topic", groupId = "payment-service-group")
    public void handleCreateOrderEvent(CreateOrderEventDto event) {
        log.info("Received CREATE_ORDER event for orderId: {}, userId: {}, status: {}", 
                event.getOrderId(), event.getUserId(), event.getStatus());
        
        // Note: This creates a payment record with zero amount
        // The actual payment will be created manually via POST /api/v1/payments
        // This just creates a placeholder payment entry
        log.debug("Skipping automatic payment creation for order {}, awaiting manual payment", event.getOrderId());
    }
}
