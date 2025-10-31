package com.innowise.userservice.integration;

import com.innowise.userservice.dao.interfaces.UserDao;
import com.innowise.userservice.dto.models.UserDto;
import com.innowise.userservice.entities.UserEntity;
import com.innowise.userservice.exceptions.DuplicateException;
import com.innowise.userservice.exceptions.NotFoundException;
import com.innowise.userservice.service.interfaces.UserService;
import jakarta.transaction.Transactional;
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
@Transactional
@TestPropertySource(properties = {
        "spring.cache.type=none",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"

})
public class UserServiceIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

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
    void setUp() {
        testUser = new UserDto();
        testUser.setName("Ada");
        testUser.setSurname("Wong");
        testUser.setBirthdate(LocalDate.of(1990, 1, 1));
        testUser.setEmail("ada@example.com");
    }

    @Test
    void testCreate() {
        UserDto created = userService.create(testUser);
        Assertions.assertNotNull(created.getId());
        Assertions.assertEquals("ada@example.com", created.getEmail());
    }

    @Test
    void testCreateDuplicateEmailException() {
        userService.create(testUser);

        DuplicateException exception = Assertions.assertThrows(
                DuplicateException.class,
                () -> userService.create(testUser)
        );
        Assertions.assertTrue(exception.getMessage().contains("User with email 'ada@example.com' already exists"));
    }

    @Test
    void testGetByEmail() {
        userService.create(testUser);
        Optional<UserDto> found = userService.getByEmail("ada@example.com");
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals("Ada", found.get().getName());
    }

    @Test
    void testGetAll() {
        userService.create(testUser);
        Page<UserDto> users = userService.getAll(PageRequest.of(0, 10));
        Assertions.assertFalse(users.isEmpty());
        Assertions.assertEquals(1, users.getTotalElements());
    }

    @Test
    void testUpdate() {
        UserDto created = userService.create(testUser);
        created.setName("ada");

        UserDto updated = userService.update(created.getId(), created);
        Assertions.assertEquals("ada", updated.getName());
    }

    @Test
    void testDelete() {
        UserDto created = userService.create(testUser);
        Long id = created.getId();

        userService.delete(id);

        Optional<UserEntity> deleted = userDao.getById(id);
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
