package com.findu.core.mapper;

import com.findu.core.domain.model.Oferta;
import com.findu.core.infrastructure.entity.OfertaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OfertaMapper {

    Oferta toDomain(OfertaEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OfertaEntity toEntity(Oferta domain);
}
