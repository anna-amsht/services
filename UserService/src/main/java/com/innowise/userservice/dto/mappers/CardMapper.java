package com.innowise.userservice.dto.mappers;

import com.innowise.userservice.dto.models.CardDto;
import com.innowise.userservice.entities.CardEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface CardMapper {
    @Mapping(target = "userId", expression = "java(cardEntity.getUser().getId())")
    CardDto toDto(CardEntity cardEntity);

    @Mapping(target = "user", ignore = true)
    CardEntity toEntity(CardDto cardDto);
}
