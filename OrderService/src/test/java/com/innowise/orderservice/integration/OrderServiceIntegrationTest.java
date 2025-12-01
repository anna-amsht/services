package com.innowise.orderservice.integration;

import com.innowise.orderservice.dao.interfaces.OrderDao;
import com.innowise.orderservice.dto.models.ItemDto;
import com.innowise.orderservice.dto.models.OrderDto;
import com.innowise.orderservice.dto.models.OrderItemDto;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    private ItemEntity testItem;
    private OrderDto testOrder;

    @BeforeEach
    void cleanDatabase() {
        orderDao.getByIds(List.of(1L, 2L, 3L)).forEach(order -> {
            orderDao.delete(order.getId());
        });

    }
    @BeforeEach
    void setUp() {

        testItem = new ItemEntity();
        testItem.setName("Test Item");
        testItem.setPrice(new BigDecimal("99.99"));
        entityManager.persist(testItem);
        entityManager.flush();

        testOrder = new OrderDto();
        testOrder.setUserId(100L);
        testOrder.setStatus("PENDING");

        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setItemId(testItem.getId());
        orderItemDto.setQuantity(2);
        testOrder.setOrderItems(new ArrayList<>(List.of(orderItemDto)));
    }

    @Test
    void testCreate() {
        OrderDto created = orderService.create(testOrder);

        Assertions.assertNotNull(created.getId());
        Assertions.assertEquals(testOrder.getUserId(), created.getUserId());
        Assertions.assertEquals("PENDING", created.getStatus());
        Assertions.assertNotNull(created.getCreationDate());
        Assertions.assertNotNull(created.getOrderItems());
        Assertions.assertEquals(1, created.getOrderItems().size());
        Assertions.assertEquals(2, created.getOrderItems().get(0).getQuantity());
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
        OrderDto created = orderService.create(testOrder);

        Optional<OrderDto> found = orderService.getById(created.getId());

        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(created.getId(), found.get().getId());
        Assertions.assertEquals(created.getUserId(), found.get().getUserId());
    }

    @Test
    void testGetByIdNotFound() {
        Optional<OrderDto> found = orderService.getById(999L);

        Assertions.assertTrue(found.isEmpty());
    }

    @Test
    void testGetByIds() {
        OrderDto created1 = orderService.create(testOrder);
        
        OrderDto testOrder2 = new OrderDto();
        testOrder2.setUserId(200L);
        testOrder2.setStatus("PROCESSING");
        OrderItemDto orderItemDto2 = new OrderItemDto();
        orderItemDto2.setItemId(testItem.getId());
        orderItemDto2.setQuantity(1);
        testOrder2.setOrderItems(new ArrayList<>(List.of(orderItemDto2)));
        OrderDto created2 = orderService.create(testOrder2);

        List<OrderDto> found = orderService.getByIds(List.of(created1.getId(), created2.getId()));

        Assertions.assertEquals(2, found.size());
    }

    @Test
    void testGetByStatuses() {
        OrderDto created1 = orderService.create(testOrder);
        
        OrderDto testOrder2 = new OrderDto();
        testOrder2.setUserId(200L);
        testOrder2.setStatus("PROCESSING");
        OrderItemDto orderItemDto2 = new OrderItemDto();
        orderItemDto2.setItemId(testItem.getId());
        orderItemDto2.setQuantity(1);
        testOrder2.setOrderItems(new ArrayList<>(List.of(orderItemDto2)));
        orderService.create(testOrder2);

        List<OrderDto> pendingOrders = orderService.getByStatuses(List.of("PENDING"));

        Assertions.assertFalse(pendingOrders.isEmpty());
        Assertions.assertTrue(pendingOrders.stream().anyMatch(o -> o.getStatus().equals("PENDING")));
    }

    @Test
    void testUpdate() {
        OrderDto created = orderService.create(testOrder);
        
        OrderDto updateDto = new OrderDto();
        updateDto.setStatus("PROCESSING");
        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setItemId(testItem.getId());
        orderItemDto.setQuantity(3);
        updateDto.setOrderItems(new ArrayList<>(List.of(orderItemDto)));

        OrderDto updated = orderService.update(created.getId(), updateDto);

        Assertions.assertEquals("PROCESSING", updated.getStatus());
        Assertions.assertEquals(1, updated.getOrderItems().size());
        Assertions.assertEquals(3, updated.getOrderItems().get(0).getQuantity());
    }

    @Test
    void testUpdateWhenOrderNotFound() {
        OrderDto updateDto = new OrderDto();
        updateDto.setStatus("PROCESSING");

        NotFoundException exception = Assertions.assertThrows(
                NotFoundException.class,
                () -> orderService.update(999L, updateDto)
        );
        Assertions.assertEquals("Order not found with id: 999", exception.getMessage());
    }

    @Test
    void testUpdateWhenItemNotFound() {
        OrderDto created = orderService.create(testOrder);
        
        OrderDto updateDto = new OrderDto();
        updateDto.setStatus("PROCESSING");
        OrderItemDto invalidOrderItem = new OrderItemDto();
        invalidOrderItem.setItemId(999L);
        invalidOrderItem.setQuantity(1);
        updateDto.setOrderItems(new ArrayList<>(List.of(invalidOrderItem)));

        BadRequestException exception = Assertions.assertThrows(
                BadRequestException.class,
                () -> orderService.update(created.getId(), updateDto)
        );
        Assertions.assertTrue(exception.getMessage().contains("Item with id 999 not found"));
    }

    @Test
    void testDelete() {
        OrderDto created = orderService.create(testOrder);
        Long id = created.getId();

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
        OrderDto created = orderService.create(testOrder);
        Long orderId = created.getId();
        
        Optional<OrderEntity> orderBefore = orderDao.getById(orderId);
        Assertions.assertTrue(orderBefore.isPresent());
        Assertions.assertFalse(orderBefore.get().getOrderItems().isEmpty());

        orderService.delete(orderId);

        Optional<OrderEntity> orderAfter = orderDao.getById(orderId);
        Assertions.assertTrue(orderAfter.isEmpty());
    }
}




