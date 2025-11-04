package com.innowise.userservice.service.implementation;

import com.innowise.userservice.dao.interfaces.CardDao;
import com.innowise.userservice.dao.interfaces.UserDao;
import com.innowise.userservice.dto.mappers.CardMapper;
import com.innowise.userservice.dto.models.CardDto;
import com.innowise.userservice.entities.CardEntity;
import com.innowise.userservice.entities.UserEntity;
import com.innowise.userservice.exceptions.DuplicateException;
import com.innowise.userservice.exceptions.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock
    private CardDao cardDao;

    @Mock
    private UserDao userDao;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private CardServiceImpl cardService;

    private CardDto cardDto;
    private CardEntity cardEntity;
    private UserEntity userEntity;

    @BeforeEach
    void setUp() {
        userEntity = new UserEntity();
        userEntity.setId(1L);

        cardEntity = new CardEntity(
                1L,
                "0001-0002-0003-0004",
                "John Doe",
                LocalDate.now().plusYears(1),
                userEntity
        );

        cardDto = CardDto.builder()
                .id(1L)
                .number("0001-0002-0003-0004")
                .holder("John Doe")
                .expirationDate(LocalDate.now().plusYears(1))
                .userId(1L)
                .build();
    }
    @Test
    void testCreate() {
        when(cardDao.existsByNumber(cardDto.getNumber())).thenReturn(false);
        when(userDao.getById(1L)).thenReturn(Optional.of(userEntity));
        when(cardMapper.toEntity(cardDto)).thenReturn(cardEntity);
        when(cardMapper.toDto(cardEntity)).thenReturn(cardDto);

        CardDto result = cardService.create(cardDto);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(cardDto.getNumber(), result.getNumber());
        verify(cardDao).create(cardEntity);
    }

    @Test
    void testCreateDuplicateNumberExceprion() {
        when(cardDao.existsByNumber(cardDto.getNumber())).thenReturn(true);

        Assertions.assertThrows(DuplicateException.class, () -> cardService.create(cardDto));
       verify(cardDao, Mockito.never()).create(Mockito.any());
    }

    @Test
    void testCreateNotFoundUserExceprion() {
        when(cardDao.existsByNumber(cardDto.getNumber())).thenReturn(false);
        when(userDao.getById(1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(NotFoundException.class, () -> cardService.create(cardDto));
    }

    @Test
    void testGetById() {
        when(cardDao.getById(1L)).thenReturn(Optional.of(cardEntity));
        when(cardMapper.toDto(cardEntity)).thenReturn(cardDto);

        Optional<CardDto> result = cardService.getById(1L);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(cardDto.getId(), result.get().getId());
        Assertions.assertEquals(cardDto.getNumber(), result.get().getNumber());
    }

    @Test
    void testGetAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CardEntity> page = new PageImpl<>(List.of(cardEntity), pageable, 1);

        when(cardDao.getAll(pageable)).thenReturn(page);
        when(cardMapper.toDto(cardEntity)).thenReturn(cardDto);

        Page<CardDto> result = cardService.getAll(pageable);

        Assertions.assertEquals(1, result.getTotalElements());
        Assertions.assertEquals(cardDto.getNumber(), result.getContent().get(0).getNumber());
    }

    @Test
    void testDelete() {
        Long cardId = 1L;
        CardEntity card = new CardEntity();
        card.setId(cardId);

        when(cardDao.getById(cardId)).thenReturn(Optional.of(card));
        cardService.delete(cardId);
        verify(cardDao).delete(cardId);

    }

    @Test
    void testDeleteCardNotFoundException() {
        Long missingId = 999L;

        when(cardDao.getById(missingId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> cardService.delete(missingId)
        );

        assertEquals("Card not found", exception.getMessage());
    }
}