package com.innowise.paymentservice.dto.mappers;

import com.innowise.paymentservice.dto.models.PaymentDto;
import com.innowise.paymentservice.entities.PaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentDto toDto(PaymentEntity paymentEntity);

    @Mapping(target = "id", ignore = true)
    PaymentEntity toEntity(PaymentDto paymentDto);
}

