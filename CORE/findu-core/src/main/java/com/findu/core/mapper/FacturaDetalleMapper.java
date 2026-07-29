package com.findu.core.mapper;

import com.findu.core.domain.model.FacturaDetalle;
import com.findu.core.infrastructure.entity.FacturaDetalleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FacturaDetalleMapper {

    FacturaDetalle toDomain(FacturaDetalleEntity entity);

    FacturaDetalleEntity toEntity(FacturaDetalle domain);
}
