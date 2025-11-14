package com.innowise.userservice.service.implementation;

import com.innowise.userservice.dao.interfaces.CardDao;
import com.innowise.userservice.dao.interfaces.UserDao;
import com.innowise.userservice.dto.mappers.CardMapper;
import com.innowise.userservice.dto.models.CardDto;
import com.innowise.userservice.entities.CardEntity;
import com.innowise.userservice.entities.UserEntity;
import com.innowise.userservice.exceptions.DuplicateException;
import com.innowise.userservice.exceptions.NotFoundException;
import com.innowise.userservice.service.interfaces.CardService;
import com.innowise.userservice.service.interfaces.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

@Service
@Transactional
@Validated
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardDao cardDao;
    private final CardMapper cardMapper;
    private final UserDao userDao;

    @Override
    public CardDto create( CardDto cardDto) {

        if (cardDao.existsByNumber(cardDto.getNumber())) {
            throw new DuplicateException("Card with this number already exists");
        }
        CardEntity cardEntity = cardMapper.toEntity(cardDto);
        UserEntity user = userDao.getById(cardDto.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));
        cardEntity.setUser(user);

        cardDao.create(cardEntity);
        return cardMapper.toDto(cardEntity);
    }

    @Override
    @Cacheable(value = "cards", key = "#id")
    public Optional<CardDto> getById(Long id) {
        return cardDao.getById(id).map(cardMapper::toDto);
    }

    @Override
    public Page<CardDto> getAll(Pageable pageable) {
        return cardDao.getAll(pageable).map(cardMapper::toDto);
    }

    @Override
    @CachePut(value = "cards", key = "#id")
    public CardDto update(Long id, CardDto updatedCardDto) {
        CardEntity existingCard = cardDao.getById(id)
                .orElseThrow(() -> new NotFoundException("Card not found"));

        UserEntity user = existingCard.getUser();

        CardEntity updatedCardEntity = cardMapper.toEntity(updatedCardDto);
        updatedCardEntity.setUser(user);

        cardDao.update(id, updatedCardEntity);
        return cardMapper.toDto(updatedCardEntity);
    }

    @Override
    @CacheEvict(value = "cards", key = "#id")
    public void delete(Long id) {
        if (cardDao.getById(id).isEmpty()) {
            throw new NotFoundException("Card not found");
        }
        cardDao.delete(id);
    }
}