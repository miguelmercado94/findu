package com.findu.core.mapper;

import com.findu.core.domain.model.EspecialistaCredencial;
import com.findu.core.infrastructure.entity.EspecialistaCredencialEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EspecialistaCredencialMapper {

    EspecialistaCredencial toDomain(EspecialistaCredencialEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    EspecialistaCredencialEntity toEntity(EspecialistaCredencial domain);
}
