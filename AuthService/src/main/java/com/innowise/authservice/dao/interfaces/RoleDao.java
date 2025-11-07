package com.innowise.authservice.dao.interfaces;

import com.innowise.authservice.entities.RoleEntity;
import java.util.Optional;

public interface RoleDao {
    Optional<RoleEntity> getByName(String name);
}