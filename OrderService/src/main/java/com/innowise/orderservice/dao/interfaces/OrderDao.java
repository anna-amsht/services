package com.innowise.orderservice.dao.interfaces;

import com.innowise.orderservice.entities.OrderEntity;

import java.util.List;
import java.util.Optional;

public interface OrderDao {
    void create(OrderEntity orderEntity);
    Optional<OrderEntity> getById(Long id);
    List<OrderEntity> getByIds(List<Long> ids);
    List<OrderEntity> getByStatuses(List<String> statuses);
    void update(Long id, OrderEntity updatedOrder);
    void updateStatus(Long id, String status);
    void delete(Long id);
}

