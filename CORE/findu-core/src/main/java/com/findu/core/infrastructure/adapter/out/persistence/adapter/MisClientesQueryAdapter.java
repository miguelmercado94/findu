package com.findu.core.infrastructure.adapter.out.persistence.adapter;

import com.findu.core.application.port.out.persistence.MisClientesQueryPort;
import com.findu.core.domain.model.ClienteVinculoResumen;
import com.findu.core.domain.model.ServicioHistorialItem;
import com.findu.core.infrastructure.adapter.out.persistence.repository.MisClientesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MisClientesQueryAdapter implements MisClientesQueryPort {

    private final MisClientesRepository repository;

    @Override
    public Page<ClienteVinculoResumen> listar(Long perfilProveedorId, String textoBusqueda, Pageable pageable) {
        return repository.listar(perfilProveedorId, patron(textoBusqueda), pageable);
    }

    @Override
    public Optional<ClienteVinculoResumen> detalle(Long perfilProveedorId, UUID clienteRef) {
        return repository.detalle(perfilProveedorId, clienteRef);
    }

    @Override
    public Page<ServicioHistorialItem> historial(Long perfilProveedorId, Long perfilClienteId, Pageable pageable) {
        return repository.historial(perfilProveedorId, perfilClienteId, pageable);
    }

    @Override
    public Optional<String> fotoCliente(Long perfilClienteId) {
        return repository.fotoCliente(perfilClienteId);
    }

    /** LIKE seguro: escapa comodines del usuario; siempre string (evita parametros nulos tipados en Postgres). */
    static String patron(String texto) {
        String t = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        return "%" + t.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}