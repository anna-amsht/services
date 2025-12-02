package com.innowise.orderservice.dto.mappers;

import com.innowise.orderservice.dto.models.OrderDto;
import com.innowise.orderservice.entities.OrderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {

    @Mapping(target = "orderItems", source = "orderItems")
    OrderDto toDto(OrderEntity orderEntity);
    
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "orderItems", ignore = true)
    OrderEntity toEntity(OrderDto orderDto);
}

