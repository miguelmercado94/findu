package com.findu.core.infrastructure.adapter.out.persistence.mapper;

import com.findu.core.domain.model.Oferta;
import com.findu.core.domain.model.OfertaCronogramaDia;
import com.findu.core.infrastructure.adapter.out.persistence.entity.OfertaCronogramaDiaEntity;
import com.findu.core.infrastructure.adapter.out.persistence.entity.OfertaEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OfertaMapper {

    Oferta toDomain(OfertaEntity entity);

    @Mapping(target = "ofertaId", source = "oferta.id")
    OfertaCronogramaDia toCronogramaDomain(OfertaCronogramaDiaEntity entity);

    OfertaEntity toEntity(Oferta domain);

    @Mapping(target = "oferta", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    OfertaCronogramaDiaEntity toCronogramaEntity(OfertaCronogramaDia domain);

    @AfterMapping
    default void linkCronogramaDias(@MappingTarget OfertaEntity entity) {
        if (entity.getCronogramaDias() != null) {
            entity.getCronogramaDias().forEach(dia -> dia.setOferta(entity));
        }
    }
}
