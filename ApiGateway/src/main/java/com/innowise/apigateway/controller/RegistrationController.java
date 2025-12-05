package com.innowise.apigateway.controller;

import com.innowise.apigateway.dto.RegisterRequestDto;
import com.innowise.apigateway.dto.RegistrationResponseDto;
import com.innowise.apigateway.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/register")
@RequiredArgsConstructor
@Slf4j
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping
    public Mono<ResponseEntity<RegistrationResponseDto>> register(@RequestBody RegisterRequestDto request) {
        log.info("Registration request received for username: {}", request.getUsername());
        
        return registrationService.register(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(error -> {
                    log.error("Registration failed: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}
