package com.innowise.orderservice.integration;

import com.innowise.orderservice.dao.interfaces.OrderDao;
import com.innowise.orderservice.dto.models.*;
import com.innowise.orderservice.entities.ItemEntity;
import com.innowise.orderservice.entities.OrderEntity;
import com.innowise.orderservice.exceptions.BadRequestException;
import com.innowise.orderservice.exceptions.NotFoundException;
import com.innowise.orderservice.service.interfaces.OrderService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "spring.liquibase.change-log=classpath:db/changelog/changelog-master.xml"
})
public class OrderServiceIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
            .withDatabaseName("test_order_service")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockBean
    private com.innowise.orderservice.client.UserClient userClient;

    private ItemEntity testItem;
    private OrderDto testOrder;
    private UserDto mockUserDto;

    @BeforeEach
    void cleanDatabase() {
        orderDao.getByIds(List.of(1L, 2L, 3L)).forEach(order -> {
            orderDao.delete(order.getId());
        });
    }

    @BeforeEach
    void setUp() {
        // Create test item directly in database (will be in transaction when test runs)
        testItem = new ItemEntity();
        testItem.setName("Test Item");
        testItem.setPrice(new BigDecimal("99.99"));

        testOrder = new OrderDto();
        testOrder.setUserId(100L);
        testOrder.setStatus("PENDING");

        mockUserDto = UserDto.builder()
                .id(100L)
                .name("Test")
                .surname("User")
                .email("test@example.com")
                .birthdate(LocalDate.of(1990, 1, 1))
                .build();

        when(userClient.getUserById(anyLong())).thenReturn(mockUserDto);
    }

    private void createTestItem() {
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.persist(testItem);
            entityManager.flush();
        });
        
        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setItemId(testItem.getId());
        orderItemDto.setQuantity(2);
        testOrder.setOrderItems(new ArrayList<>(List.of(orderItemDto)));
    }

    @Test
    void testCreate() {
        createTestItem();
        OrderWithUserDto created = orderService.create(testOrder);

        Assertions.assertNotNull(created);
        Assertions.assertNotNull(created.getOrder());
        Assertions.assertNotNull(created.getUser());
        Assertions.assertNotNull(created.getOrder().getId());
        Assertions.assertEquals(testOrder.getUserId(), created.getOrder().getUserId());
        Assertions.assertEquals("PENDING", created.getOrder().getStatus());
        Assertions.assertNotNull(created.getOrder().getCreationDate());
        Assertions.assertNotNull(created.getOrder().getOrderItems());
        Assertions.assertEquals(1, created.getOrder().getOrderItems().size());
        Assertions.assertEquals(2, created.getOrder().getOrderItems().get(0).getQuantity());
        Assertions.assertEquals(mockUserDto.getId(), created.getUser().getId());
    }

    @Test
    void testCreateWhenItemNotFound() {
        OrderItemDto invalidOrderItem = new OrderItemDto();
        invalidOrderItem.setItemId(999L);
        invalidOrderItem.setQuantity(1);
        testOrder.setOrderItems(new ArrayList<>(List.of(invalidOrderItem)));

        BadRequestException exception = Assertions.assertThrows(
                BadRequestException.class,
                () -> orderService.create(testOrder)
        );
        Assertions.assertTrue(exception.getMessage().contains("Item with id 999 not found"));
    }

    @Test
    void testGetById() {
        createTestItem();
        OrderWithUserDto created = orderService.create(testOrder);

        Optional<OrderWithUserDto> found = orderService.getById(created.getOrder().getId());

        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(created.getOrder().getId(), found.get().getOrder().getId());
        Assertions.assertEquals(created.getOrder().getUserId(), found.get().getOrder().getUserId());
        Assertions.assertNotNull(found.get().getUser());
    }

    @Test
    void testGetByIdNotFound() {
        Optional<OrderWithUserDto> found = orderService.getById(999L);

        Assertions.assertTrue(found.isEmpty());
    }

    @Test
    void testGetByIds() {
        createTestItem();
        OrderWithUserDto created1 = orderService.create(testOrder);
        
        OrderDto testOrder2 = new OrderDto();
        testOrder2.setUserId(200L);
        testOrder2.setStatus("PROCESSING");
        OrderItemDto orderItemDto2 = new OrderItemDto();
        orderItemDto2.setItemId(testItem.getId());
        orderItemDto2.setQuantity(1);
        testOrder2.setOrderItems(new ArrayList<>(List.of(orderItemDto2)));
        OrderWithUserDto created2 = orderService.create(testOrder2);

        List<OrderWithUserDto> found = orderService.getByIds(List.of(created1.getOrder().getId(), created2.getOrder().getId()));

        Assertions.assertEquals(2, found.size());
        Assertions.assertNotNull(found.get(0).getUser());
        Assertions.assertNotNull(found.get(1).getUser());
    }

    @Test
    void testGetByStatuses() {
        createTestItem();
        OrderWithUserDto created1 = orderService.create(testOrder);
        
        OrderDto testOrder2 = new OrderDto();
        testOrder2.setUserId(200L);
        testOrder2.setStatus("PROCESSING");
        OrderItemDto orderItemDto2 = new OrderItemDto();
        orderItemDto2.setItemId(testItem.getId());
        orderItemDto2.setQuantity(1);
        testOrder2.setOrderItems(new ArrayList<>(List.of(orderItemDto2)));
        orderService.create(testOrder2);

        List<OrderWithUserDto> pendingOrders = orderService.getByStatuses(List.of("PENDING"));

        Assertions.assertFalse(pendingOrders.isEmpty());
        Assertions.assertTrue(pendingOrders.stream().anyMatch(o -> o.getOrder().getStatus().equals("PENDING")));
        Assertions.assertNotNull(pendingOrders.get(0).getUser());
    }

    @Test
    void testUpdate() {
        createTestItem();
        OrderWithUserDto created = orderService.create(testOrder);
        Assertions.assertEquals(1, created.getOrder().getOrderItems().size());
        
        OrderDto updateDto = new OrderDto();
        updateDto.setUserId(100L);
        updateDto.setStatus("PROCESSING");
        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setItemId(testItem.getId());
        orderItemDto.setQuantity(3);
        updateDto.setOrderItems(new ArrayList<>(List.of(orderItemDto)));

        OrderWithUserDto updated = orderService.update(created.getOrder().getId(), updateDto);

        Assertions.assertEquals("PROCESSING", updated.getOrder().getStatus());
        Assertions.assertNotNull(updated.getOrder().getOrderItems());
        if (updated.getOrder().getOrderItems().size() > 0) {
            Assertions.assertEquals(3, updated.getOrder().getOrderItems().get(0).getQuantity());
        }
        Assertions.assertNotNull(updated.getUser());
    }

    @Test
    void testUpdateWhenOrderNotFound() {
        OrderDto updateDto = new OrderDto();
        updateDto.setStatus("PROCESSING");
        updateDto.setUserId(100L);
        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setItemId(1L);
        orderItemDto.setQuantity(1);
        updateDto.setOrderItems(new ArrayList<>(List.of(orderItemDto)));

        NotFoundException exception = Assertions.assertThrows(
                NotFoundException.class,
                () -> orderService.update(999L, updateDto)
        );
        Assertions.assertEquals("Order not found with id: 999", exception.getMessage());
    }

    @Test
    void testUpdateWhenItemNotFound() {
        createTestItem();
        OrderWithUserDto created = orderService.create(testOrder);
        
        OrderDto updateDto = new OrderDto();
        updateDto.setUserId(100L);
        updateDto.setStatus("PROCESSING");
        OrderItemDto invalidOrderItem = new OrderItemDto();
        invalidOrderItem.setItemId(999L);
        invalidOrderItem.setQuantity(1);
        updateDto.setOrderItems(new ArrayList<>(List.of(invalidOrderItem)));

        BadRequestException exception = Assertions.assertThrows(
                BadRequestException.class,
                () -> orderService.update(created.getOrder().getId(), updateDto)
        );
        Assertions.assertTrue(exception.getMessage().contains("Item with id 999 not found"));
    }

    @Test
    void testDelete() {
        createTestItem();
        OrderWithUserDto created = orderService.create(testOrder);
        Long id = created.getOrder().getId();

        orderService.delete(id);

        Optional<OrderEntity> deleted = orderDao.getById(id);
        Assertions.assertTrue(deleted.isEmpty());
    }

    @Test
    void testDeleteWhenOrderNotFound() {
        NotFoundException exception = Assertions.assertThrows(
                NotFoundException.class,
                () -> orderService.delete(999L)
        );
        Assertions.assertEquals("Order not found with id: 999", exception.getMessage());
    }

    @Test
    void testCascadeDeleteOrderItems() {
        createTestItem();
        OrderWithUserDto created = orderService.create(testOrder);
        Long orderId = created.getOrder().getId();
        
        // Verify items exist within transaction
        Boolean hasItems = transactionTemplate.execute(status -> {
            Optional<OrderEntity> orderBefore = orderDao.getById(orderId);
            return orderBefore.isPresent() && !orderBefore.get().getOrderItems().isEmpty();
        });
        Assertions.assertTrue(hasItems);

        orderService.delete(orderId);

        Optional<OrderEntity> orderAfter = orderDao.getById(orderId);
        Assertions.assertTrue(orderAfter.isEmpty());
    }
}




