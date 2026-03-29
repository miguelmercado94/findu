package com.findu.security.mapper;

import com.findu.security.domain.model.Modulo;
import com.findu.security.infrastructure.entity.ModuleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Mapeo entre modelo de dominio Modulo y entidad ModuleEntity (MapStruct).
 * Los campos de auditoría solo existen en la entidad; se ignoran al mapear dominio → entidad.
 */
@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ModuloMapper {

    Modulo toDomain(ModuleEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ModuleEntity toEntity(Modulo domain);
}
