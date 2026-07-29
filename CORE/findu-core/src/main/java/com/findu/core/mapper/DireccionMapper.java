package com.findu.core.mapper;

import com.findu.core.domain.model.Direccion;
import com.findu.core.infrastructure.entity.DireccionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DireccionMapper {

    Direccion toDomain(DireccionEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DireccionEntity toEntity(Direccion domain);
}
