package com.findu.core.application.usecase.impl;

import com.findu.core.application.service.DireccionService;
import com.findu.core.application.usecase.GestionDireccionesUseCase;
import com.findu.core.domain.model.Direccion;
import com.findu.core.dto.request.ActualizarDireccionRequest;
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

    @Override
    public DireccionResponse crearDireccion(CrearDireccionRequest request) {
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
        return toResponse(saved);
    }

    @Override
    public DireccionResponse actualizarDireccion(Long id, ActualizarDireccionRequest request) {
        Direccion direccion = direccionService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada con id: " + id));

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

    private DireccionResponse toResponse(Direccion d) {
        return new DireccionResponse(
                d.getId(), d.getEtiqueta(), d.getDireccionTexto(), null,
                d.getLatitud(), d.getLongitud(), d.getPiso(), d.getApartamento(),
                d.getReferencia(), d.isEsPrincipal()
        );
    }
}
