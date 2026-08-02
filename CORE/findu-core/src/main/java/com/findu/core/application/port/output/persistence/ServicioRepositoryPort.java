package com.findu.core.application.port.output.persistence;

import com.findu.core.domain.model.Servicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ServicioRepositoryPort {

    List<Servicio> findAll();

    List<Servicio> findByCategoriaId(Long categoriaId);

    Page<Servicio> findAll(Pageable pageable);

    Page<Servicio> findByCategoriaId(Long categoriaId, Pageable pageable);
}
