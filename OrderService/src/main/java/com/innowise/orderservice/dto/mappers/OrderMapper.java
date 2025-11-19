package com.innowise.orderservice.dto.mappers;

import com.innowise.orderservice.dto.models.OrderDto;
import com.innowise.orderservice.entities.OrderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface OrderMapper {

    OrderDto toDto(OrderEntity orderEntity);
    
    @Mapping(target = "userId", source = "userId")
    OrderEntity toEntity(OrderDto orderDto);
}

