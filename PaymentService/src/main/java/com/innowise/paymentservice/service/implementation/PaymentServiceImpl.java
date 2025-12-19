package com.innowise.paymentservice.service.implementation;

import com.innowise.paymentservice.client.RandomNumberClient;
import com.innowise.paymentservice.dao.interfaces.PaymentDao;
import com.innowise.paymentservice.dto.mappers.PaymentMapper;
import com.innowise.paymentservice.dto.models.CreatePaymentEventDto;
import com.innowise.paymentservice.dto.models.PaymentDto;
import com.innowise.paymentservice.entities.PaymentEntity;
import com.innowise.paymentservice.exceptions.BadRequestException;
import com.innowise.paymentservice.kafka.PaymentEventProducer;
import com.innowise.paymentservice.service.interfaces.PaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Validated
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentDao paymentDao;
    private final PaymentMapper paymentMapper;
    private final RandomNumberClient randomNumberClient;
    private final PaymentEventProducer paymentEventProducer;

    @Override
    public PaymentDto create(PaymentDto paymentDto) {
        logger.info("Creating payment for orderId: {}", paymentDto.getOrderId());

        PaymentEntity paymentEntity = paymentMapper.toEntity(paymentDto);
        if (paymentEntity.getTimestamp() == null) {
            paymentEntity.setTimestamp(LocalDateTime.now());
        }

        Integer randomNumber = randomNumberClient.fetchRandomNumber();
        String status = (randomNumber != null && randomNumber % 2 == 0) ? "SUCCESS" : "FAILED";
        paymentEntity.setStatus(status);

        paymentDao.create(paymentEntity);
        logger.info("Successfully created payment with ID: {}", paymentEntity.getId());

        CreatePaymentEventDto event = new CreatePaymentEventDto(
                paymentEntity.getId(),
                paymentEntity.getOrderId(),
                paymentEntity.getUserId(),
                paymentEntity.getStatus(),
                paymentEntity.getTimestamp(),
                paymentEntity.getPaymentAmount()
        );
        paymentEventProducer.sendCreatePaymentEvent(event);

        return paymentMapper.toDto(paymentEntity);
    }

    @Override
    public List<PaymentDto> getByOrderId(Long orderId) {
        logger.debug("Getting payments by orderId: {}", orderId);
        return paymentDao.getByOrderId(orderId).stream()
                .map(paymentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentDto> getByUserId(Long userId) {
        logger.debug("Getting payments by userId: {}", userId);
        return paymentDao.getByUserId(userId).stream()
                .map(paymentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentDto> getByStatuses(List<String> statuses) {
        logger.debug("Getting payments by statuses: {}", statuses);
        return paymentDao.getByStatuses(statuses).stream()
                .map(paymentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal getTotalSumByPeriod(LocalDate startDate, LocalDate endDate) {
        logger.debug("Getting total payment sum between {} and {}", startDate, endDate);

        if (startDate == null || endDate == null) {
            throw new BadRequestException("Start date and end date must be provided");
        }
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("End date must not be before start date");
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusNanos(1);

        return paymentDao.getTotalSumByPeriod(start, end);
    }
}

