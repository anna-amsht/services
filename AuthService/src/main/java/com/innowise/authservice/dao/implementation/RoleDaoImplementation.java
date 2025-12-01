package com.innowise.authservice.dao.implementation;

import com.innowise.authservice.dao.interfaces.RoleDao;
import com.innowise.authservice.entities.RoleEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional
public class RoleDaoImplementation implements RoleDao {

    private final EntityManager entityManager;

    @Override
    public Optional<RoleEntity> getByName(String name) {
        try {
            RoleEntity role = entityManager.createQuery(
                    "SELECT r FROM RoleEntity r WHERE r.name = :name", RoleEntity.class)
                    .setParameter("name", name)
                    .getSingleResult();
            return Optional.of(role);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}