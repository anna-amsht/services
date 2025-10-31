package com.innowise.userservice.dto.mappers;

import com.innowise.userservice.dto.models.UserDto;
import com.innowise.userservice.entities.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = CardMapper.class)
public interface UserMapper {

    UserDto toDto(UserEntity userEntity);
    UserEntity toEntity(UserDto userDto);
}

