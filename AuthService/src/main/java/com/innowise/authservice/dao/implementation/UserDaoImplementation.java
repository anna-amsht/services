package com.innowise.authservice.dao.implementation;

import com.innowise.authservice.dao.interfaces.UserDao;
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
public class UserDaoImplementation implements UserDao {

    private final EntityManager entityManager;

    @Override
public void create(UserEntity userEntity) {
        entityManager.persist(userEntity);
    }

    @Override
    public Optional<UserEntity> getById(Long id) {
        return Optional.ofNullable(entityManager.find(UserEntity.class, id));
    }

    @Override
    public Optional<UserEntity> getByUsername(String username) {
        return Optional.ofNullable(entityManager.find(UserEntity.class, username));
    }

    @Override
    public Optional<UserEntity> getByEmail(String email) {
        try {
            UserEntity user = entityManager.createQuery(
                    "SELECT u FROM UserEntity u WHERE u.email = :email", UserEntity.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return Optional.of(user);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<UserEntity> getByEmailWithRoles(String email) {
        try {
            UserEntity user = entityManager.createQuery(
                    "SELECT u FROM UserEntity u LEFT JOIN FETCH u.roles WHERE u.email = :email", UserEntity.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return Optional.of(user);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean update(UserEntity userEntity) { // Изменено на boolean
        try {
            entityManager.merge(userEntity);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}