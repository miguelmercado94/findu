package com.findu.core.mapper;

import com.findu.core.domain.model.PerfilEspecialista;
import com.findu.core.infrastructure.entity.PerfilEspecialistaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PerfilEspecialistaMapper {

    PerfilEspecialista toDomain(PerfilEspecialistaEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PerfilEspecialistaEntity toEntity(PerfilEspecialista domain);
}
