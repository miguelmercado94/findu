package com.findu.core.mapper;

import com.findu.core.domain.model.Servicio;
import com.findu.core.infrastructure.entity.ServicioEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ServicioMapper {

    Servicio toDomain(ServicioEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ServicioEntity toEntity(Servicio domain);
}
