package com.innowise.paymentservice.dao.interfaces;

import com.innowise.paymentservice.entities.PaymentEntity;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentDao extends MongoRepository<PaymentEntity, Long> {
    List<PaymentEntity> findByOrderId(Long orderId);
    List<PaymentEntity> findByUserId(Long userId);
    List<PaymentEntity> findByStatusIn(List<String> statuses);
    List<PaymentEntity> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}

