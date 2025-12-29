package com.innowise.orderservice.kafka;

import com.innowise.orderservice.dto.models.CreatePaymentEventDto;
import com.innowise.orderservice.service.interfaces.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentEventConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "create-payment-topic", groupId = "order-service-group")
    public void handleCreatePaymentEvent(CreatePaymentEventDto event) {
        log.info("Received CREATE_PAYMENT event for orderId: {}, paymentId: {}, status: {}", 
                event.getOrderId(), event.getPaymentId(), event.getStatus());
        
        String orderStatus = mapPaymentStatusToOrderStatus(event.getStatus());
        orderService.updateOrderStatus(event.getOrderId(), orderStatus);
        
        log.info("Updated order {} status to: {}", event.getOrderId(), orderStatus);
    }
    
    private String mapPaymentStatusToOrderStatus(String paymentStatus) {
        return switch (paymentStatus.toUpperCase()) {
            case "COMPLETED", "SUCCESS" -> "PAID";
            case "FAILED", "REJECTED" -> "PAYMENT_FAILED";
            case "PENDING" -> "PENDING_PAYMENT";
            default -> "PROCESSING";
        };
    }
}
