package com.findu.core.application.service.impl;

import com.findu.core.application.port.output.persistence.ProveedorCoberturaRepositoryPort;
import com.findu.core.application.service.ProveedorCoberturaService;
import com.findu.core.domain.model.ProveedorCobertura;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProveedorCoberturaServiceImpl implements ProveedorCoberturaService {

    private final ProveedorCoberturaRepositoryPort port;

    @Override
    @Transactional
    public void replaceCobertura(Long proveedorId, List<Long> municipioIds) {
        port.deleteAllByProveedorId(proveedorId);
        if (municipioIds != null && !municipioIds.isEmpty()) {
            port.saveAll(proveedorId, municipioIds);
        }
    }

    @Override
    public List<ProveedorCobertura> findByProveedorId(Long perfilProveedorId) {
        return port.findByProveedorId(perfilProveedorId);
    }
}
