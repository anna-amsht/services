package com.innowise.userservice.dao.interfaces;

import com.innowise.userservice.entities.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface UserDao {
    void create(UserEntity userEntity);
    void createWithId(UserEntity userEntity);
    Optional<UserEntity> getById(Long id);
    Optional<UserEntity> getByEmail(String email);
    Page<UserEntity> getAll(Pageable pageable);
    void update(Long id, UserEntity updatedUser);
    void delete(Long id);
}
