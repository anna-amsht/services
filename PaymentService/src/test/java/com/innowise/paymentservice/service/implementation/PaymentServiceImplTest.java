package com.innowise.paymentservice.service.implementation;

import com.innowise.paymentservice.client.RandomNumberClient;
import com.innowise.paymentservice.client.OrderServiceClient;
import com.innowise.paymentservice.dao.interfaces.PaymentDao;
import com.innowise.paymentservice.dto.mappers.PaymentMapper;
import com.innowise.paymentservice.dto.models.CreatePaymentEventDto;
import com.innowise.paymentservice.dto.models.PaymentDto;
import com.innowise.paymentservice.dto.models.OrderDto;
import com.innowise.paymentservice.dto.models.OrderItemDto;
import com.innowise.paymentservice.entities.PaymentEntity;
import com.innowise.paymentservice.exceptions.BadRequestException;
import com.innowise.paymentservice.kafka.PaymentEventProducer;
import com.innowise.paymentservice.service.PaymentIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentDao paymentDao;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private RandomNumberClient randomNumberClient;

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private PaymentIdGenerator paymentIdGenerator;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentEntity paymentEntity;
    private PaymentDto paymentDto;

    @BeforeEach
    void setUp() {
        paymentEntity = new PaymentEntity();
        paymentEntity.setId(1L);
        paymentEntity.setOrderId(100L);
        paymentEntity.setUserId(200L);
        paymentEntity.setStatus("SUCCESS");
        paymentEntity.setTimestamp(LocalDateTime.now());
        paymentEntity.setPaymentAmount(new BigDecimal("99.99"));

        paymentDto = PaymentDto.builder()
                .id(1L)
                .orderId(100L)
                .userId(200L)
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .paymentAmount(new BigDecimal("99.99"))
                .build();
    }

    private void setupOrderServiceClientMock() {
        when(orderServiceClient.getOrderById(any())).thenReturn(
                OrderDto.builder()
                        .id(1L)
                        .userId(100L)
                        .status("PENDING")
                        .orderItems(List.of(
                                OrderItemDto.builder()
                                        .id(1L)
                                        .productId(1L)
                                        .quantity(1)
                                        .price(new BigDecimal("99.99"))
                                        .build()
                        ))
                        .build()
        );
    }

    @Test
    void testCreateWithEvenRandomNumber() {
        setupOrderServiceClientMock();
        when(paymentMapper.toEntity(paymentDto)).thenReturn(paymentEntity);
        when(randomNumberClient.fetchRandomNumber()).thenReturn(10);
        when(paymentIdGenerator.generateId()).thenReturn(1L);
        when(paymentDao.save(any(PaymentEntity.class))).thenReturn(paymentEntity);
        when(paymentMapper.toDto(any(PaymentEntity.class))).thenReturn(paymentDto);

        PaymentDto result = paymentService.create(paymentDto);

        assertNotNull(result);
        assertEquals(paymentDto.getOrderId(), result.getOrderId());
        verify(paymentDao).save(any(PaymentEntity.class));
        verify(paymentEventProducer).sendCreatePaymentEvent(any(CreatePaymentEventDto.class));
        verify(randomNumberClient).fetchRandomNumber();
        assertEquals("SUCCESS", paymentEntity.getStatus());
    }

    @Test
    void testCreateWithOddRandomNumber() {
        setupOrderServiceClientMock();
        when(paymentMapper.toEntity(paymentDto)).thenReturn(paymentEntity);
        when(randomNumberClient.fetchRandomNumber()).thenReturn(7);
        when(paymentIdGenerator.generateId()).thenReturn(2L);
        when(paymentDao.save(any(PaymentEntity.class))).thenReturn(paymentEntity);
        when(paymentMapper.toDto(any(PaymentEntity.class))).thenReturn(paymentDto);

        PaymentDto result = paymentService.create(paymentDto);

        assertNotNull(result);
        verify(paymentDao).save(any(PaymentEntity.class));
        verify(paymentEventProducer).sendCreatePaymentEvent(any(CreatePaymentEventDto.class));
        assertEquals("FAILED", paymentEntity.getStatus());
    }

    @Test
    void testCreateWithNullRandomNumber() {
        setupOrderServiceClientMock();
        when(paymentMapper.toEntity(paymentDto)).thenReturn(paymentEntity);
        when(randomNumberClient.fetchRandomNumber()).thenReturn(null);
        when(paymentIdGenerator.generateId()).thenReturn(3L);
        when(paymentDao.save(any(PaymentEntity.class))).thenReturn(paymentEntity);
        when(paymentMapper.toDto(any(PaymentEntity.class))).thenReturn(paymentDto);

        PaymentDto result = paymentService.create(paymentDto);

        assertNotNull(result);
        verify(paymentDao).save(any(PaymentEntity.class));
        verify(paymentEventProducer).sendCreatePaymentEvent(any(CreatePaymentEventDto.class));
        assertEquals("FAILED", paymentEntity.getStatus());
    }

    @Test
    void testGetByOrderId() {
        List<PaymentEntity> entities = List.of(paymentEntity);
        when(paymentDao.findByOrderId(100L)).thenReturn(entities);
        when(paymentMapper.toDto(paymentEntity)).thenReturn(paymentDto);

        List<PaymentDto> result = paymentService.getByOrderId(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(paymentDto.getOrderId(), result.get(0).getOrderId());
        verify(paymentDao).findByOrderId(100L);
    }

    @Test
    void testGetByUserId() {
        List<PaymentEntity> entities = List.of(paymentEntity);
        when(paymentDao.findByUserId(200L)).thenReturn(entities);
        when(paymentMapper.toDto(paymentEntity)).thenReturn(paymentDto);

        List<PaymentDto> result = paymentService.getByUserId(200L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(paymentDto.getUserId(), result.get(0).getUserId());
        verify(paymentDao).findByUserId(200L);
    }

    @Test
    void testGetByStatuses() {
        List<String> statuses = List.of("SUCCESS", "FAILED");
        List<PaymentEntity> entities = List.of(paymentEntity);
        when(paymentDao.findByStatusIn(statuses)).thenReturn(entities);
        when(paymentMapper.toDto(paymentEntity)).thenReturn(paymentDto);

        List<PaymentDto> result = paymentService.getByStatuses(statuses);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(paymentDao).findByStatusIn(statuses);
    }

    @Test
    void testGetTotalSumByPeriod() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        List<PaymentEntity> payments = List.of(
            new PaymentEntity(1L, 1L, 1L, "SUCCESS", LocalDateTime.now(), new BigDecimal("500.00")),
            new PaymentEntity(2L, 2L, 2L, "SUCCESS", LocalDateTime.now(), new BigDecimal("500.00"))
        );

        when(paymentDao.findByTimestampBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(payments);

        BigDecimal result = paymentService.getTotalSumByPeriod(startDate, endDate);

        assertNotNull(result);
        assertEquals(new BigDecimal("1000.00"), result);
        verify(paymentDao).findByTimestampBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void testGetTotalSumByPeriodWithNullStartDate() {
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> paymentService.getTotalSumByPeriod(null, endDate)
        );
        assertEquals("Start date and end date must be provided", exception.getMessage());
        verify(paymentDao, never()).findByTimestampBetween(any(), any());
    }

    @Test
    void testGetTotalSumByPeriodWithNullEndDate() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> paymentService.getTotalSumByPeriod(startDate, null)
        );
        assertEquals("Start date and end date must be provided", exception.getMessage());
        verify(paymentDao, never()).findByTimestampBetween(any(), any());
    }

    @Test
    void testGetTotalSumByPeriodWithEndDateBeforeStartDate() {
        LocalDate startDate = LocalDate.of(2024, 1, 31);
        LocalDate endDate = LocalDate.of(2024, 1, 1);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> paymentService.getTotalSumByPeriod(startDate, endDate)
        );
        assertEquals("End date must not be before start date", exception.getMessage());
        verify(paymentDao, never()).findByTimestampBetween(any(), any());
    }
}
