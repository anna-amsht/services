package com.innowise.orderservice.dto.models;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String name;
    private String surname;
    private LocalDate birthdate;
    private String email;
}
