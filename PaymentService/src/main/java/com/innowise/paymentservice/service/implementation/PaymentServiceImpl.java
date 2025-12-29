package com.innowise.paymentservice.service.implementation;

import com.innowise.paymentservice.client.OrderServiceClient;
import com.innowise.paymentservice.client.RandomNumberClient;
import com.innowise.paymentservice.dao.interfaces.PaymentDao;
import com.innowise.paymentservice.dto.mappers.PaymentMapper;
import com.innowise.paymentservice.dto.models.CreatePaymentEventDto;
import com.innowise.paymentservice.dto.models.OrderDto;
import com.innowise.paymentservice.dto.models.PaymentDto;
import com.innowise.paymentservice.entities.PaymentEntity;
import com.innowise.paymentservice.exceptions.BadRequestException;
import com.innowise.paymentservice.kafka.PaymentEventProducer;
import com.innowise.paymentservice.service.PaymentIdGenerator;
import com.innowise.paymentservice.service.interfaces.PaymentService;
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
@Validated
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentDao paymentDao;
    private final PaymentMapper paymentMapper;
    private final RandomNumberClient randomNumberClient;
    private final PaymentEventProducer paymentEventProducer;
    private final PaymentIdGenerator paymentIdGenerator;
    private final OrderServiceClient orderServiceClient;

    @Override
    public PaymentDto create(PaymentDto paymentDto) {
        logger.info("Creating payment for orderId: {}", paymentDto.getOrderId());

        OrderDto order = orderServiceClient.getOrderById(paymentDto.getOrderId());
        if (order == null) {
            throw new BadRequestException("Order not found with ID: " + paymentDto.getOrderId());
        }

        if ("PAID".equals(order.getStatus())) {
            throw new BadRequestException("Order " + paymentDto.getOrderId() + " is already paid");
        }
        
        BigDecimal orderTotal = order.getTotalAmount();
        if (paymentDto.getPaymentAmount().compareTo(orderTotal) != 0) {
            throw new BadRequestException(
                String.format("Payment amount %.2f does not match order total %.2f", 
                    paymentDto.getPaymentAmount(), orderTotal));
        }

        PaymentEntity paymentEntity = paymentMapper.toEntity(paymentDto);
        paymentEntity.setId(paymentIdGenerator.generateId());
        if (paymentEntity.getTimestamp() == null) {
            paymentEntity.setTimestamp(LocalDateTime.now());
        }

        Integer randomNumber = randomNumberClient.fetchRandomNumber();
        String status = (randomNumber != null && randomNumber % 2 == 0) ? "SUCCESS" : "FAILED";
        paymentEntity.setStatus(status);

        PaymentEntity savedPayment = paymentDao.save(paymentEntity);
        logger.info("Successfully created payment with ID: {}", savedPayment.getId());

        CreatePaymentEventDto event = new CreatePaymentEventDto(
                savedPayment.getId(),
                savedPayment.getOrderId(),
                savedPayment.getUserId(),
                savedPayment.getStatus(),
                savedPayment.getTimestamp(),
                savedPayment.getPaymentAmount()
        );
        paymentEventProducer.sendCreatePaymentEvent(event);

        return paymentMapper.toDto(savedPayment);
    }

    @Override
    public List<PaymentDto> getByOrderId(Long orderId) {
        logger.debug("Getting payments by orderId: {}", orderId);
        return paymentDao.findByOrderId(orderId).stream()
                .map(paymentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentDto> getByUserId(Long userId) {
        logger.debug("Getting payments by userId: {}", userId);
        return paymentDao.findByUserId(userId).stream()
                .map(paymentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentDto> getByStatuses(List<String> statuses) {
        logger.debug("Getting payments by statuses: {}", statuses);
        return paymentDao.findByStatusIn(statuses).stream()
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

        List<PaymentEntity> payments = paymentDao.findByTimestampBetween(start, end);
        return payments.stream()
                .map(PaymentEntity::getPaymentAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

