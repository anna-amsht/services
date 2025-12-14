package com.innowise.apigateway.service;

import com.innowise.apigateway.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    @Qualifier("authServiceClient")
    private final WebClient authServiceClient;

    @Qualifier("userServiceClient")
    private final WebClient userServiceClient;

    public Mono<RegistrationResponseDto> register(RegisterRequestDto request) {
        log.info("Starting registration process for username: {}", request.getUsername());

        AuthRegisterRequestDto authRequest = new AuthRegisterRequestDto(
                request.getUsername(),
                request.getPassword(),
                request.getEmail()
        );

        return authServiceClient.post()
                .uri("/api/v1/auth/register")
                .bodyValue(authRequest)
                .retrieve()
                .onStatus(status -> status.equals(HttpStatus.CONFLICT), response -> {
                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("User already exists")
                            .flatMap(body -> Mono.error(new RuntimeException("DUPLICATE_USER: " + body)));
                })
                .bodyToMono(AuthResponseDto.class)
                .flatMap(authResponse -> {
                    log.info("Credentials created successfully for userId: {}", authResponse.getUserId());

                    UserCreateRequestDto userRequest = new UserCreateRequestDto(
                            request.getName(),
                            request.getSurname(),
                            request.getBirthdate()
                    );

                    return userServiceClient.post()
                            .uri("/api/v1/users")
                            .header("Authorization", "Bearer " + authResponse.getAccessToken())
                            .bodyValue(userRequest)
                            .retrieve()
                            .onStatus(status -> !status.is2xxSuccessful(), response -> {
                                return response.bodyToMono(String.class)
                                        .flatMap(body -> Mono.error(new RuntimeException("UserService error: " + body)));
                            })
                            .bodyToMono(Void.class)
                            .then(Mono.just(authResponse))
                            .onErrorResume(error -> {
                                log.error("Failed to create user profile for userId: {}. Initiating rollback.", 
                                        authResponse.getUserId(), error);
                                
                                return rollbackAuthCredentials(authResponse.getUserId())
                                        .then(Mono.error(new RuntimeException("User registration failed: " + error.getMessage())));
                            });
                })
                .map(authResponse -> {
                    log.info("Registration completed successfully for userId: {}", authResponse.getUserId());
                    return new RegistrationResponseDto(
                            "User registered successfully",
                            authResponse.getUserId(),
                            authResponse.getAccessToken(),
                            authResponse.getRefreshToken()
                    );
                });
    }

    private Mono<Void> rollbackAuthCredentials(Long userId) {
        log.warn("Rolling back credentials for userId: {}", userId);
        
        return authServiceClient.delete()
                .uri("/api/v1/auth/users/{userId}", userId)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("Rollback completed for userId: {}", userId))
                .doOnError(e -> log.error("Rollback failed for userId: {}", userId, e))
                .onErrorResume(e -> Mono.empty());
    }
}
