package com.innowise.orderservice.dto.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {

    private Long id;

    @NotNull
    private Long userId;

    @NotBlank
    @Size(max = 50)
    private String status;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime creationDate;

    @NotEmpty(message = "Order must have at least one item")
    private List<@Valid OrderItemDto> orderItems;
}

