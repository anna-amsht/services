package com.innowise.paymentservice.dto.models;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {

    private Long id;

    @NotNull
    private Long orderId;

    @NotNull
    private Long userId;

    @Size(max = 50, message = "Status must be at most 50 characters")
    private String status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime timestamp;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "Payment amount must be positive")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal paymentAmount;
}

