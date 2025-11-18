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
public class CardServiceIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
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

    private UserDto testUser;
    private CardDto testCard;
    private Long userId;

    @BeforeEach
    void cleanDatabase() {
        cardDao.getAll(PageRequest.of(0, 50))
                .forEach(card -> cardDao.delete(card.getId()));

        userDao.getAll(PageRequest.of(0, 50))
                .forEach(user -> userDao.delete(user.getId()));
    }

    @BeforeEach
    void setUp() {
        // фиксированные данные
        testUser = new UserDto();
        testUser.setId(200L);
        testUser.setName("TestName");
        testUser.setSurname("TestSurname");
        testUser.setBirthdate(LocalDate.of(1990, 1, 1));
        testUser.setEmail("test@example.com");

        UserDto createdUser = userService.createFromCredentials(testUser);
        userId = createdUser.getId();

        testCard = new CardDto();
        testCard.setNumber("1111-2222-2222-2222");
        testCard.setHolder("TestName TestSurname");
        testCard.setExpirationDate(LocalDate.of(2030, 1, 1));
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
        cardService.create(testCard);

        DuplicateException ex = Assertions.assertThrows(
                DuplicateException.class,
                () -> cardService.create(testCard)
        );

        Assertions.assertTrue(ex.getMessage().contains("Card with this number already exists"));
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
        Assertions.assertEquals(1, cards.getTotalElements());
    }

    @Test
    void testUpdate() {
        CardDto created = cardService.create(testCard);

        CardDto update = new CardDto();
        update.setNumber("2222-3333-3333-3333");
        update.setHolder("Updated Holder");
        update.setExpirationDate(LocalDate.of(2032, 1, 1));
        update.setUserId(userId);

        CardDto updated = cardService.update(created.getId(), update);

        Assertions.assertEquals(update.getNumber(), updated.getNumber());
        Assertions.assertEquals(update.getHolder(), updated.getHolder());
        Assertions.assertEquals(update.getExpirationDate(), updated.getExpirationDate());
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

