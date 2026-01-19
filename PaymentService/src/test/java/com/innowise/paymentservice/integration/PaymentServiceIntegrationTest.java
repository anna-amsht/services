package com.innowise.paymentservice.integration;

import com.innowise.paymentservice.client.OrderServiceClient;
import com.innowise.paymentservice.client.RandomNumberClient;
import com.innowise.paymentservice.dao.interfaces.PaymentDao;
import com.innowise.paymentservice.dto.models.OrderDto;
import com.innowise.paymentservice.dto.models.OrderItemDto;
import com.innowise.paymentservice.dto.models.PaymentDto;
import com.innowise.paymentservice.entities.PaymentEntity;
import com.innowise.paymentservice.exceptions.BadRequestException;
import com.innowise.paymentservice.kafka.PaymentEventProducer;
import com.innowise.paymentservice.service.interfaces.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "spring.liquibase.change-log=classpath:db/changelog/changelog-master.json"
})
public class PaymentServiceIntegrationTest {

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
            .withExposedPorts(27017);

    @Container
    private static final KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentDao paymentDao;

    @MockBean
    private PaymentEventProducer paymentEventProducer;

    @MockBean
    private OrderServiceClient orderServiceClient;

    @MockBean
    private RandomNumberClient randomNumberClient;

    @BeforeEach
    void cleanDatabase() {
        paymentDao.deleteAll();
    }

    @BeforeEach
    void setupOrderServiceMock() {
        when(orderServiceClient.getOrderById(anyLong()))
                .thenAnswer(invocation -> {
                    OrderItemDto mockItem = OrderItemDto.builder()
                            .id(1L)
                            .productId(1L)
                            .quantity(1)
                            .price(new BigDecimal("99999.99"))
                            .build();
                    return OrderDto.builder()
                            .id(1L)
                            .userId(100L)
                            .status("PENDING")
                            .orderItems(List.of(mockItem))
                            .build();
                });
        when(randomNumberClient.fetchRandomNumber()).thenReturn(10);
    }

    @Test
    void testCreatePaymentWithSuccessStatus() {
        PaymentDto paymentDto = PaymentDto.builder()
                .orderId(100L)
                .userId(200L)
                .paymentAmount(new BigDecimal("99999.99"))
                .build();

        PaymentDto created = paymentService.create(paymentDto);

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("SUCCESS", created.getStatus());
        assertEquals(paymentDto.getOrderId(), created.getOrderId());
        assertEquals(paymentDto.getUserId(), created.getUserId());
        assertNotNull(created.getTimestamp());
    }


    @Test
    void testGetByOrderId() {
        PaymentDto paymentDto = PaymentDto.builder()
                .orderId(102L)
                .userId(202L)
                .paymentAmount(new BigDecimal("99999.99"))
                .build();

        PaymentDto created = paymentService.create(paymentDto);

        List<PaymentDto> payments = paymentService.getByOrderId(102L);

        assertNotNull(payments);
        assertFalse(payments.isEmpty());
        assertTrue(payments.stream().anyMatch(p -> p.getId().equals(created.getId())));
    }

    @Test
    void testGetByUserId() {
        PaymentDto paymentDto = PaymentDto.builder()
                .orderId(103L)
                .userId(203L)
                .paymentAmount(new BigDecimal("99999.99"))
                .build();

        PaymentDto created = paymentService.create(paymentDto);

        List<PaymentDto> payments = paymentService.getByUserId(203L);

        assertNotNull(payments);
        assertFalse(payments.isEmpty());
        assertTrue(payments.stream().anyMatch(p -> p.getId().equals(created.getId())));
    }

    @Test
    void testGetByStatuses() {
        PaymentDto paymentDto = PaymentDto.builder()
                .orderId(104L)
                .userId(204L)
                .paymentAmount(new BigDecimal("99999.99"))
                .build();

        paymentService.create(paymentDto);

        List<PaymentDto> payments = paymentService.getByStatuses(List.of("SUCCESS"));

        assertNotNull(payments);
        assertFalse(payments.isEmpty());
        assertTrue(payments.stream().allMatch(p -> "SUCCESS".equals(p.getStatus())));
    }

    @Test
    void testGetTotalSumByPeriod() {
        PaymentDto payment1 = PaymentDto.builder()
                .orderId(105L)
                .userId(205L)
                .paymentAmount(new BigDecimal("99999.99"))
                .build();

        PaymentDto payment2 = PaymentDto.builder()
                .orderId(106L)
                .userId(206L)
                .paymentAmount(new BigDecimal("99999.99"))
                .build();

        paymentService.create(payment1);
        paymentService.create(payment2);

        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(1);

        BigDecimal total = paymentService.getTotalSumByPeriod(startDate, endDate);

        assertNotNull(total);
        assertTrue(total.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testGetTotalSumByPeriodWithNullDates() {
        assertThrows(BadRequestException.class,
                () -> paymentService.getTotalSumByPeriod(null, LocalDate.now()));

        assertThrows(BadRequestException.class,
                () -> paymentService.getTotalSumByPeriod(LocalDate.now(), null));
    }

    @Test
    void testGetTotalSumByPeriodWithInvalidDateRange() {
        LocalDate startDate = LocalDate.of(2024, 12, 31);
        LocalDate endDate = LocalDate.of(2024, 1, 1);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> paymentService.getTotalSumByPeriod(startDate, endDate)
        );
        assertEquals("End date must not be before start date", exception.getMessage());
    }
}
