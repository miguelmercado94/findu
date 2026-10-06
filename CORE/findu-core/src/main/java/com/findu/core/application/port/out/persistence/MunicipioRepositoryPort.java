package com.findu.core.application.port.out.persistence;

import com.findu.core.domain.model.Municipio;

import java.util.List;
import java.util.Optional;

public interface MunicipioRepositoryPort {

    List<Municipio> findAll();

    Optional<Municipio> findById(Long id);

    Optional<Municipio> findByCodigoDane(String codigoDane);
}
