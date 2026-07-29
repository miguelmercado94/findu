package com.findu.core.mapper;

import com.findu.core.domain.model.Municipio;
import com.findu.core.infrastructure.entity.MunicipioEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MunicipioMapper {

    Municipio toDomain(MunicipioEntity entity);

    MunicipioEntity toEntity(Municipio domain);
}
