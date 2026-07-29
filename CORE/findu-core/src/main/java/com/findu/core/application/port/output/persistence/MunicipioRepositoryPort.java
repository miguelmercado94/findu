package com.findu.core.application.port.output.persistence;

import com.findu.core.domain.model.Municipio;

import java.util.List;
import java.util.Optional;

public interface MunicipioRepositoryPort {

    List<Municipio> findAll();

    Optional<Municipio> findByCodigoDane(String codigoDane);
}
