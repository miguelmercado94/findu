package com.findu.core.application.service;

import com.findu.core.application.dto.response.ClienteDetalleResponse;
import com.findu.core.application.dto.response.ClienteResumenResponse;
import com.findu.core.application.dto.response.PaginaResponse;
import com.findu.core.application.dto.response.ServicioHistorialItemResponse;
import com.findu.core.application.port.in.GestionMisClientesUseCase;
import com.findu.core.application.port.out.persistence.ClasificacionClienteRepositoryPort;
import com.findu.core.application.port.out.persistence.ClienteProveedorFavoritoRepositoryPort;
import com.findu.core.application.port.out.persistence.MisClientesQueryPort;
import com.findu.core.application.util.Base64Images;
import com.findu.core.domain.enums.ChatEstado;
import com.findu.core.domain.exception.ArchivoProveedorException;
import com.findu.core.domain.exception.ResourceNotFoundException;
import com.findu.core.domain.model.ClienteProveedorFavorito;
import com.findu.core.domain.model.ClienteVinculoResumen;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GestionMisClientesUseCaseImpl implements GestionMisClientesUseCase {

    private static final String NO_ENCONTRADO = "Cliente";

    private final MisClientesQueryPort queryPort;
    private final ClienteProveedorFavoritoRepositoryPort favoritoPort;
    private final ClasificacionClienteRepositoryPort clasificacionPort;

    @Override
    public PaginaResponse<ClienteResumenResponse> listar(Long perfilProveedorId, String q, int page, int size) {
        return PaginaResponse.de(queryPort.listar(perfilProveedorId, q, pagina(page, size)), this::resumen);
    }

    @Override
    public ClienteDetalleResponse detalle(Long perfilProveedorId, UUID clienteRef) {
        ClienteVinculoResumen v = queryPort.detalle(perfilProveedorId, clienteRef)
                .orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADO, clienteRef));
        return new ClienteDetalleResponse(v.clienteRef(), v.nombreCompleto(), v.tieneFoto(), v.favoritoInterno(),
                v.serviciosRealizados(), v.enServicio(), v.activo(), ChatEstado.desde(v.activo()));
    }

    @Override
    public PaginaResponse<ServicioHistorialItemResponse> historial(Long perfilProveedorId, UUID clienteRef,
                                                                   int page, int size) {
        ClienteProveedorFavorito vinculo = vinculo(perfilProveedorId, clienteRef);
        return PaginaResponse.de(
                queryPort.historial(perfilProveedorId, vinculo.getPerfilClienteId(), pagina(page, size)),
                i -> new ServicioHistorialItemResponse(i.servicio(), i.categoria(), i.fecha(), i.estado()));
    }

    @Override
    public FotoCliente foto(Long perfilProveedorId, UUID clienteRef) {
        ClienteProveedorFavorito vinculo = vinculo(perfilProveedorId, clienteRef);
        String raw = queryPort.fotoCliente(vinculo.getPerfilClienteId())
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new ResourceNotFoundException("Foto de cliente", clienteRef));
        try {
            byte[] bytes = Base64Images.decode(raw);
            if (bytes.length == 0) {
                throw new ResourceNotFoundException("Foto de cliente", clienteRef);
            }
            return new FotoCliente(bytes, contentType(raw, bytes));
        } catch (ArchivoProveedorException e) {
            // valor que no es data URL / base64 (p.ej. una URL): no hay bytes que servir
            throw new ResourceNotFoundException("Foto de cliente", clienteRef);
        }
    }

    @Override
    @Transactional
    public void definirFavoritoInterno(Long perfilProveedorId, UUID clienteRef, boolean favorito) {
        ClienteProveedorFavorito vinculo = vinculo(perfilProveedorId, clienteRef);
        clasificacionPort.guardarFavoritoInterno(perfilProveedorId, vinculo.getPerfilClienteId(), favorito);
    }

    private ClienteProveedorFavorito vinculo(Long perfilProveedorId, UUID clienteRef) {
        return favoritoPort.findByProveedorAndClienteRef(perfilProveedorId, clienteRef)
                .orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADO, clienteRef));
    }

    private ClienteResumenResponse resumen(ClienteVinculoResumen v) {
        return new ClienteResumenResponse(v.clienteRef(), v.nombreCompleto(), v.tieneFoto(), v.favoritoInterno(),
                v.serviciosRealizados(), v.enServicio(), v.activo(), ChatEstado.desde(v.activo()));
    }

    private static PageRequest pagina(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), TAMANO_MAXIMO));
    }

    static String contentType(String raw, byte[] bytes) {
        String s = raw.trim();
        if (s.regionMatches(true, 0, "data:", 0, 5)) {
            int end = s.indexOf(';');
            int comma = s.indexOf(',');
            int stop = end >= 0 && (comma < 0 || end < comma) ? end : comma;
            if (stop > 5) {
                String mime = s.substring(5, stop).trim().toLowerCase();
                if (mime.startsWith("image/")) {
                    return mime;
                }
            }
        }
        if (bytes.length > 3 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P') {
            return "image/png";
        }
        if (bytes.length > 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }
}