package com.innowise.paymentservice.service.interfaces;

import com.innowise.paymentservice.dto.models.PaymentDto;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PaymentService {

    PaymentDto create(@Valid PaymentDto paymentDto);

    List<PaymentDto> getByOrderId(Long orderId);

    List<PaymentDto> getByUserId(Long userId);

    List<PaymentDto> getByStatuses(List<String> statuses);

    BigDecimal getTotalSumByPeriod(LocalDate startDate, LocalDate endDate);
}

