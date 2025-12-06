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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/register")
@RequiredArgsConstructor
@Slf4j
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping
    public Mono<ResponseEntity<?>> register(@RequestBody RegisterRequestDto request) {
        log.info("Registration request received for username: {}", request.getUsername());
        
        return registrationService.register(request)
                .<ResponseEntity<?>>map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(error -> {
                    log.error("Registration error: {}", error.getMessage(), error);
                    
                    if (error.getMessage().contains("DUPLICATE_USER")) {
                        String message = error.getMessage().substring(error.getMessage().indexOf(":") + 2);
                        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", message)));
                    }
                    
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Registration failed")));
                });
    }
}
