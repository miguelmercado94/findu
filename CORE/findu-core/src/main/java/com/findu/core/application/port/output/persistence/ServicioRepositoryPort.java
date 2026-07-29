package com.findu.core.application.port.output.persistence;

import com.findu.core.domain.model.Servicio;

import java.util.List;

public interface ServicioRepositoryPort {

    List<Servicio> findAll();

    List<Servicio> findByCategoriaId(Long categoriaId);
}
