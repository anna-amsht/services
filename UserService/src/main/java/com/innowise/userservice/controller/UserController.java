package com.innowise.userservice.controller;

import com.innowise.userservice.dto.models.UserDto;
import com.innowise.userservice.exceptions.NotFoundException;
import com.innowise.userservice.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.util.Optional;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;


    @PostMapping("/with-id")
    public ResponseEntity<UserDto> createWithId(@RequestBody UserDto userDto) {
        logger.info("Creating user with predefined ID: {}", userDto.getId());
        try {
            UserDto createdUser = userService.createWithId(userDto);
            logger.info("Successfully created user with ID: {}", userDto.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (Exception e) {
            logger.error("Error creating user with ID {}: {}", userDto.getId(), e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("#id == authentication.details['userId'] or hasRole('ADMIN')")
    public ResponseEntity<UserDto> getById(@PathVariable Long id) {
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