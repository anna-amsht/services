package com.innowise.orderservice.service.interfaces;

import com.innowise.orderservice.dto.models.OrderDto;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    OrderDto create(@Valid OrderDto orderDto);
    Optional<OrderDto> getById(Long id);
    List<OrderDto> getByIds(List<Long> ids);
    List<OrderDto> getByStatuses(List<String> statuses);
    OrderDto update(Long id, @Valid OrderDto updatedOrderDto);
    void delete(Long id);
}

