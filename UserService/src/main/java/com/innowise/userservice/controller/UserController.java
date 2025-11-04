package com.innowise.userservice.controller;

import com.innowise.userservice.dto.models.UserDto;
import com.innowise.userservice.exceptions.NotFoundException;
import com.innowise.userservice.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDto> create(@RequestBody UserDto userDto) {
        UserDto createdUser = userService.create(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable Long id) {
        Optional<UserDto> user = userService.getById(id);
        return user.map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }

    @GetMapping(params = "email")
    public ResponseEntity<UserDto> getByEmail(@RequestParam String email){
        Optional<UserDto> user = userService.getByEmail(email);
        return user.map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));
    }

    @GetMapping
    public ResponseEntity<Page<UserDto>> getAll(Pageable pageable) {
        Page<UserDto> users = userService.getAll(pageable);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> update(@PathVariable Long id,  @RequestBody UserDto userDto) {
        UserDto updated = userService.update(id, userDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }


}
