package com.innowise.orderservice.dto.mappers;

import com.innowise.orderservice.dto.models.OrderItemDto;
import com.innowise.orderservice.entities.OrderItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ItemMapper.class)
public interface OrderItemMapper {

    @Mapping(target = "itemId", expression = "java(orderItemEntity.getItem() != null ? orderItemEntity.getItem().getId() : null)")
    @Mapping(target = "item", source = "item")
    OrderItemDto toDto(OrderItemEntity orderItemEntity);

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "item", ignore = true)
    OrderItemEntity toEntity(OrderItemDto orderItemDto);
}

