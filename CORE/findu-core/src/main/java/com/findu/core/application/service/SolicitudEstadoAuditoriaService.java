package com.findu.core.application.service;

import com.findu.core.domain.model.SolicitudEstadoAuditoria;
import com.findu.core.infrastructure.adapter.out.persistence.entity.SolicitudEstadoAuditoriaEntity;
import com.findu.core.infrastructure.adapter.out.persistence.repository.SolicitudEstadoAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SolicitudEstadoAuditoriaService {

    private final SolicitudEstadoAuditoriaRepository repository;

    public SolicitudEstadoAuditoria registrarCambioEstado(
            Long solicitudServicioId,
            String estadoAnterior,
            String estadoNuevo,
            String actorTipo,
            Long actorId,
            BigDecimal latitud,
            BigDecimal longitud,
            BigDecimal distanciaMetros,
            String motivo
    ) {
        if (solicitudServicioId == null || estadoNuevo == null) {
            return null;
        }
        LocalDateTime ahora = LocalDateTime.now();
        SolicitudEstadoAuditoriaEntity entity = SolicitudEstadoAuditoriaEntity.builder()
                .solicitudServicioId(solicitudServicioId)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(estadoNuevo)
                .actorTipo(actorTipo != null ? actorTipo : "SISTEMA")
                .actorId(actorId)
                .latitud(latitud)
                .longitud(longitud)
                .distanciaMetros(distanciaMetros)
                .motivo(motivo)
                .fechaHora(ahora)
                .build();

        if (repository != null) {
            entity = repository.save(entity);
        }

        log.info("[AUDITORIA INTERNA] Solicitud #{} transición {} -> {} | actor={}:{} | gps=({}, {}) dist={}m | fecha={}",
                solicitudServicioId, estadoAnterior, estadoNuevo, actorTipo, actorId, latitud, longitud, distanciaMetros, ahora);

        return toDomain(entity);
    }

    @Transactional(readOnly = true)
    public List<SolicitudEstadoAuditoria> consultarAuditoriaPorSolicitud(Long solicitudServicioId) {
        if (repository == null || solicitudServicioId == null) {
            return List.of();
        }
        return repository.findBySolicitudServicioIdOrderByFechaHoraAsc(solicitudServicioId).stream()
                .map(this::toDomain)
                .toList();
    }

    private SolicitudEstadoAuditoria toDomain(SolicitudEstadoAuditoriaEntity e) {
        if (e == null) return null;
        return SolicitudEstadoAuditoria.builder()
                .id(e.getId())
                .solicitudServicioId(e.getSolicitudServicioId())
                .estadoAnterior(e.getEstadoAnterior())
                .estadoNuevo(e.getEstadoNuevo())
                .actorTipo(e.getActorTipo())
                .actorId(e.getActorId())
                .latitud(e.getLatitud())
                .longitud(e.getLongitud())
                .distanciaMetros(e.getDistanciaMetros())
                .motivo(e.getMotivo())
                .fechaHora(e.getFechaHora())
                .build();
    }
}

