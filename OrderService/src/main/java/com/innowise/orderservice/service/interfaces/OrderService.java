package com.innowise.orderservice.service.interfaces;

import com.innowise.orderservice.dto.models.OrderDto;
import com.innowise.orderservice.dto.models.OrderWithUserDto;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    OrderWithUserDto create(@Valid OrderDto orderDto);
    Optional<OrderWithUserDto> getById(Long id);
    List<OrderWithUserDto> getByIds(List<Long> ids);
    List<OrderWithUserDto> getByStatuses(List<String> statuses);
    OrderWithUserDto update(Long id, @Valid OrderDto updatedOrderDto);
    void delete(Long id);
}

