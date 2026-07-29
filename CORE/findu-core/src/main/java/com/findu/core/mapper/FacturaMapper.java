package com.findu.core.mapper;

import com.findu.core.domain.model.Factura;
import com.findu.core.infrastructure.entity.FacturaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FacturaMapper {

    Factura toDomain(FacturaEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    FacturaEntity toEntity(Factura domain);
}
