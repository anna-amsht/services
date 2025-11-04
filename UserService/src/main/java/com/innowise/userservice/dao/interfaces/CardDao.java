package com.innowise.userservice.dao.interfaces;

import com.innowise.userservice.entities.CardEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CardDao {
    void create(CardEntity cardEntity);
    Optional<CardEntity> getById(Long id);
    Page<CardEntity> getAll(Pageable pageable);
    void delete(Long id);
    void update(Long id, CardEntity updatedCard);
    boolean existsByNumber(String number);

}
