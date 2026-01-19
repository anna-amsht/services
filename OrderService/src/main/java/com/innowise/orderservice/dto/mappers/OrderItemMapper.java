package com.innowise.orderservice.dto.mappers;

import com.innowise.orderservice.dto.models.OrderItemDto;
import com.innowise.orderservice.entities.OrderItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ItemMapper.class)
public interface OrderItemMapper {

    @Mapping(target = "itemId", source = "item.id")
    @Mapping(target = "item", source = "item")
    @Mapping(target = "price", source = "item.price")
    OrderItemDto toDto(OrderItemEntity orderItemEntity);

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "item", ignore = true)
    OrderItemEntity toEntity(OrderItemDto orderItemDto);
}

