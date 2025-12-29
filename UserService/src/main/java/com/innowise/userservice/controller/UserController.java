package com.innowise.userservice.controller;

import com.innowise.userservice.dto.models.UserDto;
import com.innowise.userservice.dto.models.UserProfileCreateRequest;
import com.innowise.userservice.exceptions.DuplicateException;
import com.innowise.userservice.exceptions.NotFoundException;
import com.innowise.userservice.service.interfaces.UserService;
import com.innowise.userservice.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.Page;

import java.util.Optional;
import jakarta.validation.ConstraintViolationException;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping

    public ResponseEntity<?> createUser(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UserProfileCreateRequest request) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        
        if (!jwtUtil.validateJwtToken(token)) {
            return ResponseEntity.badRequest().body("Invalid JWT token");
        }

        String email = jwtUtil.getUserNameFromJwtToken(token);
        Long userId = jwtUtil.getUserIdFromJwtToken(token);

        logger.info("Creating user profile for email: {} with userId: {}", email, userId);

        try {
            UserDto userDto = new UserDto();
            userDto.setId(userId);
            userDto.setEmail(email);
            userDto.setName(request.getName());
            userDto.setSurname(request.getSurname());
            userDto.setBirthdate(request.getBirthdate());

            UserDto createdUser = userService.createFromCredentials(userDto);
            logger.info("Successfully created user profile with ID: {}", createdUser.getId());

            return ResponseEntity.ok(createdUser);
        }  catch (DuplicateException e) {
            logger.error("Duplicate user error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (ConstraintViolationException e) {
            logger.error("Validation error creating user profile: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Validation failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating user profile: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to create user profile: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("#id == authentication.details['userId'] or hasRole('ADMIN')")
    public ResponseEntity<UserDto> getById(@PathVariable Long id) {
        Optional<UserDto> user = userService.getById(id);
        return user.map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }

    @GetMapping("/internal/{id}")
    public ResponseEntity<UserDto> getByIdInternal(
            @PathVariable Long id,
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken) {
        if (!"${INTERNAL_SERVICE_TOKEN}".equals(internalToken) && !"internal-service-secret".equals(internalToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Optional<UserDto> user = userService.getById(id);
        return user.map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }

    @GetMapping(params = "email")
    @PreAuthorize("hasRole('ADMIN')") // Only admins can search by email for privacy reasons
    public ResponseEntity<UserDto> getByEmail(@RequestParam String email){
        Optional<UserDto> user = userService.getByEmail(email);
        return user.map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") // Only admins can get all users
    public ResponseEntity<Page<UserDto>> getAll(Pageable pageable) {
        Page<UserDto> users = userService.getAll(pageable);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    @PreAuthorize("#id == authentication.details['userId'] or hasRole('ADMIN')")
    public ResponseEntity<UserDto> update(@PathVariable Long id,  @RequestBody UserDto userDto) {
        UserDto updated = userService.update(id, userDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("#id == authentication.details['userId'] or hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}