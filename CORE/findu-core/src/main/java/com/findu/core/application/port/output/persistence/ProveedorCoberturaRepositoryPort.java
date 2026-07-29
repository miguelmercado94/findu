package com.findu.core.application.port.output.persistence;

import com.findu.core.domain.model.ProveedorCobertura;

import java.util.List;

public interface ProveedorCoberturaRepositoryPort {

    void saveAll(Long proveedorId, List<Long> municipioIds);

    List<ProveedorCobertura> findByProveedorId(Long perfilProveedorId);

    List<ProveedorCobertura> findProveedoresByMunicipioId(Long municipioId);

    void deleteAllByProveedorId(Long perfilProveedorId);
}
