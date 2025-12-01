package com.innowise.authservice.dao.implementation;

import com.innowise.authservice.dao.interfaces.UserDao;
import com.innowise.authservice.entities.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional
public class UserDaoImplementation implements UserDao {

    private static final Logger logger = LoggerFactory.getLogger(UserDaoImplementation.class);

    private final EntityManager entityManager;

    @Override
    public void create(UserEntity userEntity) {
        logger.debug("Creating user in AuthService");
        entityManager.persist(userEntity);
        logger.debug("Successfully created user in AuthService with ID: {}", userEntity.getId());
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

    @Override
    public void delete(Long id) {
        logger.debug("Deleting user with ID: {}", id);
        getById(id).ifPresent(entityManager::remove);
        logger.debug("Successfully deleted user with ID: {}", id);
    }
}