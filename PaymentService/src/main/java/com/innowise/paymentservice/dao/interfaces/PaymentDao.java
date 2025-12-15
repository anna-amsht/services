package com.innowise.paymentservice.dao.interfaces;

import com.innowise.paymentservice.entities.PaymentEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentDao {
    void create(PaymentEntity paymentEntity);
    List<PaymentEntity> getByOrderId(Long orderId);
    List<PaymentEntity> getByUserId(Long userId);
    List<PaymentEntity> getByStatuses(List<String> statuses);
    BigDecimal getTotalSumByPeriod(LocalDateTime start, LocalDateTime end);
}

