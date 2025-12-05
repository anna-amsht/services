package com.innowise.apigateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponseDto {
    private String message;
    private Long userId;
    private String accessToken;
    private String refreshToken;
}
