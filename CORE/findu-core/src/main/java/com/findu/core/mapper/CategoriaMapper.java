package com.findu.core.mapper;

import com.findu.core.domain.model.Categoria;
import com.findu.core.infrastructure.entity.CategoriaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CategoriaMapper {

    Categoria toDomain(CategoriaEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CategoriaEntity toEntity(Categoria domain);
}
