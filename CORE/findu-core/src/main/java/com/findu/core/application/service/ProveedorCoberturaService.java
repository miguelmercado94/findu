package com.findu.core.application.service;

import com.findu.core.domain.model.ProveedorCobertura;

import java.util.List;

public interface ProveedorCoberturaService {

    void replaceCobertura(Long proveedorId, List<Long> municipioIds);

    List<ProveedorCobertura> findByProveedorId(Long perfilProveedorId);
}
