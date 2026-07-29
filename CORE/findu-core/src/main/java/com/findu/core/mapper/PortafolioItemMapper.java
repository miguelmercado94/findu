package com.findu.core.mapper;

import com.findu.core.domain.model.PortafolioItem;
import com.findu.core.infrastructure.entity.PortafolioItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PortafolioItemMapper {

    PortafolioItem toDomain(PortafolioItemEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    PortafolioItemEntity toEntity(PortafolioItem domain);
}
