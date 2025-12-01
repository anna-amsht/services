package com.innowise.authservice.dao.interfaces;

import com.innowise.authservice.entities.RefreshTokenEntity;
import com.innowise.authservice.entities.UserEntity;
import java.util.Optional;

public interface RefreshTokenDao {
    void save(RefreshTokenEntity refreshToken);
    Optional<RefreshTokenEntity> getByToken(String token);
    Optional<RefreshTokenEntity> getByUser(UserEntity user);
    void deleteByUser(UserEntity user);
    void delete(RefreshTokenEntity refreshToken);
}