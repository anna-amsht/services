package com.innowise.paymentservice.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.innowise.paymentservice.dao.interfaces.PaymentDao;
import com.innowise.paymentservice.dto.models.PaymentDto;
import com.innowise.paymentservice.entities.PaymentEntity;
import com.innowise.paymentservice.exceptions.BadRequestException;
import com.innowise.paymentservice.kafka.PaymentEventProducer;
import com.innowise.paymentservice.service.interfaces.PaymentService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

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

    private static WireMockServer wireMockServer;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("random-number.url", () -> "http://localhost:" + wireMockServer.port() + "/api/random");
    }

    @BeforeAll
    static void setupWireMock() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void tearDownWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentDao paymentDao;

    @MockBean
    private PaymentEventProducer paymentEventProducer;

    @BeforeEach
    void cleanDatabase() {
        paymentDao.deleteAll();
    }

    @BeforeEach
    void setupWireMockStubs() {
        wireMockServer.resetAll();
    }

    @Test
    void testCreatePaymentWithSuccessStatus() {
        stubFor(get(urlEqualTo("/api/random"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[10]")));

        PaymentDto paymentDto = PaymentDto.builder()
                .orderId(100L)
                .userId(200L)
                .paymentAmount(new BigDecimal("99.99"))
                .build();

        PaymentDto created = paymentService.create(paymentDto);

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("SUCCESS", created.getStatus());
        assertEquals(paymentDto.getOrderId(), created.getOrderId());
        assertEquals(paymentDto.getUserId(), created.getUserId());
        assertNotNull(created.getTimestamp());

        verify(getRequestedFor(urlEqualTo("/api/random")));
    }

    @Test
    void testCreatePaymentWithFailedStatus() {
        stubFor(get(urlEqualTo("/api/random"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[7]")));

        PaymentDto paymentDto = PaymentDto.builder()
                .orderId(101L)
                .userId(201L)
                .paymentAmount(new BigDecimal("49.99"))
                .build();

        PaymentDto created = paymentService.create(paymentDto);

        assertNotNull(created);
        assertEquals("FAILED", created.getStatus());

        verify(getRequestedFor(urlEqualTo("/api/random")));
    }

    @Test
    void testGetByOrderId() {
        stubFor(get(urlEqualTo("/api/random"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[10]")));

        PaymentDto paymentDto = PaymentDto.builder()
                .orderId(102L)
                .userId(202L)
                .paymentAmount(new BigDecimal("150.00"))
                .build();

        PaymentDto created = paymentService.create(paymentDto);

        List<PaymentDto> payments = paymentService.getByOrderId(102L);

        assertNotNull(payments);
        assertFalse(payments.isEmpty());
        assertTrue(payments.stream().anyMatch(p -> p.getId().equals(created.getId())));
    }

    @Test
    void testGetByUserId() {
        stubFor(get(urlEqualTo("/api/random"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[8]")));

        PaymentDto paymentDto = PaymentDto.builder()
                .orderId(103L)
                .userId(203L)
                .paymentAmount(new BigDecimal("75.50"))
                .build();

        PaymentDto created = paymentService.create(paymentDto);

        List<PaymentDto> payments = paymentService.getByUserId(203L);

        assertNotNull(payments);
        assertFalse(payments.isEmpty());
        assertTrue(payments.stream().anyMatch(p -> p.getId().equals(created.getId())));
    }

    @Test
    void testGetByStatuses() {
        stubFor(get(urlEqualTo("/api/random"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[12]")));

        PaymentDto paymentDto = PaymentDto.builder()
                .orderId(104L)
                .userId(204L)
                .paymentAmount(new BigDecimal("200.00"))
                .build();

        paymentService.create(paymentDto);

        List<PaymentDto> payments = paymentService.getByStatuses(List.of("SUCCESS"));

        assertNotNull(payments);
        assertFalse(payments.isEmpty());
        assertTrue(payments.stream().allMatch(p -> "SUCCESS".equals(p.getStatus())));
    }

    @Test
    void testGetTotalSumByPeriod() {
        stubFor(get(urlEqualTo("/api/random"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[10]")));

        PaymentDto payment1 = PaymentDto.builder()
                .orderId(105L)
                .userId(205L)
                .paymentAmount(new BigDecimal("100.00"))
                .build();

        PaymentDto payment2 = PaymentDto.builder()
                .orderId(106L)
                .userId(206L)
                .paymentAmount(new BigDecimal("150.00"))
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
