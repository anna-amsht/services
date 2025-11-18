package com.innowise.userservice.controller;

import com.innowise.userservice.dto.models.CardDto;
import com.innowise.userservice.exceptions.NotFoundException;
import com.innowise.userservice.service.interfaces.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {
    private final CardService cardService;

    @PostMapping
    @PreAuthorize("#cardDto.userId == authentication.details['userId'] or hasRole('ADMIN')")
    public ResponseEntity<CardDto> createCard(@RequestBody CardDto cardDto) {
        CardDto createdCard = cardService.create(cardDto);
        return ResponseEntity.ok(createdCard);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@cardSecurityService.isCardOwner(#id, authentication) or hasRole('ADMIN')")
    public ResponseEntity<CardDto> getCardById(@PathVariable Long id) {
        Optional<CardDto> card = cardService.getById(id);
        return card.map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Card not found with id: " + id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CardDto>> getAllCards(Pageable pageable) {
        Page<CardDto> cards = cardService.getAll(pageable);
        return ResponseEntity.ok(cards);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@cardSecurityService.isCardOwner(#id, authentication) or hasRole('ADMIN')")
    public ResponseEntity<CardDto> updateCard(@PathVariable Long id, @RequestBody CardDto cardDto) {
        CardDto updatedCard = cardService.update(id, cardDto);
        return ResponseEntity.ok(updatedCard);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@cardSecurityService.isCardOwner(#id, authentication) or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        cardService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}