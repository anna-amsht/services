package com.innowise.userservice.service.interfaces;

import com.innowise.userservice.dto.models.UserDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserService {
    UserDto create(@Valid UserDto userDto);
    UserDto createWithId(@Valid UserDto userDto);
    Optional<UserDto> getById(Long id);
    Optional<UserDto> getByEmail(String email);
    Page<UserDto> getAll(Pageable pageable);
    UserDto update(Long id, @Valid UserDto updatedUserDto);
    void delete(Long id);
}
