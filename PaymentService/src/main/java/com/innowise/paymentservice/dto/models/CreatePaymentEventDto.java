package com.innowise.paymentservice.dto.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentEventDto {
    private Long paymentId;
    private Long orderId;
    private Long userId;
    private String status;
    private LocalDateTime timestamp;
    private BigDecimal paymentAmount;
}
