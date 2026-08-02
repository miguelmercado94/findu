package com.findu.core.application.usecase.impl;

import com.findu.core.application.service.DireccionService;
import com.findu.core.application.service.PerfilClienteService;
import com.findu.core.application.usecase.GestionDireccionesUseCase;
import com.findu.core.domain.model.Direccion;
import com.findu.core.domain.model.constants.EstadoPerfil;
import com.findu.core.dto.request.ActualizarDireccionRequest;
import com.findu.core.dto.request.CrearDireccionClienteRequest;
import com.findu.core.dto.request.CrearDireccionRequest;
import com.findu.core.dto.response.DireccionResponse;
import com.findu.core.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GestionDireccionesUseCaseImpl implements GestionDireccionesUseCase {

    private final DireccionService direccionService;
    private final PerfilClienteService perfilClienteService;

    @Override
    public DireccionResponse crearDireccion(CrearDireccionRequest request) {
        if (request.perfilClienteId() != null) {
            perfilClienteService.findById(request.perfilClienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Perfil de cliente no encontrado con id: " + request.perfilClienteId()));

            // Validar etiqueta única
            validarEtiquetaUnica(request.perfilClienteId(), request.etiqueta(), null);

            // Si es principal, quitar principal a la anterior
            if (request.esPrincipal()) {
                quitarPrincipalAnterior(request.perfilClienteId());
            }
        }

        Direccion direccion = Direccion.builder()
                .perfilClienteId(request.perfilClienteId())
                .perfilProveedorId(request.perfilProveedorId())
                .etiqueta(request.etiqueta())
                .direccionTexto(request.direccionTexto())
                .municipioId(request.municipioId())
                .latitud(request.latitud())
                .longitud(request.longitud())
                .piso(request.piso())
                .apartamento(request.apartamento())
                .referencia(request.referencia())
                .esPrincipal(request.esPrincipal())
                .active(true)
                .build();

        Direccion saved = direccionService.save(direccion);

        if (request.perfilClienteId() != null && request.esPrincipal()) {
            activarPerfilSiIncompleto(request.perfilClienteId());
        }

        return toResponse(saved);
    }

    @Override
    public DireccionResponse crearDireccionCliente(Long perfilClienteId, CrearDireccionClienteRequest request) {
        perfilClienteService.findById(perfilClienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de cliente no encontrado con id: " + perfilClienteId));

        // Validar etiqueta única para este cliente
        validarEtiquetaUnica(perfilClienteId, request.etiqueta(), null);

        // Si es principal, quitar principal a la anterior
        if (request.esPrincipal()) {
            quitarPrincipalAnterior(perfilClienteId);
        }

        Direccion direccion = Direccion.builder()
                .perfilClienteId(perfilClienteId)
                .etiqueta(request.etiqueta())
                .direccionTexto(request.direccionTexto())
                .municipioId(request.municipioId())
                .latitud(request.latitud())
                .longitud(request.longitud())
                .piso(request.piso())
                .apartamento(request.apartamento())
                .referencia(request.referencia())
                .esPrincipal(request.esPrincipal())
                .active(true)
                .build();

        Direccion saved = direccionService.save(direccion);

        if (request.esPrincipal()) {
            activarPerfilSiIncompleto(perfilClienteId);
        }

        return toResponse(saved);
    }

    @Override
    public DireccionResponse actualizarDireccion(Long id, ActualizarDireccionRequest request) {
        Direccion direccion = direccionService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada con id: " + id));

        // Validar etiqueta única si se está cambiando
        if (request.etiqueta() != null && direccion.getPerfilClienteId() != null) {
            validarEtiquetaUnica(direccion.getPerfilClienteId(), request.etiqueta(), id);
        }

        if (request.etiqueta() != null) direccion.setEtiqueta(request.etiqueta());
        if (request.direccionTexto() != null) direccion.setDireccionTexto(request.direccionTexto());
        if (request.municipioId() != null) direccion.setMunicipioId(request.municipioId());
        if (request.latitud() != null) direccion.setLatitud(request.latitud());
        if (request.longitud() != null) direccion.setLongitud(request.longitud());
        if (request.piso() != null) direccion.setPiso(request.piso());
        if (request.apartamento() != null) direccion.setApartamento(request.apartamento());
        if (request.referencia() != null) direccion.setReferencia(request.referencia());

        Direccion updated = direccionService.save(direccion);
        return toResponse(updated);
    }

    @Override
    public void eliminarDireccion(Long id) {
        Direccion direccion = direccionService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada con id: " + id));

        // No se puede eliminar la dirección principal
        if (direccion.isEsPrincipal()) {
            throw new IllegalStateException("No se puede eliminar la dirección principal. Cambia la principal a otra dirección primero.");
        }

        direccion.setActive(false);
        direccionService.save(direccion);
    }

    @Override
    @Transactional(readOnly = true)
    public DireccionResponse consultarDireccion(Long id) {
        Direccion direccion = direccionService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada con id: " + id));
        return toResponse(direccion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DireccionResponse> listarDireccionesCliente(Long clienteId) {
        return direccionService.findByClienteId(clienteId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DireccionResponse obtenerDireccionSolicitud(Long solicitudId) {
        Direccion direccion = direccionService.findBySolicitudId(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada para la solicitud: " + solicitudId));
        return toResponse(direccion);
    }

    @Override
    public DireccionResponse marcarComoPrincipal(Long direccionId) {
        Direccion direccion = direccionService.findById(direccionId)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada con id: " + direccionId));

        if (direccion.getPerfilClienteId() == null) {
            throw new IllegalStateException("Solo se puede marcar como principal una dirección de cliente.");
        }

        // Quitar principal a la anterior
        quitarPrincipalAnterior(direccion.getPerfilClienteId());

        // Marcar la nueva como principal
        direccion.setEsPrincipal(true);
        Direccion updated = direccionService.save(direccion);
        return toResponse(updated);
    }

    // ─── Reglas de negocio ────────────────────────────────────────────────────

    /**
     * Valida que la etiqueta no exista ya para el mismo cliente.
     * @param excludeDireccionId ID de dirección a excluir (para updates), null para creates
     */
    private void validarEtiquetaUnica(Long perfilClienteId, String etiqueta, Long excludeDireccionId) {
        if (etiqueta == null || etiqueta.isBlank()) return;

        List<Direccion> direcciones = direccionService.findByClienteId(perfilClienteId);
        boolean existeDuplicada = direcciones.stream()
                .filter(d -> !d.getId().equals(excludeDireccionId))
                .anyMatch(d -> etiqueta.equalsIgnoreCase(d.getEtiqueta()));

        if (existeDuplicada) {
            throw new IllegalStateException("Ya existe una dirección con la etiqueta '" + etiqueta + "' para este cliente.");
        }
    }

    /**
     * Quita el flag esPrincipal de la dirección principal actual del cliente.
     */
    private void quitarPrincipalAnterior(Long perfilClienteId) {
        direccionService.findByClienteId(perfilClienteId).stream()
                .filter(Direccion::isEsPrincipal)
                .forEach(d -> {
                    d.setEsPrincipal(false);
                    direccionService.save(d);
                });
    }

    /**
     * Si el perfil está en INCOMPLETO, lo cambia a ACTIVO (ya tiene dirección principal).
     */
    private void activarPerfilSiIncompleto(Long perfilClienteId) {
        perfilClienteService.findById(perfilClienteId).ifPresent(perfil -> {
            if (EstadoPerfil.INCOMPLETO.equals(perfil.getEstado())) {
                perfil.setEstado(EstadoPerfil.ACTIVO);
                perfilClienteService.save(perfil);
            }
        });
    }

    private DireccionResponse toResponse(Direccion d) {
        return new DireccionResponse(
                d.getId(), d.getEtiqueta(), d.getDireccionTexto(), null,
                d.getLatitud(), d.getLongitud(), d.getPiso(), d.getApartamento(),
                d.getReferencia(), d.isEsPrincipal()
        );
    }
}
