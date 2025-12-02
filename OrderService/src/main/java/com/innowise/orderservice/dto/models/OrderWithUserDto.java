package com.innowise.orderservice.dto.models;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderWithUserDto {
    private OrderDto order;
    private UserDto user;
}
