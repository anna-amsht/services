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

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 100)
    private String surname;

    @NotNull
    @Past
    private LocalDate birthdate;

    @NotBlank
    @Email
    private String email;

    private List<CardDto> cards;

}
