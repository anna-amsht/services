package com.innowise.authservice.dao.interfaces;

import com.innowise.authservice.entities.UserEntity;
import java.util.Optional;

public interface UserDao {
    void create(UserEntity userEntity);
    Optional<UserEntity> getById(Long id);
    Optional<UserEntity> getByUsername(String username);
    Optional<UserEntity> getByEmail(String email);
    Optional<UserEntity> getByEmailWithRoles(String email);
    boolean update(UserEntity userEntity); // Изменено на boolean
    void delete(Long id);
}