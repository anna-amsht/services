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
    private final UserDao userDao;
    private final UserMapper userMapper;

    @Override
    public UserDto create(UserDto userDto) {
        if (userDao.getByEmail(userDto.getEmail()).isPresent()) {
            throw new DuplicateException("User with email '" + userDto.getEmail() + "' already exists");
        }
        UserEntity userEntity = userMapper.toEntity(userDto);
        userDao.create(userEntity);
        return userMapper.toDto(userEntity);
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
        UserEntity updatedUserEntity = userMapper.toEntity(updatedUserDto);
        userDao.update(id, updatedUserEntity);
        return userMapper.toDto(updatedUserEntity);
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
