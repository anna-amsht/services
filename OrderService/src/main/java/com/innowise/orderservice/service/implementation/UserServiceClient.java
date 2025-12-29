package com.innowise.orderservice.service.implementation;

import com.innowise.orderservice.client.UserClient;
import com.innowise.orderservice.dto.models.UserDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final UserClient userClient;

    @Value("${internal.service.token:internal-service-secret}")
    private String internalToken;

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    public UserDto getUserById(Long userId) {
        log.info("Fetching user info for userId: {}", userId);
        return userClient.getUserById(userId, internalToken);
    }

    private UserDto getUserByIdFallback(Long userId, Exception ex) {
        log.error("Failed to fetch user info for userId: {}. Error: {}", userId, ex.getMessage());
        return UserDto.builder()
                .id(userId)
                .name("Unknown")
                .surname("User")
                .build();
    }
}
