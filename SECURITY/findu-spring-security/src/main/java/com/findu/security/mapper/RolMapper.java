package com.findu.security.mapper;

import com.findu.security.domain.model.Rol;
import com.findu.security.infrastructure.entity.RoleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Mapeo entre modelo de dominio Rol y entidad RoleEntity (MapStruct).
 * Los campos de auditoría solo existen en la entidad; se ignoran al mapear dominio → entidad.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RolMapper {

    Rol toDomain(RoleEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    RoleEntity toEntity(Rol domain);
}
