package com.findu.core.mapper;

import com.findu.core.domain.model.PerfilProveedor;
import com.findu.core.infrastructure.entity.PerfilProveedorEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PerfilProveedorMapper {

    PerfilProveedor toDomain(PerfilProveedorEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PerfilProveedorEntity toEntity(PerfilProveedor domain);
}
