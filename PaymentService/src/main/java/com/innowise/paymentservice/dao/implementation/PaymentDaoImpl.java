package com.innowise.paymentservice.dao.implementation;

import com.innowise.paymentservice.dao.interfaces.PaymentDao;
import com.innowise.paymentservice.entities.PaymentEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional
public class PaymentDaoImpl implements PaymentDao {

    private static final Logger logger = LoggerFactory.getLogger(PaymentDaoImpl.class);

    private final EntityManager entityManager;

    @Override
    public void create(PaymentEntity paymentEntity) {
        entityManager.persist(paymentEntity);
        logger.debug("Successfully created payment with ID: {}", paymentEntity.getId());
    }

    @Override
    public List<PaymentEntity> getByOrderId(Long orderId) {
        TypedQuery<PaymentEntity> query = entityManager.createQuery(
                "SELECT p FROM PaymentEntity p WHERE p.orderId = :orderId", PaymentEntity.class);
        query.setParameter("orderId", orderId);
        return query.getResultList();
    }

    @Override
    public List<PaymentEntity> getByUserId(Long userId) {
        TypedQuery<PaymentEntity> query = entityManager.createQuery(
                "SELECT p FROM PaymentEntity p WHERE p.userId = :userId", PaymentEntity.class);
        query.setParameter("userId", userId);
        return query.getResultList();
    }

    @Override
    public List<PaymentEntity> getByStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        TypedQuery<PaymentEntity> query = entityManager.createQuery(
                "SELECT p FROM PaymentEntity p WHERE p.status IN :statuses", PaymentEntity.class);
        query.setParameter("statuses", statuses);
        return query.getResultList();
    }

    @Override
    public BigDecimal getTotalSumByPeriod(LocalDateTime start, LocalDateTime end) {
        TypedQuery<BigDecimal> query = entityManager.createQuery(
                "SELECT COALESCE(SUM(p.paymentAmount), 0) FROM PaymentEntity p WHERE p.timestamp BETWEEN :start AND :end",
                BigDecimal.class);
        query.setParameter("start", start);
        query.setParameter("end", end);
        return query.getSingleResult();
    }
}

