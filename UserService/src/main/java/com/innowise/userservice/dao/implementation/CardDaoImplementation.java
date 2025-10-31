package com.innowise.userservice.dao.implementation;

import com.innowise.userservice.dao.interfaces.CardDao;
import com.innowise.userservice.entities.CardEntity;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional
public class CardDaoImplementation implements CardDao {
    private final EntityManager entityManager;
    @Override
    public void create(CardEntity cardEntity) {
        entityManager.persist(cardEntity);
    }

    @Override
    public Optional<CardEntity> getById(Long id) {
        return Optional.ofNullable(entityManager.find(CardEntity.class, id));
    }

    @Override
    public Page<CardEntity> getAll(Pageable pageable) {
        Long total = entityManager.createQuery("SELECT COUNT(card) FROM CardEntity card", Long.class)
                .getSingleResult();

        List<CardEntity> content = entityManager.createQuery("SELECT card FROM CardEntity card", CardEntity.class)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public void delete(Long id) {
        getById(id).ifPresent(entityManager::remove);
    }

    @Override
    public void update(Long id, CardEntity updatedCard) {
        CardEntity existing = entityManager.find(CardEntity.class, id);
        if (existing != null) {
            existing.setNumber(updatedCard.getNumber());
            existing.setHolder(updatedCard.getHolder());
            existing.setExpirationDate(updatedCard.getExpirationDate());
        }
    }

    @Override
    public boolean existsByNumber(String number) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(c) FROM CardEntity c WHERE c.number = :number", Long.class)
                .setParameter("number", number)
                .getSingleResult();
        return count > 0;
    }
}
