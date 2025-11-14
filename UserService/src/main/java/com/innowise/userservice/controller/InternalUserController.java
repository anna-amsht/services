package com.innowise.userservice.controller;

import com.innowise.userservice.dto.models.InternalUserCreateRequest;
import com.innowise.userservice.dto.models.UserDto;
import com.innowise.userservice.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private static final Logger logger = LoggerFactory.getLogger(InternalUserController.class);

    @Value("${internal.token:internal-secret-token}")
    private String internalToken;

    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> createInternalUser(@RequestHeader(value = "X-Internal-Token", required = false) String token,
                                               @RequestBody InternalUserCreateRequest request) {
        logger.info("Received token: {}", token);
        logger.info("Expected token: {}", internalToken);
        
        if (token == null || token.isEmpty()) {
            logger.warn("No token provided in request");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No token provided");
        }
        
        if (!internalToken.equals(token)) {
            logger.warn("Unauthorized attempt to access internal endpoint. Expected: {}, Received: {}", internalToken, token);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        try {
            logger.info("Creating internal user with ID: {} and email: {}", request.getId(), request.getEmail());
            
            UserDto userDto = UserDto.builder()
                    .id(request.getId())
                    .email(request.getEmail())
                    .name(null)
                    .surname(null)
                    .birthdate(null)
                    .build();
            
            UserDto createdUser = userService.createWithId(userDto);
            logger.info("Successfully created internal user with ID: {}", createdUser.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (Exception e) {
            logger.error("Error creating internal user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create user: " + e.getMessage());
        }
    }
}