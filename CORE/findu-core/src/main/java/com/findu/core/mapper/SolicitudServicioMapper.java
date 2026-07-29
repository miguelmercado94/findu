package com.findu.core.mapper;

import com.findu.core.domain.model.SolicitudServicio;
import com.findu.core.infrastructure.entity.SolicitudServicioEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SolicitudServicioMapper {

    SolicitudServicio toDomain(SolicitudServicioEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SolicitudServicioEntity toEntity(SolicitudServicio domain);
}
