package com.innowise.userservice.service.interfaces;

import com.innowise.userservice.dto.models.CardDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CardService {
    CardDto create(@Valid CardDto cardDto);
    Optional<CardDto> getById(Long id);
    Page<CardDto> getAll(Pageable pageable);
    CardDto update(Long id, @Valid CardDto updatedCardDto);
    void delete(Long id);
}
