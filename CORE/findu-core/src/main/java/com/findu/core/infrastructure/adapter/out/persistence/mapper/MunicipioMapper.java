package com.findu.core.infrastructure.adapter.out.persistence.mapper;

import com.findu.core.domain.model.Municipio;
import com.findu.core.infrastructure.adapter.out.persistence.entity.MunicipioEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MunicipioMapper {

    Municipio toDomain(MunicipioEntity entity);

    MunicipioEntity toEntity(Municipio domain);
}
