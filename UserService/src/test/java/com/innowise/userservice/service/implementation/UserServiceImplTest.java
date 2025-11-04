package com.innowise.userservice.service.implementation;

import com.innowise.userservice.dao.interfaces.UserDao;
import com.innowise.userservice.dto.mappers.UserMapper;
import com.innowise.userservice.dto.models.UserDto;
import com.innowise.userservice.entities.UserEntity;
import com.innowise.userservice.exceptions.DuplicateException;
import com.innowise.userservice.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserDao userDao;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private UserEntity userEntity;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        userEntity = new UserEntity(1L, "Ada", "Wong",
                LocalDate.of(1990, 1, 1), "ada@example.com", new ArrayList<>());

        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setName("Ada");
        userDto.setSurname("Wong");
        userDto.setBirthdate(LocalDate.of(1990, 1, 1));
        userDto.setEmail("ada@example.com");
        userDto.setCards(new ArrayList<>());
    }

    @Test
    void testCreate() {
        when(userDao.getByEmail(userDto.getEmail())).thenReturn(Optional.empty());
        when(userMapper.toEntity(userDto)).thenReturn(userEntity);
        when(userMapper.toDto(userEntity)).thenReturn(userDto);

        UserDto result = userService.create(userDto);

        assertEquals(userDto.getEmail(), result.getEmail());
        verify(userDao).create(userEntity);
    }

    @Test
    void testCreateWhenDuplicateEmailException() {
        when(userDao.getByEmail(userDto.getEmail())).thenReturn(Optional.of(userEntity));

        DuplicateException exception = assertThrows(
                DuplicateException.class,
                () -> userService.create(userDto)
        );
        assertEquals("User with email 'ada@example.com' already exists", exception.getMessage());

    }
    @Test
    void testGetById() {
        when(userDao.getById(1L)).thenReturn(Optional.of(userEntity));
        when(userMapper.toDto(userEntity)).thenReturn(userDto);

        Optional<UserDto> result = userService.getById(1L);

        assertTrue(result.isPresent());
        assertEquals(userDto.getEmail(), result.get().getEmail());
    }

    @Test
    void testGetByIdUserNotFoundException() {
        when(userDao.getById(999L)).thenReturn(Optional.empty());

        Optional<UserDto> result = userService.getById(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetByEmail() {
        when(userDao.getByEmail("ada@example.com")).thenReturn(Optional.of(userEntity));
        when(userMapper.toDto(userEntity)).thenReturn(userDto);

        Optional<UserDto> result = userService.getByEmail("ada@example.com");

        assertTrue(result.isPresent());
        assertEquals("ada@example.com", result.get().getEmail());
    }

    @Test
    void testGetByEmailNotFoundException() {
        when(userDao.getByEmail("missing@example.com")).thenReturn(Optional.empty());

        Optional<UserDto> result = userService.getByEmail("missing@example.com");

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetAll() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<UserEntity> entityPage = new PageImpl<>(List.of(userEntity), pageable, 1);

        when(userDao.getAll(pageable)).thenReturn(entityPage);
        when(userMapper.toDto(userEntity)).thenReturn(userDto);

        Page<UserDto> result = userService.getAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Ada", result.getContent().get(0).getName());
    }

    @Test
    void testUpdate() {
        when(userMapper.toEntity(userDto)).thenReturn(userEntity);
        when(userMapper.toDto(userEntity)).thenReturn(userDto);

        UserDto result = userService.update(1L, userDto);

        verify(userDao).update(1L, userEntity);
        assertEquals("ada@example.com", result.getEmail());
    }

    @Test
    void testDelete() {
        when(userDao.getById(1L)).thenReturn(Optional.of(userEntity));

        userService.delete(1L);

        verify(userDao).delete(1L);
    }

    @Test
    void testDeleteNotFoundException_WhenUserDoesNotExist() {
        when(userDao.getById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> userService.delete(999L)
        );

        assertEquals("User not found", exception.getMessage());
    }
}