package com.innowise.userservice.dto.models;

import jakarta.validation.constraints.*;
import lombok.*;


import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;

    private String name;

    private String surname;

    private LocalDate birthdate;

    @NotBlank
    @Email
    private String email;

    private List<CardDto> cards;

}