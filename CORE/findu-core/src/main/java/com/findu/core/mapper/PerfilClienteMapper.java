package com.findu.core.mapper;

import com.findu.core.domain.model.PerfilCliente;
import com.findu.core.infrastructure.entity.PerfilClienteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PerfilClienteMapper {

    PerfilCliente toDomain(PerfilClienteEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PerfilClienteEntity toEntity(PerfilCliente domain);
}
