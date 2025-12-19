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
        
        PaymentDto paymentDto = PaymentDto.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .paymentAmount(BigDecimal.ZERO)
                .build();
        
        PaymentDto createdPayment = paymentService.create(paymentDto);
        log.info("Created payment with ID: {} for orderId: {}", createdPayment.getId(), event.getOrderId());
    }
}
