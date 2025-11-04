package com.innowise.userservice.dto.models;

import com.innowise.userservice.entities.UserEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;


import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardDto {

    private Long id;

    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{4}-\\d{4}-\\d{4}$")
    private String number;

    @NotBlank
    @Size(max = 100)
    private String holder;

    @NotNull
    @FutureOrPresent
    private LocalDate expirationDate;

    @NotNull
    private Long userId;

}
