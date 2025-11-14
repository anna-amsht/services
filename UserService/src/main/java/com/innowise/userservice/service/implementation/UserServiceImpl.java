package com.innowise.userservice.service.implementation;

import com.innowise.userservice.dao.interfaces.UserDao;
import com.innowise.userservice.dto.mappers.UserMapper;
import com.innowise.userservice.dto.models.UserDto;
import com.innowise.userservice.entities.UserEntity;
import com.innowise.userservice.exceptions.DuplicateException;
import com.innowise.userservice.exceptions.NotFoundException;
import com.innowise.userservice.service.interfaces.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

@Service
@Transactional
@Validated
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    
    private final UserDao userDao;
    private final UserMapper userMapper;

    @Override
    public UserDto create(UserDto userDto) {
        logger.info("Creating user with auto-generated ID");
        if (userDao.getByEmail(userDto.getEmail()).isPresent()) {
            throw new DuplicateException("User with email '" + userDto.getEmail() + "' already exists");
        }
        UserEntity userEntity = userMapper.toEntity(userDto);
        userDao.create(userEntity);
        logger.info("Successfully created user with auto-generated ID: {}", userEntity.getId());
        return userMapper.toDto(userEntity);
    }

    @Override
    public UserDto createWithId(UserDto userDto) {
        logger.info("Creating user with predefined ID: {}", userDto.getId());
        if (userDao.getByEmail(userDto.getEmail()).isPresent()) {
            throw new DuplicateException("User with email '" + userDto.getEmail() + "' already exists");
        }
        UserEntity userEntity = userMapper.toEntity(userDto);
        userDao.createWithId(userEntity);
        // After creating with ID, we need to fetch the entity to ensure it's properly managed
        Optional<UserEntity> createdEntity = userDao.getById(userEntity.getId());
        if (createdEntity.isPresent()) {
            logger.info("Successfully created user with predefined ID: {}", userDto.getId());
            return userMapper.toDto(createdEntity.get());
        } else {
            logger.error("Failed to retrieve created user with ID: {}", userDto.getId());
            throw new RuntimeException("Failed to create user with ID: " + userDto.getId());
        }
    }

    @Override
    @Cacheable(value ="users", key ="#id")
    public Optional<UserDto> getById(Long id) {
        return userDao.getById(id)
                .map(userMapper::toDto);
    }

    @Override
    public Optional<UserDto> getByEmail(String email) {
        return userDao.getByEmail(email)
                .map(userMapper::toDto);
    }

    @Override
    public Page<UserDto> getAll(Pageable pageable) {
        Page<UserEntity> userEntities = userDao.getAll(pageable);
        return userEntities.map(userMapper::toDto);
    }

    @Override
    @CachePut(value = "users", key = "#id")
    public UserDto update(Long id, UserDto updatedUserDto) {
        // Check if email is already taken by another user
        Optional<UserEntity> existingUser = userDao.getByEmail(updatedUserDto.getEmail());
        if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
            throw new DuplicateException("User with email '" + updatedUserDto.getEmail() + "' already exists");
        }

        UserEntity updatedUserEntity = userMapper.toEntity(updatedUserDto);
        userDao.update(id, updatedUserEntity);
        // Fetch the updated entity to ensure it's properly managed
        Optional<UserEntity> updatedEntity = userDao.getById(id);
        if (updatedEntity.isPresent()) {
            return userMapper.toDto(updatedEntity.get());
        } else {
            throw new NotFoundException("User not found after update: " + id);
        }
    }
    
    @Override
    @CacheEvict(value = "users", key = "#id")
    public void delete(Long id) {
        if (userDao.getById(id).isEmpty()) {
            throw new NotFoundException("User not found");
        }
        userDao.delete(id);
    }
}