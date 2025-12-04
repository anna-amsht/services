package com.innowise.orderservice.dto.mappers;

import com.innowise.orderservice.dto.models.ItemDto;
import com.innowise.orderservice.entities.ItemEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    ItemDto toDto(ItemEntity itemEntity);
    ItemEntity toEntity(ItemDto itemDto);
}

