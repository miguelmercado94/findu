package com.findu.core.application.service;

import com.findu.core.domain.model.Categoria;
import com.findu.core.domain.model.Municipio;
import com.findu.core.domain.model.Servicio;

import java.util.List;

public interface CatalogoService {

    List<Categoria> findAllCategorias();

    List<Servicio> findServiciosByCategoria(Long categoriaId);

    List<Municipio> findAllMunicipios();
}
