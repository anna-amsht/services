package com.innowise.orderservice.dao.implementation;

import com.innowise.orderservice.dao.interfaces.OrderDao;
import com.innowise.orderservice.entities.OrderEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional
public class OrderDaoImpl implements OrderDao {

    private static final Logger logger = LoggerFactory.getLogger(OrderDaoImpl.class);

    private final EntityManager entityManager;

    @Override
    public void create(OrderEntity orderEntity) {
        logger.debug("Creating order with userId: {}", orderEntity.getUserId());
        entityManager.persist(orderEntity);
        logger.debug("Successfully created order with ID: {}", orderEntity.getId());
    }

    @Override
    public Optional<OrderEntity> getById(Long id) {
        logger.debug("Getting order by id: {}", id);
        OrderEntity order = entityManager.find(OrderEntity.class, id);
        return Optional.ofNullable(order);
    }

    @Override
    public List<OrderEntity> getByIds(List<Long> ids) {
        logger.debug("Getting orders by ids: {}", ids);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        TypedQuery<OrderEntity> query = entityManager.createQuery(
                "SELECT o FROM OrderEntity o WHERE o.id IN :ids", OrderEntity.class);
        query.setParameter("ids", ids);
        return query.getResultList();
    }

    @Override
    public List<OrderEntity> getByStatuses(List<String> statuses) {
        logger.debug("Getting orders by statuses: {}", statuses);
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        TypedQuery<OrderEntity> query = entityManager.createQuery(
                "SELECT o FROM OrderEntity o WHERE o.status IN :statuses", OrderEntity.class);
        query.setParameter("statuses", statuses);
        return query.getResultList();
    }

    @Override
    public void update(Long id, OrderEntity updatedOrder) {
        logger.debug("Updating order with id: {}", id);
        OrderEntity existingOrder = entityManager.find(OrderEntity.class, id);
        if (existingOrder != null) {

            existingOrder.setStatus(updatedOrder.getStatus());

            // Only update orderItems if they are provided
            if (updatedOrder.getOrderItems() != null && !updatedOrder.getOrderItems().isEmpty()) {
                existingOrder.getOrderItems().clear();
                updatedOrder.getOrderItems().forEach(orderItem -> {
                    orderItem.setOrder(existingOrder);
                    existingOrder.getOrderItems().add(orderItem);
                });
            }
            
            logger.debug("Successfully updated order with ID: {}", id);
        }
    }

    @Override
    public void delete(Long id) {
        logger.debug("Deleting order with id: {}", id);
        OrderEntity order = entityManager.find(OrderEntity.class, id);
        if (order != null) {
            entityManager.remove(order);
            logger.debug("Successfully deleted order with ID: {}", id);
        }
    }
}

