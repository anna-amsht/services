package com.innowise.userservice.integration;

import com.innowise.userservice.dao.interfaces.UserDao;
import com.innowise.userservice.dto.models.UserDto;
import com.innowise.userservice.entities.UserEntity;
import com.innowise.userservice.exceptions.DuplicateException;
import com.innowise.userservice.exceptions.NotFoundException;
import com.innowise.userservice.service.interfaces.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.Optional;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "spring.cache.type=none",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
public class UserServiceIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserDao userDao;

    private UserDto testUser;

    @BeforeEach
    void cleanDatabase() {
        userDao.getAll(PageRequest.of(0, 50))
                .forEach(user -> userDao.delete(user.getId()));
    }

    @BeforeEach
    void setUp() {
        testUser = new UserDto();
        testUser.setId(100L);
        testUser.setEmail("jwtuser@example.com");
        testUser.setName("John");
        testUser.setSurname("Doe");
        testUser.setBirthdate(LocalDate.of(1990, 1, 1));
    }

    @Test
    void testCreate() {
        UserDto created = userService.createFromCredentials(testUser);
        Assertions.assertNotNull(created.getId());
        Assertions.assertEquals(testUser.getId(), created.getId());
        Assertions.assertEquals(testUser.getEmail(), created.getEmail());
        Assertions.assertEquals(testUser.getName(), created.getName());
    }

    @Test
    void testCreateDuplicateEmailException() {
        userService.createFromCredentials(testUser);
        UserDto duplicate = new UserDto();
        duplicate.setId(200L);
        duplicate.setEmail(testUser.getEmail());
        duplicate.setName("Another");
        duplicate.setSurname("User");
        duplicate.setBirthdate(LocalDate.of(1992, 1, 1));

        DuplicateException ex = Assertions.assertThrows(
                DuplicateException.class,
                () -> userService.createFromCredentials(duplicate)
        );

        Assertions.assertTrue(ex.getMessage().contains(testUser.getEmail()));
    }

    @Test
    void testGetByEmail() {
        userService.createFromCredentials(testUser);

        Optional<UserDto> found = userService.getByEmail(testUser.getEmail());

        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(testUser.getName(), found.get().getName());
    }

    @Test
    void testGetAll() {
        userService.createFromCredentials(testUser);

        Page<UserDto> users = userService.getAll(PageRequest.of(0, 10));

        Assertions.assertEquals(1, users.getTotalElements());
    }

    @Test
    void testUpdate() {
        UserDto created = userService.createFromCredentials(testUser);

        UserDto updatedDto = new UserDto();
        updatedDto.setEmail(created.getEmail());
        updatedDto.setName("Updated");
        updatedDto.setSurname("User");
        updatedDto.setBirthdate(created.getBirthdate());

        UserDto updated = userService.update(created.getId(), updatedDto);

        Assertions.assertEquals("Updated", updated.getName());
    }

    @Test
    void testDelete() {
        UserDto created = userService.createFromCredentials(testUser);

        userService.delete(created.getId());

        Optional<UserEntity> deleted = userDao.getById(created.getId());
        Assertions.assertTrue(deleted.isEmpty());
    }

    @Test
    void testDeleteNotFoundException() {
        NotFoundException ex = Assertions.assertThrows(
                NotFoundException.class,
                () -> userService.delete(999L)
        );
        Assertions.assertEquals("User not found", ex.getMessage());
    }
}