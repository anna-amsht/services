package com.innowise.userservice.integration;

import com.innowise.userservice.dao.interfaces.CardDao;
import com.innowise.userservice.dao.interfaces.UserDao;
import com.innowise.userservice.dto.models.CardDto;
import com.innowise.userservice.dto.models.UserDto;
import com.innowise.userservice.entities.CardEntity;
import com.innowise.userservice.exceptions.DuplicateException;
import com.innowise.userservice.exceptions.NotFoundException;
import com.innowise.userservice.service.interfaces.CardService;
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
import java.util.UUID;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "spring.cache.type=none",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
public class CardServiceIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @Autowired
    private CardService cardService;

    @Autowired
    private UserService userService;

    @Autowired
    private CardDao cardDao;

    @Autowired
    private UserDao userDao;

    private CardDto testCard;
    private UserDto testUser;
    private Long userId;

    @BeforeEach
    void cleanDatabase() {
        userDao.getAll(PageRequest.of(0, 10))
                .forEach(user -> userDao.delete(user.getId()));
    }
    @BeforeEach
    void setUp() {
        testUser = new UserDto();
        testUser.setName("testUserName");
        testUser.setSurname("testUserSurname");
        testUser.setBirthdate(LocalDate.of(1990, 1, 1));
        testUser.setEmail("testmail@example.com");

        UserDto createdUser = userService.create(testUser);
        userId = createdUser.getId();

        testCard = new CardDto();
        testCard.setNumber("1234-5678-9012-3456");
        testCard.setHolder("testUserName testUserSurname");
        testCard.setExpirationDate(LocalDate.now().plusYears(1));
        testCard.setUserId(userId);
    }

    @Test
    void testCreate() {
        CardDto created = cardService.create(testCard);
        Assertions.assertNotNull(created.getId());
        Assertions.assertEquals(testCard.getNumber(), created.getNumber());
        Assertions.assertEquals(userId, created.getUserId());
    }

    @Test
    void testCreateDuplicateNumberException() {
        CardDto created = cardService.create(testCard);
        CardDto duplicateCard = new CardDto();
        duplicateCard.setNumber("1234-5678-9012-3456");
        duplicateCard.setHolder("testUserName testUserSurname");
        duplicateCard.setExpirationDate(LocalDate.now().plusYears(1));
        duplicateCard.setUserId(userId);

        DuplicateException exception = Assertions.assertThrows(
                DuplicateException.class,
                () -> cardService.create(duplicateCard)
        );
        Assertions.assertTrue(exception.getMessage().contains("Card with this number already exists"));
    }

    @Test
    void testGetById() {
        CardDto created = cardService.create(testCard);
        Optional<CardDto> found = cardService.getById(created.getId());
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(testCard.getNumber(), found.get().getNumber());
    }

    @Test
    void testGetAll() {
        cardService.create(testCard);
        Page<CardDto> cards = cardService.getAll(PageRequest.of(0, 10));
        Assertions.assertFalse(cards.isEmpty());
        Assertions.assertEquals(1, cards.getTotalElements());
    }

    @Test
    void testUpdate() {
        CardDto created = cardService.create(testCard);

        CardDto updateCard = new CardDto();
        updateCard.setNumber("9876-5432-1098-7654");
        updateCard.setHolder("testUserNameUpdt testUserSurnameUpdt");
        updateCard.setExpirationDate(LocalDate.now().plusYears(2));
        updateCard.setUserId(userId);

        CardDto updated = cardService.update(created.getId(), updateCard);
        Assertions.assertEquals(updateCard.getNumber(), updated.getNumber());
        Assertions.assertEquals("testUserNameUpdt testUserSurnameUpdt", updated.getHolder());
        Assertions.assertEquals(LocalDate.now().plusYears(2), updated.getExpirationDate());
        Assertions.assertEquals(userId, updated.getUserId());
    }

    @Test
    void testDelete() {

        CardDto created = cardService.create(testCard);
        Long id = created.getId();

        cardService.delete(id);

        Optional<CardEntity> deleted = cardDao.getById(id);
        Assertions.assertTrue(deleted.isEmpty());
    }

    @Test
    void testDeleteNotFoundException() {
        NotFoundException ex = Assertions.assertThrows(
                NotFoundException.class,
                () -> cardService.delete(999L)
        );
        Assertions.assertEquals("Card not found", ex.getMessage());
    }

}