package com.innowise.authservice.dao.implementation;

import com.innowise.authservice.dao.interfaces.RefreshTokenDao;
import com.innowise.authservice.entities.RefreshTokenEntity;
import com.innowise.authservice.entities.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional
public class RefreshTokenDaoImplementation implements RefreshTokenDao {

    private final EntityManager entityManager;

    @Override
    public void save(RefreshTokenEntity refreshToken) {
        if (refreshToken.getId() == null) {
            entityManager.persist(refreshToken);
        } else {
            entityManager.merge(refreshToken);
        }
    }

    @Override
    public Optional<RefreshTokenEntity> getByToken(String token) {
        try {
            RefreshTokenEntity refreshToken = entityManager.createQuery(
                    "SELECT rt FROM RefreshTokenEntity rt WHERE rt.token = :token", 
                    RefreshTokenEntity.class)
                    .setParameter("token", token)
                    .getSingleResult();
            return Optional.of(refreshToken);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<RefreshTokenEntity> getByUser(UserEntity user) {
        try {
            RefreshTokenEntity refreshToken = entityManager.createQuery(
                    "SELECT rt FROM RefreshTokenEntity rt WHERE rt.user = :user", 
                    RefreshTokenEntity.class)
                    .setParameter("user", user)
                    .getSingleResult();
            return Optional.of(refreshToken);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public void deleteByUser(UserEntity user) {
        entityManager.createQuery(
                "DELETE FROM RefreshTokenEntity rt WHERE rt.user = :user")
                .setParameter("user", user)
                .executeUpdate();
    }

    @Override
    public void deleteByUserId(Long userId) {
        entityManager.createQuery(
                "DELETE FROM RefreshTokenEntity rt WHERE rt.user.id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
    }

    @Override
    public void delete(RefreshTokenEntity refreshToken) {
        if (entityManager.contains(refreshToken)) {
            entityManager.remove(refreshToken);
        } else {
            entityManager.remove(entityManager.merge(refreshToken));
        }
    }
}