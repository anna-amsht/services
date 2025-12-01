package com.innowise.userservice.dao.implementation;

import com.innowise.userservice.dao.interfaces.UserDao;
import com.innowise.userservice.entities.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional
public class UserDaoImplementation implements UserDao {

    private static final Logger logger = LoggerFactory.getLogger(UserDaoImplementation.class);

    private final EntityManager entityManager;

    @Override
    public void create(UserEntity userEntity) {
        logger.debug("Creating user with auto-generated ID");
        entityManager.persist(userEntity);
    }

    @Override
    public Optional<UserEntity> getById(Long id) {
        return Optional.ofNullable(entityManager.find(UserEntity.class, id));
    }

    @Override
    public Optional<UserEntity> getByEmail(String email) {
        try {
            UserEntity user = entityManager.createQuery("SELECT user FROM UserEntity user WHERE user.email = :email", UserEntity.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return Optional.of(user);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public Page<UserEntity> getAll(Pageable pageable) {
        Long total = entityManager.createQuery("SELECT COUNT(u) FROM UserEntity u", Long.class)
                .getSingleResult();

        List<UserEntity> content = entityManager.createQuery("SELECT u FROM UserEntity u", UserEntity.class)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public void update(Long id, UserEntity updatedUser) {
        UserEntity user = entityManager.find(UserEntity.class, id);
        if (user != null) {
            user.setName(updatedUser.getName());
            user.setSurname(updatedUser.getSurname());
            user.setBirthdate(updatedUser.getBirthdate());
            user.setEmail(updatedUser.getEmail());
            entityManager.flush(); // Ensure changes are persisted
        }
    }

    @Override
    public void delete(Long id) {
        getById(id).ifPresent(entityManager::remove);
    }

}