package com.findu.core.mapper;

import com.findu.core.domain.model.Calificacion;
import com.findu.core.infrastructure.entity.CalificacionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CalificacionMapper {

    Calificacion toDomain(CalificacionEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    CalificacionEntity toEntity(Calificacion domain);
}
