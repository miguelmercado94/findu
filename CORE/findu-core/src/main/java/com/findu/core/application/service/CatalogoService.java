package com.findu.core.application.service;

import com.findu.core.domain.model.Categoria;
import com.findu.core.domain.model.Municipio;
import com.findu.core.domain.model.Servicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CatalogoService {

    List<Categoria> findAllCategorias();

    Page<Categoria> findCategorias(Pageable pageable);

    Page<Categoria> findSubcategorias(Long categoriaPadreId, Pageable pageable);

    List<Servicio> findServiciosByCategoria(Long categoriaId);

    Page<Servicio> findServicios(Long categoriaId, Pageable pageable);

    List<Municipio> findAllMunicipios();
}
